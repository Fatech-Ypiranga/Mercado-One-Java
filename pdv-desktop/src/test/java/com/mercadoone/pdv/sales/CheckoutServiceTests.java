package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.LocalSaleStatus;
import com.mercadoone.pdv.offline.OfflineSaleQueue;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.offline.OfflineStorageConfig;
import com.mercadoone.pdv.sales.CheckoutService.CheckoutResult;
import com.mercadoone.pdv.sales.CheckoutService.Draft;
import com.mercadoone.pdv.sales.CheckoutService.DraftLine;
import com.mercadoone.pdv.sales.CheckoutService.Outcome;
import com.mercadoone.pdv.sales.CheckoutService.Session;
import com.mercadoone.pdv.sync.SyncStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutServiceTests {

    private static final Session SESSION = new Session("http://api", "token");

    @TempDir
    Path tempDir;

    private OfflineSaleQueue queue;
    private final List<String> calls = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private Function<HttpRequest, OnlineSaleClient.ApiResponse> responder;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        queue = new OfflineSaleQueue(new OfflineStorageConfig(tempDir.resolve("pdv.sqlite3")));
        queue.initialize();
        AtomicInteger ids = new AtomicInteger();
        OnlineSaleClient client = new OnlineSaleClient(request -> {
            calls.add(request.method() + " " + request.uri().getPath());
            bodies.add(body(request));
            return responder.apply(request);
        });
        service = new CheckoutService(queue, client,
                Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC),
                () -> "local-" + ids.incrementAndGet());
    }

    @Test
    void confirmsOnlineWithClientSaleIdAndMarksSent() {
        responder = request -> ok("{\"id\":41}");

        CheckoutResult result = service.checkout(SESSION, draft("0", payment(PaymentMethod.CASH, "20.00")));

        assertEquals(Outcome.CONFIRMED_ONLINE, result.outcome());
        assertEquals(41L, result.remoteSaleId());
        assertEquals(List.of("POST /api/sales"), calls);
        assertTrue(bodies.getFirst().contains("\"clientSaleId\":\"local-1\""));
        assertEquals(LocalSaleStatus.SENT, queue.recentSales(1).getFirst().status());
        assertEquals(SyncStatus.ONLINE, service.currentStatus());
    }

    @Test
    void queuesOfflineWhenApiIsUnavailableAndResendsLater() {
        responder = request -> {
            throw new OnlineSaleClient.ApiUnavailableException("Nao foi possivel comunicar com a API.", null);
        };

        CheckoutResult result = service.checkout(SESSION, draft("0", payment(PaymentMethod.PIX, "20.00")));

        assertEquals(Outcome.QUEUED_OFFLINE, result.outcome());
        assertTrue(result.completed());
        assertEquals(SyncStatus.SYNC_PENDING, service.currentStatus());

        responder = request -> ok("{\"status\":\"SENT\",\"sale\":{\"id\":7},\"conflicts\":[]}");
        var summary = service.synchronize(SESSION);

        assertEquals(1, summary.sent());
        assertTrue(bodies.getLast().contains("\"createdAt\":\"2026-11-10T12:00:00Z\""));
        assertTrue(bodies.getLast().contains("\"localSaleId\":\"local-1\""));
        assertEquals(LocalSaleStatus.SENT, queue.recentSales(1).getFirst().status());
        assertEquals(SyncStatus.ONLINE, service.currentStatus());
    }

    @Test
    void businessRejectionMarksSaleRejectedAndKeepsItOutOfRetry() {
        responder = request -> new OnlineSaleClient.ApiResponse(400,
                "{\"success\":false,\"error\":{\"code\":\"BUSINESS_RULE\",\"message\":\"Estoque insuficiente para finalizar a venda.\"}}");

        CheckoutResult result = service.checkout(SESSION, draft("0", payment(PaymentMethod.CARD, "20.00")));

        assertEquals(Outcome.REJECTED, result.outcome());
        assertTrue(result.message().contains("Estoque insuficiente"));
        assertEquals(LocalSaleStatus.REJECTED, queue.recentSales(1).getFirst().status());
        assertTrue(queue.pendingSales().isEmpty());
    }

    @Test
    void expiredSessionKeepsSaleQueued() {
        responder = request -> new OnlineSaleClient.ApiResponse(401, "");

        CheckoutResult result = service.checkout(SESSION, draft("0", payment(PaymentMethod.CASH, "20.00")));

        assertEquals(Outcome.QUEUED_OFFLINE, result.outcome());
        assertEquals(1, queue.pendingSales().size());
    }

    @Test
    void withoutSessionSaleGoesStraightToQueue() {
        responder = request -> {
            throw new AssertionError("Nao deve chamar a API sem sessao.");
        };

        CheckoutResult result = service.checkout(new Session("http://api", ""), draft("0", payment(PaymentMethod.CASH, "20.00")));

        assertEquals(Outcome.QUEUED_OFFLINE, result.outcome());
        assertTrue(calls.isEmpty());
        assertEquals(SyncStatus.SYNC_PENDING, service.currentStatus());
    }

    @Test
    void validatesMultiplePaymentsDiscountAndPolicy() {
        responder = request -> ok("{\"role\":\"OPERADOR_CAIXA\",\"maxDiscountPercent\":10}");
        service.refreshDiscountLimit(SESSION);

        assertEquals("Falta registrar R$ 5.00 em pagamentos.",
                service.validate(draft("0", payment(PaymentMethod.CASH, "15.00"))));
        assertTrue(service.validate(draft("3.00", payment(PaymentMethod.CASH, "17.00"))).contains("Desconto acima"));
        assertNull(service.validate(draft("2.00", payment(PaymentMethod.CASH, "10.00"), payment(PaymentMethod.PIX, "8.00"))));

        responder = request -> ok("{\"id\":9}");
        CheckoutResult result = service.checkout(SESSION,
                draft("2.00", payment(PaymentMethod.CASH, "10.00"), payment(PaymentMethod.PIX, "8.00")));

        assertEquals(Outcome.CONFIRMED_ONLINE, result.outcome());
        String body = bodies.getLast();
        assertTrue(body.contains("\"discountAmount\":2.00"));
        assertTrue(body.contains("\"method\":\"PIX\""));
        assertEquals(new BigDecimal("18.00"), queue.recentSales(1).getFirst().netAmount());
    }

    @Test
    void reflectsConflictResolutionFromAdmin() {
        responder = request -> {
            throw new OnlineSaleClient.ApiUnavailableException("fora", null);
        };
        service.checkout(SESSION, draft("0", payment(PaymentMethod.CASH, "20.00")));
        service.checkout(SESSION, draft("0", payment(PaymentMethod.CASH, "20.00")));

        responder = request -> ok("{\"status\":\"CONFLICT\",\"sale\":null,\"conflicts\":[{\"code\":\"PRICE_CHANGED\",\"message\":\"Preco mudou.\"}]}");
        service.synchronize(SESSION);
        assertEquals(List.of("local-1", "local-2"), queue.conflictSaleIds());
        assertEquals(SyncStatus.SYNC_FAILED, service.currentStatus());

        responder = request -> {
            if (request.uri().getPath().endsWith("/status")) {
                assertTrue(request.uri().getQuery().contains("localSaleIds=local-1"));
                return ok("""
                        [{"localSaleId":"local-1","status":"SENT","remoteSaleId":99,"note":"ok"},
                         {"localSaleId":"local-2","status":"REJECTED","remoteSaleId":null,"note":"Duplicada"}]
                        """);
            }
            throw new AssertionError("Fila sem pendencias nao deve reenviar.");
        };
        var summary = service.synchronize(SESSION);

        assertEquals(1, summary.acceptedInAdmin());
        assertEquals(1, summary.rejectedInAdmin());
        assertTrue(queue.conflictSaleIds().isEmpty());
        var recent = queue.recentSales(2);
        assertTrue(recent.stream().anyMatch(sale -> sale.status() == LocalSaleStatus.SENT && Long.valueOf(99).equals(sale.remoteSaleId())));
        assertTrue(recent.stream().anyMatch(sale -> sale.status() == LocalSaleStatus.REJECTED && sale.lastMessage().contains("Duplicada")));
        assertEquals(SyncStatus.ONLINE, service.currentStatus());
    }

    @Test
    void receiptShowsDiscountPaymentsAndNonFiscalNotice() {
        responder = request -> ok("{\"role\":\"GERENTE\",\"maxDiscountPercent\":null}");
        service.refreshDiscountLimit(SESSION);
        assertTrue(service.discountLimit().unlimited());
        responder = request -> ok("{\"id\":5}");
        Draft draft = draft("2.00", payment(PaymentMethod.CASH, "10.00"), payment(PaymentMethod.PIX, "8.00"));
        CheckoutResult result = service.checkout(SESSION, draft);

        String text = ReceiptFormatter.format(draft, result, ZoneOffset.UTC);

        assertNotNull(text);
        assertTrue(text.contains("SEM VALOR FISCAL"));
        assertTrue(text.contains("Desconto: -R$ 2,00"));
        assertTrue(text.contains("Total: R$ 18,00"));
        assertTrue(text.contains("Pago em PIX: R$ 8,00"));
        assertTrue(text.contains("#5"));
    }

    private static Draft draft(String discount, LocalSalePayment... payments) {
        return new Draft(null, List.of(new DraftLine(1L, "Cafe", new BigDecimal("2.000"), new BigDecimal("10.00"))),
                new BigDecimal(discount), List.of(payments));
    }

    private static LocalSalePayment payment(PaymentMethod method, String amount) {
        return new LocalSalePayment(method, new BigDecimal(amount));
    }

    private static OnlineSaleClient.ApiResponse ok(String data) {
        return new OnlineSaleClient.ApiResponse(200, "{\"success\":true,\"data\":" + data + "}");
    }

    private static String body(HttpRequest request) {
        return request.bodyPublisher().map(publisher -> {
            StringBuilder content = new StringBuilder();
            publisher.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    content.append(StandardCharsets.UTF_8.decode(item));
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });
            return content.toString();
        }).orElse("");
    }
}
