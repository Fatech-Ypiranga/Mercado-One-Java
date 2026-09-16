package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.AuthService;
import com.mercadoone.backend.modules.access.application.LoginResult;
import com.mercadoone.backend.modules.access.application.JwtService.AuthenticatedUser;
import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.access.domain.UserStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiEnvelope<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request.login(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.accessToken(), result.expiresAt()).toString());
        return ApiEnvelope.ok(LoginResponse.from(result));
    }

    @PostMapping("/logout")
    public ApiEnvelope<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("mercado_one_admin_session", "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString());
        return ApiEnvelope.ok(null);
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
        return ResponseCookie.from("mercado_one_admin_session", token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
