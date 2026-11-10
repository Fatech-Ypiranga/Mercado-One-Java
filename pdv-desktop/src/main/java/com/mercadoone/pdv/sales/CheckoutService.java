package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.LocalSaleStatus;
import com.mercadoone.pdv.offline.OfflineSaleQueue;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSale;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSaleItem;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.sales.OnlineSaleClient.ApiRejectedException;
import com.mercadoone.pdv.sales.OnlineSaleClient.ApiUnavailableException;
import com.mercadoone.pdv.sales.OnlineSaleClient.DiscountLimit;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSaleItem;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSalePayment;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSaleSyncRequest;
import com.mercadoone.pdv.sales.OnlineSaleClient.OnlineSaleRequest;
import com.mercadoone.pdv.sales.OnlineSaleClient.RemoteSaleStatus;
import com.mercadoone.pdv.sales.OnlineSaleClient.SaleLine;
import com.mercadoone.pdv.sync.SyncStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Regras de finalizacao e sincronizacao do PDV, independentes da tela JavaFX.
 *
 * <p>Fluxo de finalizacao: valida o rascunho, grava a venda no SQLite como {@code PENDING} e, se houver sessao,
 * tenta {@code POST /api/sales} com {@code clientSaleId = localSaleId}. A API e idempotente por esse id, entao
 * um reenvio posterior pela fila offline nao duplica a venda.</p>
 */
