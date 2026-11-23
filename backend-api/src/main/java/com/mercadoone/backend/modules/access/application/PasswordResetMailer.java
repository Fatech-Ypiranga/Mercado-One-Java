package com.mercadoone.backend.modules.access.application;

import java.time.Duration;

/** Envio do link de redefinicao de senha. Sem SMTP configurado, {@link #enabled()} e falso e nada e enviado. */
public interface PasswordResetMailer {

    boolean enabled();

    void send(String toEmail, String name, String link, Duration validity);
}
