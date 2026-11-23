package com.mercadoone.pdv.offline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorCredentialStoreTests {

    @TempDir
    Path tempDir;

    private OfflineStorageConfig config;
    private OperatorCredentialStore store;

    @BeforeEach
    void setUp() {
        config = new OfflineStorageConfig(tempDir.resolve("pdv.sqlite3"));
        // Poucas iterações só para o teste ser rápido; em produção vale DEFAULT_ITERATIONS.
        store = new OperatorCredentialStore(config, 1_000);
        store.initialize();
    }

    @Test
    void anOperatorWhoLoggedInOnlineCanEnterOfflineWithTheSamePassword() {
        store.remember("Maria", "Maria Souza", "senha-forte-1".toCharArray());

        assertEquals(Optional.of("Maria Souza"), store.verify("maria", "senha-forte-1".toCharArray()));
        assertEquals(Optional.of("Maria Souza"), store.verify("  MARIA ", "senha-forte-1".toCharArray()));
    }

    @Test
    void wrongPasswordUnknownOperatorAndEmptyPasswordAreRefused() {
        store.remember("maria", "Maria Souza", "senha-forte-1".toCharArray());

        assertTrue(store.verify("maria", "outra-senha".toCharArray()).isEmpty());
        assertTrue(store.verify("joao", "senha-forte-1".toCharArray()).isEmpty());
        assertTrue(store.verify("maria", new char[0]).isEmpty());
        assertTrue(store.verify("", "senha-forte-1".toCharArray()).isEmpty());
        assertFalse(store.knows("joao"));
        assertTrue(store.knows("Maria"));
    }

    @Test
    void anOnlineLoginWithANewPasswordReplacesTheOldOne() {
        store.remember("maria", "Maria Souza", "senha-antiga".toCharArray());
        store.remember("maria", "Maria S.", "senha-nova-2".toCharArray());

        assertTrue(store.verify("maria", "senha-antiga".toCharArray()).isEmpty());
        assertEquals(Optional.of("Maria S."), store.verify("maria", "senha-nova-2".toCharArray()));
    }

    @Test
    void thePasswordIsNeverWrittenToTheDatabaseFile() throws Exception {
        store.remember("maria", "Maria Souza", "senha-muito-unica-123".toCharArray());

        String raw = new String(Files.readAllBytes(config.databasePath()), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("senha-muito-unica-123"));
    }

    @Test
    void twoOperatorsWithTheSamePasswordGetDifferentHashes() throws Exception {
        store.remember("ana", "Ana", "mesma-senha-9".toCharArray());
        store.remember("bia", "Bia", "mesma-senha-9".toCharArray());

        try (var connection = java.sql.DriverManager.getConnection(config.jdbcUrl());
             var rows = connection.createStatement().executeQuery("select count(distinct hash) from operator_credentials")) {
            rows.next();
            assertEquals(2, rows.getInt(1));
        }
    }
}
