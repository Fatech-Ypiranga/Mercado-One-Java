package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.AuthService;
import com.mercadoone.backend.modules.access.application.LoginResult;
import com.mercadoone.backend.modules.access.application.JwtService.AuthenticatedUser;
import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.access.domain.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;
    private final List<String> adminOrigins;

    AuthController(
            AuthService authService,
            @Value("${mercado-one.security.cookie-secure:false}") boolean cookieSecure,
            @Value("${mercado-one.security.cors-allowed-origins}") String allowedOrigins
    ) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
        this.adminOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @PostMapping("/login")
    public ApiEnvelope<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request.login(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.accessToken(), result.expiresAt()).toString());
        return ApiEnvelope.ok(LoginResponse.from(result));
    }

    @PostMapping("/logout")
    public ApiEnvelope<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        if (cookieSecure && !isTrustedLogoutOrigin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem nao autorizada para logout.");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", Instant.now()).toString());
        return ApiEnvelope.ok(null);
    }

    private boolean isTrustedLogoutOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        return origin != null && (adminOrigins.contains(origin)
                || origin.equals("https://" + request.getHeader(HttpHeaders.HOST)));
    }

    @GetMapping("/me")
    public ApiEnvelope<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiEnvelope.ok(new MeResponse(
                user.id(),
                user.name(),
                user.login(),
                user.role(),
                UserStatus.ACTIVE
        ));
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UserSummary user
    ) {
        static LoginResponse from(LoginResult result) {
            return new LoginResponse(
                    result.accessToken(),
                    result.tokenType(),
                    result.expiresAt(),
                    UserSummary.from(result.user())
            );
        }
    }

    public record UserSummary(Long id, String nome, String login, UserRole perfil) {
        static UserSummary from(AppUser user) {
            return new UserSummary(user.getId(), user.getName(), user.getLogin(), user.getRole());
        }
    }

    public record MeResponse(Long id, String nome, String login, UserRole perfil, UserStatus status) {
    }

    private ResponseCookie sessionCookie(String token, Instant expiresAt) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("mercado_one_admin_session", token)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge);
        if (cookieSecure) {
            return builder.secure(true).sameSite("None").build();
        }
        return builder.sameSite("Lax").build();
    }
}
