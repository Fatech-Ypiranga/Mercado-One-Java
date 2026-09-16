package com.mercadoone.pdv.sync;

public enum SyncStatus {
    ONLINE("Online"),
    OFFLINE_READY("Offline pronto"),
    SYNC_PENDING("Sincronizacao pendente"),
    SYNC_FAILED("Falha de sincronizacao");

    private final String displayName;

    SyncStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
