package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.sales.CheckoutService.DraftLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartLinesTests {

    private static DraftLine line(long productId, String qty, String price) {
        return new DraftLine(productId, "Produto " + productId, new BigDecimal(qty), new BigDecimal(price));
    }

    @Test
    void scanningTheSameProductAgainAddsToTheExistingLine() {
        List<DraftLine> cart = new ArrayList<>();
        CartLines.add(cart, line(1, "1.000", "10.00"));
        CartLines.add(cart, line(1, "1.000", "10.00"));
        CartLines.add(cart, line(1, "3.000", "10.00"));

        assertEquals(1, cart.size());
        assertEquals(0, new BigDecimal("5").compareTo(cart.getFirst().quantity()));
    }

    @Test
    void differentProductsKeepSeparateLinesInOrder() {
        List<DraftLine> cart = new ArrayList<>();
        CartLines.add(cart, line(1, "1", "10.00"));
        CartLines.add(cart, line(2, "1", "5.00"));
        CartLines.add(cart, line(1, "1", "10.00"));

        assertEquals(2, cart.size());
        assertEquals(1L, cart.get(0).productId());
        assertEquals(2L, cart.get(1).productId());
        assertEquals(0, new BigDecimal("2").compareTo(cart.get(0).quantity()));
    }

    @Test
    void sameProductAtADifferentPriceIsNotMerged() {
        List<DraftLine> cart = new ArrayList<>();
        CartLines.add(cart, line(1, "1", "10.00"));
        CartLines.add(cart, line(1, "1", "12.00"));

        assertEquals(2, cart.size());
    }

    @Test
    void mergedQuantityKeepsThreeDecimalPlaces() {
        List<DraftLine> cart = new ArrayList<>();
        CartLines.add(cart, line(1, "0.500", "10.00"));
        CartLines.add(cart, line(1, "0.250", "10.00"));

        assertEquals(new BigDecimal("0.750"), cart.getFirst().quantity());
    }
}
