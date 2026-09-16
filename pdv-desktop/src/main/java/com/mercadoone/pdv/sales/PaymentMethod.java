package com.mercadoone.pdv.sales;

public enum PaymentMethod {
    CASH("Dinheiro"),
    CARD("Cartao"),
    PIX("PIX"),
    STORE_CREDIT("Fiado");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
