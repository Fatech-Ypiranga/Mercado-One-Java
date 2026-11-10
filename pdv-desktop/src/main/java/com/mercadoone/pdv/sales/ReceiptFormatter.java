package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.sales.CheckoutService.CheckoutResult;
import com.mercadoone.pdv.sales.CheckoutService.Draft;
import com.mercadoone.pdv.sales.CheckoutService.DraftLine;
import com.mercadoone.pdv.sales.CheckoutService.Outcome;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Comprovante simples, sem valor fiscal. */
public final class ReceiptFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReceiptFormatter() {
    }

    public static String format(Draft draft, CheckoutResult result, ZoneId zone) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Mercado One\n");
        receipt.append("Comprovante simples - SEM VALOR FISCAL\n");
        receipt.append("Venda local: ").append(result.sale().localSaleId()).append('\n');
        if (result.remoteSaleId() != null) {
            receipt.append("Venda no servidor: #").append(result.remoteSaleId()).append('\n');
        }
        receipt.append("Data: ").append(DATE_TIME.format(result.sale().createdAt().atZone(zone))).append('\n');
        receipt.append("------------------------------\n");
        for (DraftLine line : draft.lines()) {
            receipt.append(line.productName()).append('\n')
                    .append("  ").append(line.quantity().stripTrailingZeros().toPlainString())
                    .append(" x ").append(money(line.unitPrice()))
                    .append(" = ").append(money(line.totalAmount()))
                    .append('\n');
        }
        receipt.append("------------------------------\n");
        receipt.append("Subtotal: ").append(money(draft.grossAmount())).append('\n');
        if (draft.discountAmount().signum() > 0) {
            receipt.append("Desconto: -").append(money(draft.discountAmount())).append('\n');
        }
        receipt.append("Total: ").append(money(draft.netAmount())).append('\n');
        for (LocalSalePayment payment : draft.payments()) {
            receipt.append("Pago em ").append(payment.method().displayName()).append(": ").append(money(payment.amount())).append('\n');
        }
        if (result.outcome() == Outcome.QUEUED_OFFLINE) {
            receipt.append("* Venda registrada offline; sujeita a sincronizacao.\n");
        }
        return receipt.toString();
    }

    public static String money(BigDecimal value) {
        return "R$ " + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
