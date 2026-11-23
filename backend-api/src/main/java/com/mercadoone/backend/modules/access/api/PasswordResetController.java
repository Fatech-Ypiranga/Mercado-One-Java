package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Redefinicao de senha por e-mail. Rotas publicas: quem esqueceu a senha nao tem sessao. */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordReset;

    PasswordResetController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    /** A tela usa isto para decidir entre o formulario de e-mail e a orientacao de falar com o administrador. */
    @GetMapping("/status")
    public ApiEnvelope<StatusResponse> status() {
        return ApiEnvelope.ok(new StatusResponse(passwordReset.enabled()));
    }

    /** Responde sempre igual, exista ou nao a conta. */
    @PostMapping("/request")
    public ApiEnvelope<Void> request(@Valid @RequestBody RequestBodyDto request) {
        passwordReset.request(request.identifier());
        return ApiEnvelope.ok(null);
    }

    @PostMapping("/check")
    public ApiEnvelope<CheckResponse> check(@Valid @RequestBody CheckRequest request) {
        return ApiEnvelope.ok(new CheckResponse(passwordReset.linkIsValid(request.token())));
    }

    @PostMapping("/confirm")
    public ApiEnvelope<Void> confirm(@Valid @RequestBody ConfirmRequest request) {
        passwordReset.confirm(request.token(), request.password());
        return ApiEnvelope.ok(null);
    }

    public record StatusResponse(boolean enabled) {
    }

    public record RequestBodyDto(@NotBlank @Size(max = 180) String identifier) {
    }

    public record CheckRequest(@NotBlank @Size(max = 200) String token) {
    }

    public record CheckResponse(boolean valid) {
    }

    public record ConfirmRequest(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 6, max = 120) String password
    ) {
    }
}
