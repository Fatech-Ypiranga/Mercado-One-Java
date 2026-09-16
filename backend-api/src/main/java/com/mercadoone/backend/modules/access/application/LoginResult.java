package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.AppUser;

import java.time.Instant;

public record LoginResult(String accessToken, String tokenType, Instant expiresAt, AppUser user) {
}
