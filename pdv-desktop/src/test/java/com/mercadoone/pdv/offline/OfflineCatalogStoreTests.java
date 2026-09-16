package com.mercadoone.pdv.offline;

import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfflineCatalogStoreTests {

    @TempDir
    Path tempDir;

    @Test
    void replacesAndSearchesLocalCatalogByNameSkuOrBarcode() {
        OfflineCatalogStore store = new OfflineCatalogStore(new OfflineStorageConfig(tempDir.resolve("catalog.sqlite3")));
        store.initialize();

        store.replaceAll(List.of(
                new CatalogProduct(1L, "Cafe Premium", "789100000001", "CAF-001", "UN", new BigDecimal("12.50")),
                new CatalogProduct(2L, "Arroz Tipo 1", "789100000002", "ARR-001", "UN", new BigDecimal("22.90"))
        ));

        assertEquals("Cafe Premium", store.search("caf").getFirst().name());
        assertEquals("Cafe Premium", store.search("CAF-001").getFirst().name());
        assertEquals("Arroz Tipo 1", store.search("000002").getFirst().name());

        store.replaceAll(List.of(new CatalogProduct(3L, "Feijao", null, "FEI-001", "UN", new BigDecimal("8.90"))));

        assertEquals(0, store.search("caf").size());
        assertEquals("Feijao", store.search("").getFirst().name());
    }

    @Test
    void replacesExistingProductPriceOnCatalogRefresh() {
        OfflineCatalogStore store = new OfflineCatalogStore(new OfflineStorageConfig(tempDir.resolve("catalog-price.sqlite3")));
        store.initialize();

        store.replaceAll(List.of(new CatalogProduct(1L, "Cafe Premium", "789100000001", "CAF-001", "UN", new BigDecimal("12.50"))));
        store.replaceAll(List.of(new CatalogProduct(1L, "Cafe Premium", "789100000001", "CAF-001", "UN", new BigDecimal("13.90"))));

        assertEquals(new BigDecimal("13.90"), store.search("CAF-001").getFirst().salePrice());
    }
}
