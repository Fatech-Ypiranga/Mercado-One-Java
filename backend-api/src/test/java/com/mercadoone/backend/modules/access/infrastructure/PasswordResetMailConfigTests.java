package com.mercadoone.backend.modules.access.infrastructure;

import com.mercadoone.backend.modules.access.application.PasswordResetMailer;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetMailConfigTests {

    private final PasswordResetMailConfig config = new PasswordResetMailConfig();

    @Test
    void withoutHostOrSenderTheMailerIsOff() {
        assertFalse(config.passwordResetMailer("", 587, "", "", "no-reply@mercado.test", true).enabled());
        assertFalse(config.passwordResetMailer("smtp.mercado.test", 587, "", "", "", true).enabled());
    }

    @Test
    void withHostAndSenderTheMailerSendsARealSmtpMessage() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<String> conversation = CompletableFuture.supplyAsync(() -> serveOneMessage(server));
            PasswordResetMailer mailer = config.passwordResetMailer(
                    "localhost", server.getLocalPort(), "", "", "no-reply@mercado.test", false);
            assertTrue(mailer.enabled());

            mailer.send("ana@mercado.test", "Ana Souza", "http://app.teste/nova-senha?token=abc123", Duration.ofMinutes(60));

            String smtp = conversation.get(10, TimeUnit.SECONDS);
            assertTrue(smtp.contains("MAIL FROM:<no-reply@mercado.test>"), smtp);
            assertTrue(smtp.contains("RCPT TO:<ana@mercado.test>"), smtp);
            assertTrue(smtp.contains("Subject: Redefinir sua senha no mercadoOne"), smtp);
            assertTrue(smtp.contains("http://app.teste/nova-senha?token=abc123"), smtp);
            assertTrue(smtp.contains("60 minutos"), smtp);
        }
    }

    /** Servidor SMTP minimo: aceita uma mensagem e devolve tudo o que o cliente enviou. */
    private static String serveOneMessage(ServerSocket server) {
        StringBuilder received = new StringBuilder();
        try (Socket socket = server.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            out.print("220 teste ESMTP\r\n");
            out.flush();
            boolean data = false;
            String line;
            while ((line = in.readLine()) != null) {
                received.append(line).append('\n');
                if (data) {
                    if (line.equals(".")) {
                        data = false;
                        out.print("250 OK\r\n");
                        out.flush();
                    }
                    continue;
                }
                String command = line.toUpperCase();
                if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                    out.print("250 teste\r\n");
                } else if (command.startsWith("DATA")) {
                    data = true;
                    out.print("354 envie\r\n");
                } else if (command.startsWith("QUIT")) {
                    out.print("221 tchau\r\n");
                    out.flush();
                    break;
                } else {
                    out.print("250 OK\r\n");
                }
                out.flush();
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        return received.toString();
    }
}
