package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.sales.CheckoutService.DraftLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Operações sobre as linhas do carrinho do PDV. */
public final class CartLines {

    private CartLines() {
    }

    /**
     * Adiciona a linha ao carrinho. Se já existe uma linha do mesmo produto, com o mesmo preço,
     * soma a quantidade nela em vez de criar outra: ler o mesmo produto três vezes dá uma linha
     * de quantidade 3.
     */
    public static void add(List<DraftLine> cart, DraftLine line) {
        for (int i = 0; i < cart.size(); i++) {
            DraftLine existing = cart.get(i);
            if (existing.productId().equals(line.productId()) && existing.unitPrice().compareTo(line.unitPrice()) == 0) {
                BigDecimal total = existing.quantity().add(line.quantity()).setScale(3, RoundingMode.HALF_UP);
                cart.set(i, new DraftLine(existing.productId(), existing.productName(), total, existing.unitPrice()));
                return;
            }
        }
        cart.add(line);
    }
}
