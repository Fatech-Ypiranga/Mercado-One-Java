package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeScanTests {

    private static CatalogProduct product(long id, String name, String barcode, String sku) {
        return new CatalogProduct(id, name, barcode, sku, "UN", new BigDecimal("10.00"));
    }

    private final CatalogProduct arroz = product(1, "Arroz 5kg", "7891000100103", "ARROZ-5");
    private final CatalogProduct feijao = product(2, "Feijao 1kg", "7896006711117", "FEIJAO-1");

    @Test
    void exactBarcodeMatchPicksTheProduct() {
        assertEquals(arroz, BarcodeScan.exactMatch("7891000100103", List.of(arroz)).orElseThrow());
    }

    @Test
    void exactMatchIgnoresSurroundingSpacesAndCaseInSku() {
        assertEquals(arroz, BarcodeScan.exactMatch("  arroz-5 \r\n", List.of(arroz, feijao)).orElseThrow());
    }

    @Test
    void partialBarcodeIsNotAnExactMatch() {
        assertTrue(BarcodeScan.exactMatch("789100", List.of(arroz)).isEmpty());
    }

    @Test
    void nameSearchIsNeverAutoAdded() {
        assertTrue(BarcodeScan.exactMatch("Arroz 5kg", List.of(arroz)).isEmpty());
    }

    @Test
    void twoProductsWithTheSameCodeAreAmbiguousAndNotAutoAdded() {
        CatalogProduct clone = product(3, "Arroz 5kg (outro)", "7891000100103", "ARROZ-X");
        assertTrue(BarcodeScan.exactMatch("7891000100103", List.of(arroz, clone)).isEmpty());
    }

    @Test
    void blankTermOrEmptyResultsGiveNoMatch() {
        assertTrue(BarcodeScan.exactMatch("", List.of(arroz)).isEmpty());
        assertTrue(BarcodeScan.exactMatch(null, List.of(arroz)).isEmpty());
        assertTrue(BarcodeScan.exactMatch("7891000100103", List.of()).isEmpty());
    }

    @Test
    void recognizesTypicalRetailBarcodes() {
        assertTrue(BarcodeScan.looksLikeBarcode("7891000100103"));   // EAN-13
        assertTrue(BarcodeScan.looksLikeBarcode("96385074"));        // EAN-8
        assertTrue(BarcodeScan.looksLikeBarcode("036000291452"));    // UPC-A
        assertTrue(BarcodeScan.looksLikeBarcode(" 7891000100103\n"));
    }

    @Test
    void doesNotTreatNamesQuantitiesOrShortNumbersAsBarcodes() {
        assertFalse(BarcodeScan.looksLikeBarcode("arroz"));
        assertFalse(BarcodeScan.looksLikeBarcode("ARROZ-5"));
        assertFalse(BarcodeScan.looksLikeBarcode("123"));
        assertFalse(BarcodeScan.looksLikeBarcode(""));
        assertFalse(BarcodeScan.looksLikeBarcode(null));
    }
}
