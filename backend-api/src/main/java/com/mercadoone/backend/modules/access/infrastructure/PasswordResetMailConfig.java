package com.mercadoone.backend.modules.access.infrastructure;

import com.mercadoone.backend.modules.access.application.PasswordResetMailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;
import java.util.Properties;

/**
 * E-mail de redefinicao de senha, configurado so por variaveis de ambiente (MERCADO_ONE_MAIL_*).
 * Sem MERCADO_ONE_MAIL_HOST ou sem MERCADO_ONE_MAIL_FROM o envio fica desligado e o sistema nao promete e-mail.
 */
@Configuration
class PasswordResetMailConfig {

    @Bean
    PasswordResetMailer passwordResetMailer(
            @Value("${mercado-one.mail.host:}") String host,
            @Value("${mercado-one.mail.port:587}") int port,
            @Value("${mercado-one.mail.username:}") String username,
            @Value("${mercado-one.mail.password:}") String password,
            @Value("${mercado-one.mail.from:}") String from,
            @Value("${mercado-one.mail.starttls:true}") boolean starttls
    ) {
        if (host.isBlank() || from.isBlank()) {
            return new DisabledPasswordResetMailer();
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        if (!username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
        }
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        properties.put("mail.smtp.connectiontimeout", "8000");
        properties.put("mail.smtp.timeout", "8000");
        properties.put("mail.smtp.writetimeout", "8000");
        return new SmtpPasswordResetMailer(sender, from.trim());
    }

    static final class DisabledPasswordResetMailer implements PasswordResetMailer {
        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void send(String toEmail, String name, String link, Duration validity) {
            throw new IllegalStateException("Envio de e-mail desligado.");
        }
    }

    static final class SmtpPasswordResetMailer implements PasswordResetMailer {
        private final JavaMailSender sender;
        private final String from;

        SmtpPasswordResetMailer(JavaMailSender sender, String from) {
            this.sender = sender;
            this.from = from;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public void send(String toEmail, String name, String link, Duration validity) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Redefinir sua senha no mercadoOne");
            message.setText("""
                    Ola, %s.

                    Recebemos um pedido para redefinir a senha da sua conta no mercadoOne.
                    Para criar uma nova senha, abra o link abaixo. Ele vale por %d minutos e so pode ser usado uma vez:

                    %s

                    Se voce nao pediu isso, ignore este e-mail. Sua senha continua a mesma.
                    """.formatted(name, validity.toMinutes(), link));
            sender.send(message);
        }
    }
}
