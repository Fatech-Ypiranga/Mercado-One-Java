package com.mercadoone.pdv.offline;

import java.nio.file.Path;

public record OfflineStorageConfig(Path databasePath) {

    /** Variavel de ambiente opcional para apontar o SQLite local para outro arquivo. */
    public static final String DATABASE_PATH_ENV = "MERCADO_ONE_PDV_DB_PATH";

    public static OfflineStorageConfig defaultConfig() {
        return fromEnvironment(System.getenv(DATABASE_PATH_ENV), System.getProperty("user.home"));
    }

    static OfflineStorageConfig fromEnvironment(String overridePath, String userHome) {
        if (overridePath != null && !overridePath.isBlank()) {
            return new OfflineStorageConfig(Path.of(overridePath.trim()));
        }
        return new OfflineStorageConfig(Path.of(userHome, ".mercado-one", "pdv-offline.sqlite3"));
    }

    public String jdbcUrl() {
        return "jdbc:sqlite:" + databasePath;
    }
}
