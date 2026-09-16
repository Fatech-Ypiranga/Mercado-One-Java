package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${mercado-one.security.jwt-secret}") String secret,
            @Value("${mercado-one.security.jwt-expiration-minutes}") long expirationMinutes
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * 60;
    }

    public TokenPayload createToken(AppUser user) {
        Instant expiresAt = Instant.now().plusSeconds(expirationSeconds);
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("""
                {"sub":"%s","login":"%s","name":"%s","role":"%s","exp":%d}
                """.formatted(
                user.getId(),
                escapeJson(user.getLogin()),
                escapeJson(user.getName()),
                user.getRole().name(),
                expiresAt.getEpochSecond()
        ).trim());
        String unsignedToken = header + "." + payload;
        return new TokenPayload(unsignedToken + "." + sign(unsignedToken), expiresAt);
    }

    public Optional<AuthenticatedUser> validate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            return Optional.empty();
        }

        try {
            String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            long expiresAt = Long.parseLong(readString(payload, "exp"));
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(
                    Long.parseLong(readString(payload, "sub")),
                    readString(payload, "login"),
                    readString(payload, "name"),
                    UserRole.valueOf(readString(payload, "role"))
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT.", exception);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < expectedBytes.length; index++) {
            result |= expectedBytes[index] ^ actualBytes[index];
        }
        return result == 0;
    }

    private static String readString(String json, String field) {
        String quotedField = "\"" + field + "\":";
        int start = json.indexOf(quotedField);
        if (start < 0) {
            throw new IllegalArgumentException("Missing JWT field.");
        }
        start += quotedField.length();
        if (json.charAt(start) != '"') {
            int end = json.indexOf(',', start);
            if (end < 0) {
                end = json.indexOf('}', start);
            }
            return json.substring(start, end).trim();
        }
        start += 1;
        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int index = start; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaping) {
                value.append(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        throw new IllegalArgumentException("Invalid JWT field.");
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record TokenPayload(String accessToken, Instant expiresAt) {
    }

    public record AuthenticatedUser(Long id, String login, String name, UserRole role) {
    }
}
