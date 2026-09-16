package com.mercadoone.backend.modules.access.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercado-one.security.seed-admin")
public record SeedAdminProperties(boolean enabled, String name, String login, String password) {
}
