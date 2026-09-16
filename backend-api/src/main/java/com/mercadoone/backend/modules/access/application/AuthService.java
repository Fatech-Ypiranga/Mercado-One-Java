package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.infrastructure.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(String login, String password) {
        AppUser user = users.findByLoginIgnoreCase(login)
                .filter(AppUser::isActive)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas."));
        JwtService.TokenPayload token = jwtService.createToken(user);
        return new LoginResult(token.accessToken(), "Bearer", token.expiresAt(), user);
    }
}
