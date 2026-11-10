package com.mercadoone.backend.infrastructure.config;

import com.mercadoone.backend.modules.access.application.JwtService;
import com.mercadoone.backend.modules.access.application.AuthenticatedUser;
import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.infrastructure.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String SESSION_COOKIE = "mercado_one_admin_session";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final JwtService jwtService;
    private final AppUserRepository users;
    private final List<String> trustedOrigins;
    private final boolean cookieSecure;

    JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserRepository users,
            @Value("${mercado-one.security.cors-allowed-origins}") String allowedOrigins,
            @Value("${mercado-one.security.cookie-secure:false}") boolean cookieSecure
    ) {
        this.jwtService = jwtService;
        this.users = users;
        this.trustedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        this.cookieSecure = cookieSecure;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String bearer = bearerToken(request);
        String token = bearer != null ? bearer : cookieToken(request);
        if (bearer == null && token != null && !SAFE_METHODS.contains(request.getMethod()) && !isTrustedOrigin(request)) {
            // Protecao CSRF: requisicao mutavel autenticada por cookie precisa vir de origem confiavel.
            response.sendError(HttpStatus.FORBIDDEN.value(), "Origem nao autorizada.");
            return;
        }
        if (token != null) {
            jwtService.validate(token)
                    .flatMap(user -> users.findById(user.id()))
                    .filter(AppUser::isActive)
                    .filter(user -> !jwtService.issuedBefore(token, user.getUpdatedAt()))
                    .ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            origin = originOf(request.getHeader(HttpHeaders.REFERER));
        }
        if (origin == null) {
            // Sem Origin/Referer: navegadores modernos sempre enviam Origin em POST/PUT cross-site.
            // Em modo local (SameSite=Lax) o cookie nao acompanha requisicoes cross-site.
            return !cookieSecure;
        }
        String host = request.getHeader(HttpHeaders.HOST);
        return trustedOrigins.contains(origin)
                || (host != null && origin.equals("https://" + host))
                || (!cookieSecure && host != null && origin.equals("http://" + host));
    }

    private static String originOf(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private static String cookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void authenticate(AppUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getLogin(), user.getName(), user.getRole()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
