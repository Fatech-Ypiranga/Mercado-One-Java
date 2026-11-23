package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.PasswordResetToken;
import com.mercadoone.backend.modules.access.infrastructure.PasswordResetTokenRepository;
import com.mercadoone.backend.modules.audit.application.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Redefinicao de senha por e-mail.
 *
 * <p>Regras: o pedido responde sempre igual, exista ou nao a conta (nao revela quem e usuario); o link vale por
 * tempo limitado e uma unica vez; um pedido novo invalida os anteriores; no maximo {@value #MAX_REQUESTS_PER_HOUR}
 * pedidos por hora por usuario; o token so existe no link (o banco guarda o hash). O envio do e-mail roda depois
 * do commit e em outra thread, para que o tempo de resposta nao diferencie conta existente de inexistente.
 */
@Service
public class PasswordResetService {

    static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserManagementService users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetMailer mailer;
    private final AuditService auditService;
    private final Clock clock;
    private final String appUrl;
    private final Duration validity;
    private final SecureRandom random = new SecureRandom();

    PasswordResetService(
            UserManagementService users,
            PasswordResetTokenRepository tokens,
            PasswordResetMailer mailer,
            AuditService auditService,
            @Value("${mercado-one.password-reset.app-url:http://localhost:4200}") String appUrl,
            @Value("${mercado-one.password-reset.token-minutes:60}") long tokenMinutes
    ) {
        this.users = users;
        this.tokens = tokens;
        this.mailer = mailer;
        this.auditService = auditService;
        this.clock = Clock.systemUTC();
        this.appUrl = appUrl.endsWith("/") ? appUrl.substring(0, appUrl.length() - 1) : appUrl;
        this.validity = Duration.ofMinutes(tokenMinutes);
    }

    public boolean enabled() {
        return mailer.enabled();
    }

    /** Pede o link. Nao devolve nada nem falha para conta inexistente, sem e-mail, inativa ou acima do limite. */
    @Transactional
    public void request(String identifier) {
        if (!mailer.enabled()) {
            throw new ResetUnavailableException();
        }
        AppUser user = users.findActiveByLoginOrEmail(identifier).orElse(null);
        if (user == null || user.getEmail() == null) {
            return;
        }
        Instant now = Instant.now(clock);
        if (tokens.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1))) >= MAX_REQUESTS_PER_HOUR) {
            return;
        }
        tokens.closeOpenTokens(user.getId(), now);
        String raw = newToken();
        tokens.save(new PasswordResetToken(user.getId(), hash(raw), now.plus(validity), now));
        auditService.record("PASSWORD_RESET_REQUESTED", "app_user", user.getId(), user.getId(), Map.of("login", user.getLogin()));

        String email = user.getEmail();
        String name = user.getName();
        String link = appUrl + "/nova-senha?token=" + raw;
        afterCommit(() -> CompletableFuture.runAsync(() -> {
            try {
                mailer.send(email, name, link, validity);
            } catch (RuntimeException exception) {
                // Nao vaza para o chamador: a resposta ao pedido e sempre a mesma.
                LOG.warn("Falha ao enviar o e-mail de redefinicao de senha: {}", exception.getMessage());
            }
        }));
    }

    /** O link ainda serve? Usado pela tela para mostrar "link expirado" antes de pedir a nova senha. */
    @Transactional(readOnly = true)
    public boolean linkIsValid(String rawToken) {
        return rawToken != null && tokens.findByTokenHash(hash(rawToken.trim()))
                .filter(token -> token.usableAt(Instant.now(clock)))
                .isPresent();
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        PasswordResetToken token = rawToken == null ? null : tokens.findByTokenHash(hash(rawToken.trim())).orElse(null);
        if (token == null || !token.usableAt(now)) {
            throw new ResetLinkInvalidException();
        }
        // Valida a senha antes de gastar o link: senha curta nao queima o token.
        users.resetPasswordByOwner(token.getUserId(), newPassword);
        token.markUsed(now);
        tokens.closeOpenTokens(token.getUserId(), now);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel neste Java.", exception);
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
