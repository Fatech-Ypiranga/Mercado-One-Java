package com.mercadoone.backend.infrastructure.config;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.api.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas de segurança no envelope padrão da API.
 *
 * <p>A resposta é escrita direto, sem {@code sendError}. Com {@code sendError} o contêiner repete a
 * requisição em {@code /error} como anônima e a negação de permissão (403) virava 401, o que o admin
 * web trata como sessão expirada e encerra a sessão.
 */
@Component
class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Sem autenticação válida: 401. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        write(response, HttpStatus.UNAUTHORIZED,
                ApiError.of("UNAUTHENTICATED", "Autenticacao necessaria."));
    }

    /** Autenticado, mas sem permissão para a operação: 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN,
                ApiError.of("FORBIDDEN", "Voce nao tem permissao para esta operacao."));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiError error) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiEnvelope.failed(error));
    }
}
