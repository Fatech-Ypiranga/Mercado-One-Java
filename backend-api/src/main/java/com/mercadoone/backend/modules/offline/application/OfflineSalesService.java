package com.mercadoone.backend.modules.offline.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.catalog.application.CatalogService;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.inventory.application.InventoryService;
import com.mercadoone.backend.modules.offline.domain.OfflineConflictStatus;
import com.mercadoone.backend.modules.offline.domain.OfflineSaleConflictRecord;
import com.mercadoone.backend.modules.offline.infrastructure.OfflineSaleConflictRepository;
import com.mercadoone.backend.modules.sales.application.SalesService;
import com.mercadoone.backend.modules.sales.application.SalesService.SaleCommand;
import com.mercadoone.backend.modules.sales.application.SalesService.SaleItemData;
import com.mercadoone.backend.modules.sales.application.SalesService.SalePaymentData;
import com.mercadoone.backend.modules.sales.domain.Sale;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OfflineSalesService {

    private final SalesService salesService;
    private final CatalogService catalog;
    private final InventoryService inventory;
    private final OfflineSaleConflictRepository conflicts;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    OfflineSalesService(
            SalesService salesService,
            CatalogService catalog,
            InventoryService inventory,
            OfflineSaleConflictRepository conflicts,
            AuditService auditService
    ) {
        this.salesService = salesService;
        this.catalog = catalog;
        this.inventory = inventory;
        this.conflicts = conflicts;
        this.auditService = auditService;
    }

    @Transactional
    public OfflineSaleSyncResult syncOfflineSale(OfflineSaleData data, Long operatorUserId, UserRole operatorRole) {
        Optional<Sale> alreadySynced = salesService.findByClientSaleId(data.localSaleId());
        if (alreadySynced.isPresent()) {
            return new OfflineSaleSyncResult("SENT", alreadySynced.get(), List.of());
        }
        List<OfflineSaleConflict> saleConflicts = validate(data, operatorRole);
        if (!saleConflicts.isEmpty()) {
            saveConflict(data, operatorUserId, saleConflicts);
            return new OfflineSaleSyncResult("CONFLICT", null, saleConflicts);
        }
        // Regras verificadas acima; uma excecao aqui reverte tudo e o PDV reenvia depois.
        Sale sale = salesService.confirmSale(toCommand(data), operatorUserId, operatorRole, false);
        conflicts.findFirstByLocalSaleIdAndStatusOrderByUpdatedAtDesc(data.localSaleId(), OfflineConflictStatus.PENDING)
                .ifPresent(pending -> pending.accept(sale.getId(), operatorUserId, "Sincronizada sem conflito em nova tentativa."));
        return new OfflineSaleSyncResult("SENT", sale, List.of());
    }

    @Transactional(readOnly = true)
    public List<LocalSaleRemoteStatus> remoteStatus(List<String> localSaleIds) {
        return localSaleIds.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .map(this::remoteStatusOf)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfflineSaleConflictRecord> listConflicts(OfflineConflictStatus status) {
        if (status == null) {
            return conflicts.findAllByOrderByUpdatedAtDesc();
        }
        return conflicts.findByStatusOrderByUpdatedAtDesc(status);
    }

    @Transactional
    public OfflineSaleConflictRecord resolveConflict(
            Long conflictId,
            ConflictResolutionAction action,
            String note,
            Long resolvedByUserId
    ) {
        OfflineSaleConflictRecord conflict = conflicts.findById(conflictId)
                .orElseThrow(() -> new EntityNotFoundException("Conflito offline nao encontrado."));
        if (conflict.getStatus() != OfflineConflictStatus.PENDING) {
            throw new IllegalArgumentException("Conflito offline ja foi resolvido.");
        }
        if (action == ConflictResolutionAction.ACCEPT) {
            // Venda ja entregue ao cliente: preserva operador, preco praticado e data; estoque pode ficar negativo.
            OfflineSaleData data = readPayload(conflict.getSalePayload());
            Sale sale = salesService.confirmSale(toCommand(data), conflict.getOperatorUserId(), null, true);
            conflict.accept(sale.getId(), resolvedByUserId, clean(note));
            auditService.record("OFFLINE_CONFLICT_ACCEPTED", "offline_sale_conflict", conflict.getId(), resolvedByUserId, Map.of(
                    "localSaleId", conflict.getLocalSaleId(),
                    "remoteSaleId", sale.getId()
            ));
        } else {
            String rejectionNote = clean(note);
            if (rejectionNote == null) {
                throw new IllegalArgumentException("Informe uma observacao para rejeitar o conflito offline.");
            }
            conflict.reject(resolvedByUserId, rejectionNote);
            auditService.record("OFFLINE_CONFLICT_REJECTED", "offline_sale_conflict", conflict.getId(), resolvedByUserId, Map.of(
                    "localSaleId", conflict.getLocalSaleId()
            ));
        }
        return conflict;
    }

    private List<OfflineSaleConflict> validate(OfflineSaleData data, UserRole operatorRole) {
        List<OfflineSaleConflict> found = new ArrayList<>();
        BigDecimal itemTotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> requestedByProduct = new HashMap<>();
        Map<Long, Product> productsById = new HashMap<>();
        for (OfflineSaleItemData item : data.items()) {
            requestedByProduct.merge(item.productId(), item.quantity(), BigDecimal::add);
            itemTotal = itemTotal.add(item.unitPrice().multiply(item.quantity()).setScale(2, RoundingMode.HALF_UP));
            Product product = catalog.findProduct(item.productId()).orElse(null);
            if (product == null) {
                found.add(new OfflineSaleConflict("PRODUCT_NOT_FOUND", "Produto " + item.productId() + " nao encontrado."));
                continue;
            }
            productsById.put(product.getId(), product);
            if (!product.isActive()) {
                found.add(new OfflineSaleConflict("PRODUCT_INACTIVE", "Produto " + product.getName() + " esta inativo."));
            }
            if (product.getSalePrice().compareTo(item.unitPrice()) != 0) {
                found.add(new OfflineSaleConflict("PRICE_CHANGED", "Preco vigente de " + product.getName() + " mudou."));
            }
        }
        requestedByProduct.forEach((productId, requested) -> {
            Product product = productsById.get(productId);
            if (product != null && inventory.availableQuantity(productId).compareTo(requested) < 0) {
                found.add(new OfflineSaleConflict("STOCK_INSUFFICIENT", "Estoque insuficiente para " + product.getName() + "."));
            }
        });
        BigDecimal discount = data.discountAmount() == null
                ? BigDecimal.ZERO
                : data.discountAmount().setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() < 0 || (discount.signum() > 0 && discount.compareTo(itemTotal) >= 0)) {
            found.add(new OfflineSaleConflict("DISCOUNT_INVALID", "Desconto deve ser positivo e menor que o total bruto."));
        } else if (!salesService.discountAllowed(operatorRole, discount, itemTotal)) {
            found.add(new OfflineSaleConflict("DISCOUNT_NOT_ALLOWED", "Desconto acima do permitido para o perfil do operador."));
        }
        BigDecimal paidTotal = data.payments().stream()
                .map(SalePaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (itemTotal.subtract(discount).compareTo(paidTotal) != 0) {
            found.add(new OfflineSaleConflict("PAYMENT_TOTAL", "Valor pago difere do total da venda offline."));
        }
        return found;
    }

    private static SaleCommand toCommand(OfflineSaleData data) {
        return new SaleCommand(
                data.customerId(),
                data.items().stream().map(item -> new SaleItemData(item.productId(), item.quantity(), item.unitPrice())).toList(),
                data.payments(),
                data.discountAmount(),
                data.localSaleId(),
                data.createdAt()
        );
    }

    private LocalSaleRemoteStatus remoteStatusOf(String localSaleId) {
        Optional<Sale> sale = salesService.findByClientSaleId(localSaleId);
        if (sale.isPresent()) {
            return new LocalSaleRemoteStatus(localSaleId, "SENT", sale.get().getId(), null);
        }
        return conflicts.findFirstByLocalSaleIdOrderByUpdatedAtDesc(localSaleId)
                .map(conflict -> switch (conflict.getStatus()) {
                    case PENDING -> new LocalSaleRemoteStatus(localSaleId, "CONFLICT", null, null);
                    case ACCEPTED -> new LocalSaleRemoteStatus(localSaleId, "SENT", conflict.getRemoteSaleId(), conflict.getResolutionNote());
                    case REJECTED -> new LocalSaleRemoteStatus(localSaleId, "REJECTED", null, conflict.getResolutionNote());
                })
                .orElseGet(() -> new LocalSaleRemoteStatus(localSaleId, "UNKNOWN", null, null));
    }

    private void saveConflict(OfflineSaleData data, Long operatorUserId, List<OfflineSaleConflict> saleConflicts) {
        String summary = String.join("\n", saleConflicts.stream()
                .map(conflict -> conflict.code() + ": " + conflict.message())
                .toList());
        String payload = writePayload(data);
        OfflineSaleConflictRecord conflict = conflicts
                .findFirstByLocalSaleIdAndStatusOrderByUpdatedAtDesc(data.localSaleId(), OfflineConflictStatus.PENDING)
                .orElseGet(() -> new OfflineSaleConflictRecord(
                        data.localSaleId(),
                        data.createdAt(),
                        data.customerId(),
                        operatorUserId,
                        summary,
                        payload
                ));
        conflict.refresh(summary, payload);
        conflicts.save(conflict);
        auditService.record("OFFLINE_SALE_CONFLICT", "offline_sale", data.localSaleId(), operatorUserId, Map.of(
                "conflictCount", saleConflicts.size()
        ));
    }

    private String writePayload(OfflineSaleData data) {
        try {
            return objectMapper.writeValueAsString(OfflineSalePayload.from(data));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel registrar venda offline em conflito.", exception);
        }
    }

    private OfflineSaleData readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, OfflineSalePayload.class).toData();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel ler venda offline em conflito.", exception);
        }
    }

    private static String clean(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    public record OfflineSaleData(
            String localSaleId,
            Instant createdAt,
            Long customerId,
            List<OfflineSaleItemData> items,
            List<SalePaymentData> payments,
            BigDecimal discountAmount
    ) {
    }

    public record OfflineSaleItemData(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record OfflineSaleConflict(String code, String message) {
    }

    public record OfflineSaleSyncResult(String status, Sale sale, List<OfflineSaleConflict> conflicts) {
    }

    public record LocalSaleRemoteStatus(String localSaleId, String status, Long remoteSaleId, String note) {
    }

    public enum ConflictResolutionAction {
        ACCEPT,
        REJECT
    }

    private record OfflineSalePayload(
            String localSaleId,
            String createdAt,
            Long customerId,
            List<OfflineSaleItemData> items,
            List<SalePaymentData> payments,
            BigDecimal discountAmount
    ) {
        static OfflineSalePayload from(OfflineSaleData data) {
            return new OfflineSalePayload(
                    data.localSaleId(),
                    data.createdAt().toString(),
                    data.customerId(),
                    data.items(),
                    data.payments(),
                    data.discountAmount()
            );
        }

        OfflineSaleData toData() {
            return new OfflineSaleData(localSaleId, Instant.parse(createdAt), customerId, items, payments, discountAmount);
        }
    }
}
