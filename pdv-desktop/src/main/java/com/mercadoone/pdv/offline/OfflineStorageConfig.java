package com.mercadoone.pdv.offline;

import java.nio.file.Path;

public record OfflineStorageConfig(Path databasePath) {

    public static OfflineStorageConfig defaultConfig() {
        String userHome = System.getProperty("user.home");
        Path databasePath = Path.of(userHome, ".mercado-one", "pdv-offline.sqlite3");

        return new OfflineStorageConfig(databasePath);
    }

    public String jdbcUrl() {
        return "jdbc:sqlite:" + databasePath;
    }
}
