package com.mercadoone.pdv.offline;

public enum LocalSaleStatus {
    PENDING,
    SENT,
    CONFLICT,
    ERROR,
    /** Recusada pela API (venda online invalida) ou conflito rejeitado no admin. Nao e reenviada. */
    REJECTED
}
