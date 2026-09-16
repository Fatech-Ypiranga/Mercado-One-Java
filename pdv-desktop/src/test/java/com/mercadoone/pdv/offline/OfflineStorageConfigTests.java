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
}
