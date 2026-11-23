package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.OfflineStorageConfig;
import com.mercadoone.pdv.offline.OperatorCredentialStore;
import com.mercadoone.pdv.sales.OperatorAccess.AccessDeniedException;
import com.mercadoone.pdv.sales.OperatorAccess.Entry;
import com.mercadoone.pdv.sales.OperatorAccess.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorAccessTests {

    @TempDir
    Path tempDir;

    private Function<HttpRequest, OnlineSaleClient.ApiResponse> responder;
    private OperatorCredentialStore store;
    private OperatorAccess access;

    @BeforeEach
    void setUp() {
        store = new OperatorCredentialStore(new OfflineStorageConfig(tempDir.resolve("pdv.sqlite3")), 1_000);
        store.initialize();
        access = new OperatorAccess(new OnlineSaleClient(request -> responder.apply(request)), store);
    }

    private static OnlineSaleClient.ApiResponse loginOk() {
        return new OnlineSaleClient.ApiResponse(200, """
                {"success":true,"data":{"accessToken":"tok-123","tokenType":"Bearer","user":{"id":4,"nome":"Maria Souza","login":"maria","perfil":"OPERADOR_CAIXA"}}}
                """);
    }

    private void apiDown() {
        responder = request -> {
            throw new OnlineSaleClient.ApiUnavailableException("fora", null);
        };
    }

    @Test
    void onlineLoginReturnsTokenAndNameAndRemembersTheCredential() {
        responder = request -> loginOk();

        Entry entry = access.enter("http://api", " maria ", "senha-1".toCharArray());

        assertEquals(Mode.ONLINE, entry.mode());
        assertEquals("maria", entry.login());
        assertEquals("Maria Souza", entry.name());
        assertEquals("tok-123", entry.token());
        assertTrue(store.knows("maria"));
    }

    @Test
    void withTheApiDownAKnownOperatorEntersOfflineWithoutToken() {
        responder = request -> loginOk();
        access.enter("http://api", "maria", "senha-1".toCharArray());
        apiDown();

        Entry entry = access.enter("http://api", "maria", "senha-1".toCharArray());

        assertEquals(Mode.OFFLINE, entry.mode());
        assertEquals("Maria Souza", entry.name());
        assertEquals("", entry.token());
    }

    @Test
    void withTheApiDownAWrongPasswordIsRefusedEvenForAKnownOperator() {
        responder = request -> loginOk();
        access.enter("http://api", "maria", "senha-1".toCharArray());
        apiDown();

        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> access.enter("http://api", "maria", "senha-errada".toCharArray()));

        assertEquals(OperatorAccess.WRONG_CREDENTIALS, denied.getMessage());
    }

    @Test
    void withTheApiDownAnOperatorWhoNeverLoggedInHereIsRefusedWithAnExplanation() {
        apiDown();

        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> access.enter("http://api", "joao", "qualquer".toCharArray()));

        assertEquals(OperatorAccess.NO_CONNECTION_NO_ACCESS, denied.getMessage());
    }

    @Test
    void anExpiredLocalCredentialExplainsWhyOfflineEntryIsRefused() throws Exception {
        java.nio.file.Path dir = tempDir.resolve("antigo");
        OfflineStorageConfig config = new OfflineStorageConfig(dir.resolve("pdv.sqlite3"));
        java.time.Clock longAgo = java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
        OperatorCredentialStore old = new OperatorCredentialStore(config, 1_000, java.time.Duration.ofDays(30), longAgo);
        old.initialize();
        old.remember("maria", "Maria Souza", "senha-1".toCharArray());
        OperatorAccess stale = new OperatorAccess(new OnlineSaleClient(request -> responder.apply(request)),
                new OperatorCredentialStore(config, 1_000));
        apiDown();

        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> stale.enter("http://api", "maria", "senha-1".toCharArray()));

        assertEquals(OperatorAccess.EXPIRED_CREDENTIAL, denied.getMessage());
    }

    @Test
    void whenTheServerRefusesThePasswordThereIsNoOfflineFallback() {
        responder = request -> loginOk();
        access.enter("http://api", "maria", "senha-antiga".toCharArray());
        responder = request -> new OnlineSaleClient.ApiResponse(401, """
                {"success":false,"error":{"code":"UNAUTHENTICATED","message":"Credenciais invalidas."}}
                """);

        // A senha mudou no admin: a local ainda confere, mas o servidor respondeu e recusou.
        assertThrows(AccessDeniedException.class,
                () -> access.enter("http://api", "maria", "senha-antiga".toCharArray()));
    }

    @Test
    void anOnlineLoginRenewsTheLocalPasswordSoOfflineFollowsTheNewOne() {
        responder = request -> loginOk();
        access.enter("http://api", "maria", "senha-antiga".toCharArray());
        access.enter("http://api", "maria", "senha-nova-2".toCharArray());
        apiDown();

        assertThrows(AccessDeniedException.class,
                () -> access.enter("http://api", "maria", "senha-antiga".toCharArray()));
        assertEquals(Mode.OFFLINE, access.enter("http://api", "maria", "senha-nova-2".toCharArray()).mode());
    }

    @Test
    void emptyLoginOrPasswordIsRefusedWithoutCallingTheApi() {
        responder = request -> {
            throw new AssertionError("Nao deve chamar a API.");
        };

        assertThrows(AccessDeniedException.class, () -> access.enter("http://api", "", "x".toCharArray()));
        assertThrows(AccessDeniedException.class, () -> access.enter("http://api", "maria", new char[0]));
    }

    @Test
    void reconnectGetsATokenWhenTheApiComesBackAndStaysEmptyWhileItIsDown() {
        apiDown();
        assertEquals(Optional.empty(), access.reconnect("http://api", "maria", "senha-1".toCharArray()));

        responder = request -> loginOk();
        assertEquals(Optional.of("tok-123"), access.reconnect("http://api", "maria", "senha-1".toCharArray()));
    }
}
