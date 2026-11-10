package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.UserRole;

/** Usuario autenticado na requisicao atual (principal do Spring Security). */
public record AuthenticatedUser(Long id, String login, String name, UserRole role) {
}
