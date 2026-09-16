package com.mercadoone.pdv.offline;

import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSale;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSaleItem;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.sales.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfflineSaleQueueTests {

    @TempDir
    Path tempDir;

    @Test
    void createsSchemaPersistsSaleAndUpdatesStatus() {
        OfflineSaleQueue queue = new OfflineSaleQueue(new OfflineStorageConfig(tempDir.resolve("pdv.sqlite3")));
        queue.initialize();

        queue.enqueue(new LocalSale(
                "local-1",
                Instant.parse("2026-09-09T00:00:00Z"),
                null,
                LocalSaleStatus.PENDING,
                List.of(new LocalSaleItem(10L, new BigDecimal("2.000"), new BigDecimal("8.50"))),
                List.of(new LocalSalePayment(PaymentMethod.CASH, new BigDecimal("17.00")))
        ));

        List<LocalSale> pending = queue.pendingSales();
        assertEquals(1, pending.size());
        assertEquals(LocalSaleStatus.PENDING, pending.getFirst().status());
        assertEquals(new BigDecimal("8.50"), pending.getFirst().items().getFirst().unitPrice());

        queue.markConflict("local-1", "Preco mudou.");
        assertEquals(0, queue.pendingSales().size());
    }

    @Test
    void retriesErroredSalesAsPendingWork() {
        OfflineSaleQueue queue = new OfflineSaleQueue(new OfflineStorageConfig(tempDir.resolve("pdv-retry.sqlite3")));
        queue.initialize();

        queue.enqueue(new LocalSale(
                "local-2",
                Instant.now(),
                5L,
                LocalSaleStatus.PENDING,
                List.of(new LocalSaleItem(11L, new BigDecimal("1.000"), new BigDecimal("3.00"))),
                List.of(new LocalSalePayment(PaymentMethod.PIX, new BigDecimal("3.00")))
        ));

        queue.markError("local-2", "API fora.");

        assertEquals(1, queue.pendingSales().size());
        assertEquals(LocalSaleStatus.ERROR, queue.pendingSales().getFirst().status());
    }
}
