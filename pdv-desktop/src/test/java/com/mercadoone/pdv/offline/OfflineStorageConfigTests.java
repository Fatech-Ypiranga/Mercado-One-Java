package com.mercadoone.pdv.offline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineStorageConfigTests {

    @Test
    void createsSqliteJdbcUrlForLocalPdvStorage() {
        OfflineStorageConfig config = OfflineStorageConfig.defaultConfig();

        assertTrue(config.databasePath().toString().endsWith("pdv-offline.sqlite3"));
        assertTrue(config.jdbcUrl().startsWith("jdbc:sqlite:"));
    }

    @Test
    void environmentOverrideReplacesDefaultPath() {
        OfflineStorageConfig config = OfflineStorageConfig.fromEnvironment("C:/caixa-2/pdv.sqlite3", "/home/x");
        assertTrue(config.databasePath().endsWith(java.nio.file.Path.of("caixa-2", "pdv.sqlite3")));

        OfflineStorageConfig fallback = OfflineStorageConfig.fromEnvironment(" ", "/home/x");
        assertTrue(fallback.databasePath().endsWith(java.nio.file.Path.of(".mercado-one", "pdv-offline.sqlite3")));
    }
}
