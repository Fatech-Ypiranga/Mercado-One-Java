package com.mercadoone.pdv.offline;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Credencial local para o operador abrir o caixa quando a API está fora do ar.
 *
 * <p>Guarda só um hash com sal (PBKDF2-HMAC-SHA256) da senha de quem já entrou online neste computador.
 * Nunca guarda a senha. A credencial é gravada ou renovada a cada login online bem-sucedido, então uma
 * senha trocada no admin passa a valer aqui no próximo login online. Quem nunca entrou online neste
 * caixa não entra offline.
 *
 * <p>A credencial vale por {@link #DEFAULT_VALIDITY} desde o último login online. Passado esse prazo ela
 * deixa de abrir o caixa offline: é o que limita o tempo em que um operador desativado ou demitido no admin
 * ainda consegue entrar neste computador sem internet.
 */
public class OperatorCredentialStore {

    /** Custo do PBKDF2 em produção. Os testes usam um valor menor. */
    public static final int DEFAULT_ITERATIONS = 210_000;
    /** Prazo da credencial local, contado do último login online. */
    public static final java.time.Duration DEFAULT_VALIDITY = java.time.Duration.ofDays(30);
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final OfflineStorageConfig config;
    private final int iterations;
    private final java.time.Duration validity;
    private final java.time.Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OperatorCredentialStore(OfflineStorageConfig config) {
        this(config, DEFAULT_ITERATIONS);
    }

    public OperatorCredentialStore(OfflineStorageConfig config, int iterations) {
        this(config, iterations, DEFAULT_VALIDITY, java.time.Clock.systemUTC());
    }

    public OperatorCredentialStore(OfflineStorageConfig config, int iterations, java.time.Duration validity, java.time.Clock clock) {
        this.config = config;
        this.iterations = iterations;
        this.validity = validity;
        this.clock = clock;
    }

    public void initialize() {
        try {
            Files.createDirectories(config.databasePath().getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel preparar armazenamento offline.", exception);
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists operator_credentials (
                        login text primary key,
                        display_name text not null,
                        salt text not null,
                        hash text not null,
                        iterations integer not null,
                        updated_at text not null
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel inicializar credenciais locais.", exception);
        }
    }

    /** Grava ou renova a credencial depois de um login online bem-sucedido. */
    public void remember(String login, String displayName, char[] password) {
        String key = normalize(login);
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations);
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into operator_credentials(login, display_name, salt, hash, iterations, updated_at)
                     values (?, ?, ?, ?, ?, ?)
                     on conflict(login) do update set display_name = excluded.display_name, salt = excluded.salt,
                         hash = excluded.hash, iterations = excluded.iterations, updated_at = excluded.updated_at
                     """)) {
            statement.setString(1, key);
            statement.setString(2, displayName == null || displayName.isBlank() ? login.trim() : displayName.trim());
            statement.setString(3, Base64.getEncoder().encodeToString(salt));
            statement.setString(4, Base64.getEncoder().encodeToString(hash));
            statement.setInt(5, iterations);
            statement.setString(6, Instant.now(clock).toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel guardar a credencial local.", exception);
        }
    }

    /** Nome do operador se login e senha conferem com a credencial local; vazio caso contrário. */
    public Optional<String> verify(String login, char[] password) {
        if (login == null || login.isBlank() || password == null || password.length == 0) {
            return Optional.empty();
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "select display_name, salt, hash, iterations, updated_at from operator_credentials where login = ?")) {
            statement.setString(1, normalize(login));
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || expired(rows.getString("updated_at"))) {
                    return Optional.empty();
                }
                byte[] expected = Base64.getDecoder().decode(rows.getString("hash"));
                byte[] actual = derive(password, Base64.getDecoder().decode(rows.getString("salt")), rows.getInt("iterations"));
                return MessageDigest.isEqual(expected, actual) ? Optional.of(rows.getString("display_name")) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler a credencial local.", exception);
        }
    }

    /** O operador já entrou online neste computador? Só ele pode entrar offline. */
    public boolean knows(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("select 1 from operator_credentials where login = ?")) {
            statement.setString(1, normalize(login));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler a credencial local.", exception);
        }
    }

    /** O operador entrou online neste computador, mas há mais tempo que a validade da credencial? */
    public boolean isExpired(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("select updated_at from operator_credentials where login = ?")) {
            statement.setString(1, normalize(login));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && expired(rows.getString("updated_at"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler a credencial local.", exception);
        }
    }

    private boolean expired(String updatedAt) {
        return Instant.parse(updatedAt).plus(validity).isBefore(Instant.now(clock));
    }

    private static String normalize(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2 indisponivel neste Java.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl());
    }
}