public class CheckoutService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final OfflineSaleQueue queue;
    private final OnlineSaleClient client;
    private final Clock clock;
    private final Supplier<String> idGenerator;
    private volatile Boolean lastApiReachable;
    private volatile DiscountLimit discountLimit = DiscountLimit.NONE;

    public CheckoutService(OfflineSaleQueue queue, OnlineSaleClient client) {
        this(queue, client, Clock.systemUTC(), () -> UUID.randomUUID().toString());
    }

    CheckoutService(OfflineSaleQueue queue, OnlineSaleClient client, Clock clock, Supplier<String> idGenerator) {
        this.queue = queue;
        this.client = client;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    public DiscountLimit discountLimit() {
        return discountLimit;
    }

    /** Busca a politica de desconto do operador. Sem resposta da API, mantem a ultima conhecida. */
    public DiscountLimit refreshDiscountLimit(Session session) {
        if (!session.authenticated()) {
            return discountLimit;
        }
        try {
            discountLimit = client.discountPolicy(session.apiBaseUrl(), session.token());
            lastApiReachable = true;
        } catch (ApiUnavailableException exception) {
            lastApiReachable = false;
        }
        return discountLimit;
    }

    /** Valida o rascunho e devolve a mensagem de erro, ou {@code null} quando pode finalizar. */
    public String validate(Draft draft) {
        if (draft.lines().isEmpty()) {
            return "Adicione ao menos um item.";
        }
        BigDecimal gross = draft.grossAmount();
        BigDecimal discount = draft.discountAmount();
        if (discount.signum() < 0) {
            return "Desconto nao pode ser negativo.";
        }
        if (discount.signum() > 0 && discount.compareTo(gross) >= 0) {
            return "Desconto deve ser menor que o total bruto.";
        }
        if (discount.signum() > 0 && !discountLimit.unlimited()) {
            BigDecimal limit = gross.multiply(discountLimit.maxPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            if (discount.compareTo(limit) > 0) {
                return "Desconto acima do permitido para o perfil (max. " + discountLimit.maxPercent().stripTrailingZeros().toPlainString()
                        + "% = R$ " + limit.toPlainString() + ").";
            }
        }
        if (draft.payments().isEmpty()) {
            return "Registre ao menos um pagamento.";
        }
        if (draft.payments().stream().anyMatch(payment -> payment.amount().signum() <= 0)) {
            return "Pagamentos devem ter valor maior que zero.";
        }
        BigDecimal remaining = draft.remainingAmount();
        if (remaining.signum() != 0) {
            return remaining.signum() > 0
                    ? "Falta registrar R$ " + remaining.toPlainString() + " em pagamentos."
                    : "Pagamentos excedem o total em R$ " + remaining.negate().toPlainString() + ".";
        }
        return null;
    }

    public CheckoutResult checkout(Session session, Draft draft) {
        String validation = validate(draft);
        if (validation != null) {
            return new CheckoutResult(Outcome.INVALID, null, null, validation);
        }
        LocalSale sale = new LocalSale(
                idGenerator.get(),
                Instant.now(clock),
                draft.customerId(),
                LocalSaleStatus.PENDING,
                draft.lines().stream()
                        .map(line -> new LocalSaleItem(line.productId(), line.quantity(), line.unitPrice()))
                        .toList(),
                draft.payments(),
                draft.discountAmount()
        );
        queue.enqueue(sale);

        if (!session.authenticated()) {
            return new CheckoutResult(Outcome.QUEUED_OFFLINE, sale, null,
                    "Sem sessao na API. Venda salva na fila offline e sera enviada no proximo login.");
        }
        try {
            Long remoteId = client.finalizeOnlineSale(session.apiBaseUrl(), session.token(), toOnlineRequest(sale));
            queue.markSent(sale.localSaleId(), remoteId);
            lastApiReachable = true;
            return new CheckoutResult(Outcome.CONFIRMED_ONLINE, sale, remoteId, "Venda confirmada na API (#" + remoteId + ").");
        } catch (ApiUnavailableException exception) {
            queue.markError(sale.localSaleId(), exception.getMessage());
            lastApiReachable = false;
            return new CheckoutResult(Outcome.QUEUED_OFFLINE, sale, null,
                    "API indisponivel. Venda salva na fila offline e sera reenviada.");
        } catch (ApiRejectedException exception) {
            lastApiReachable = true;
            if (exception.isAuthenticationFailure()) {
                queue.markError(sale.localSaleId(), exception.getMessage());
                return new CheckoutResult(Outcome.QUEUED_OFFLINE, sale, null,
                        "Sessao expirada. Venda salva na fila offline; entre novamente para enviar.");
            }
            queue.markRejected(sale.localSaleId(), exception.getMessage());
            return new CheckoutResult(Outcome.REJECTED, sale, null, "Venda recusada pela API: " + exception.getMessage());
        }
    }

    /**
     * Reenvia vendas {@code PENDING}/{@code ERROR} e consulta a API sobre vendas em {@code CONFLICT}
     * para refletir aceite ou rejeicao feitos no admin.
     */
    public SyncSummary synchronize(Session session) {
        if (!session.authenticated()) {
            return new SyncSummary(0, 0, 0, 0, 0);
        }
        int sent = 0;
        int conflicts = 0;
        int errors = 0;
        for (LocalSale sale : queue.pendingSales()) {
            try {
                var result = client.syncOfflineSale(session.apiBaseUrl(), session.token(), toSyncRequest(sale));
                lastApiReachable = true;
                if ("SENT".equals(result.status())) {
                    queue.markSent(sale.localSaleId(), result.saleId());
                    sent++;
                } else {
                    queue.markConflict(sale.localSaleId(), result.conflictMessage());
                    conflicts++;
                }
            } catch (ApiUnavailableException exception) {
                lastApiReachable = false;
                queue.markError(sale.localSaleId(), exception.getMessage());
                errors++;
                break;
            } catch (RuntimeException exception) {
                queue.markError(sale.localSaleId(), exception.getMessage());
                errors++;
            }
        }

        int accepted = 0;
        int rejected = 0;
        List<String> conflictIds = queue.conflictSaleIds();
        if (!conflictIds.isEmpty() && !Boolean.FALSE.equals(lastApiReachable)) {
            try {
                for (int start = 0; start < conflictIds.size(); start += 100) {
                    List<String> batch = conflictIds.subList(start, Math.min(conflictIds.size(), start + 100));
                    for (RemoteSaleStatus status : client.remoteStatus(session.apiBaseUrl(), session.token(), batch)) {
                        switch (status.status()) {
                            case "SENT" -> {
                                queue.markSent(status.localSaleId(), status.remoteSaleId());
                                accepted++;
                            }
                            case "REJECTED" -> {
                                queue.markRejected(status.localSaleId(),
                                        status.note() == null ? "Conflito rejeitado no admin." : "Rejeitada no admin: " + status.note());
                                rejected++;
                            }
                            default -> {
                                // CONFLICT pendente ou UNKNOWN: mantem o registro local intacto.
                            }
                        }
                    }
                }
                lastApiReachable = true;
            } catch (ApiUnavailableException exception) {
                lastApiReachable = false;
            } catch (RuntimeException exception) {
                // Consulta de status e auxiliar; falha nao altera a fila.
            }
        }
        return new SyncSummary(sent, conflicts, errors, accepted, rejected);
    }

    public SyncStatus currentStatus() {
        Map<LocalSaleStatus, Integer> counts = queue.countByStatus();
        if (counts.get(LocalSaleStatus.ERROR) > 0 && Boolean.TRUE.equals(lastApiReachable)) {
            return SyncStatus.SYNC_FAILED;
        }
        if (counts.get(LocalSaleStatus.PENDING) + counts.get(LocalSaleStatus.ERROR) > 0) {
            return SyncStatus.SYNC_PENDING;
        }
        if (counts.get(LocalSaleStatus.CONFLICT) > 0) {
            return SyncStatus.SYNC_FAILED;
        }
        return Boolean.TRUE.equals(lastApiReachable) ? SyncStatus.ONLINE : SyncStatus.OFFLINE_READY;
    }

    public void markApiReachable(boolean reachable) {
        lastApiReachable = reachable;
    }

    static OnlineSaleRequest toOnlineRequest(LocalSale sale) {
        return new OnlineSaleRequest(
                sale.localSaleId(),
                sale.customerId(),
                sale.items().stream().map(item -> new SaleLine(item.productId(), item.quantity())).toList(),
                sale.payments().stream().map(payment -> new OfflineSalePayment(payment.method(), payment.amount())).toList(),
                sale.discountAmount().signum() == 0 ? null : sale.discountAmount()
        );
    }

    static OfflineSaleSyncRequest toSyncRequest(LocalSale sale) {
        return new OfflineSaleSyncRequest(
                sale.localSaleId(),
                sale.createdAt().toString(),
                sale.customerId(),
                sale.items().stream()
                        .map(item -> new OfflineSaleItem(item.productId(), item.quantity(), item.unitPrice()))
                        .toList(),
                sale.payments().stream()
                        .map(payment -> new OfflineSalePayment(payment.method(), payment.amount()))
                        .toList(),
                sale.discountAmount().signum() == 0 ? null : sale.discountAmount()
        );
    }

    public record Session(String apiBaseUrl, String token) {
        public boolean authenticated() {
            return token != null && !token.isBlank();
        }
    }

    public record DraftLine(Long productId, String productName, BigDecimal quantity, BigDecimal unitPrice) {
        public BigDecimal totalAmount() {
            return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public record Draft(
            Long customerId,
            List<DraftLine> lines,
            BigDecimal discountAmount,
            List<LocalSalePayment> payments
    ) {
        public Draft {
            discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount.setScale(2, RoundingMode.HALF_UP);
            lines = List.copyOf(lines);
            payments = List.copyOf(payments);
        }

        public BigDecimal grossAmount() {
            return lines.stream().map(DraftLine::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal netAmount() {
            return grossAmount().subtract(discountAmount);
        }

        public BigDecimal paidAmount() {
            return payments.stream().map(LocalSalePayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal remainingAmount() {
            return netAmount().subtract(paidAmount());
        }
    }

    public enum Outcome {
        CONFIRMED_ONLINE,
        QUEUED_OFFLINE,
        REJECTED,
        INVALID
    }

    public record CheckoutResult(Outcome outcome, LocalSale sale, Long remoteSaleId, String message) {
        /** Venda concluida para o cliente: confirmada online ou preservada na fila offline. */
        public boolean completed() {
            return outcome == Outcome.CONFIRMED_ONLINE || outcome == Outcome.QUEUED_OFFLINE;
        }
    }

    public record SyncSummary(int sent, int conflicts, int errors, int acceptedInAdmin, int rejectedInAdmin) {
        public String describe() {
            if (sent + conflicts + errors + acceptedInAdmin + rejectedInAdmin == 0) {
                return "Fila offline sem pendencias.";
            }
            return "Sincronizacao: %d enviada(s), %d conflito(s), %d falha(s), %d aceita(s) no admin, %d rejeitada(s) no admin."
                    .formatted(sent, conflicts, errors, acceptedInAdmin, rejectedInAdmin);
        }
    }
}
