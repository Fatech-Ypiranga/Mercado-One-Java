package com.mercadoone.backend.modules.access.infrastructure;

import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(SeedAdminProperties.class)
public class SeedAdminConfig {

    @Bean
    CommandLineRunner seedAdmin(
            SeedAdminProperties properties,
            AppUserRepository users,
            PasswordEncoder passwordEncoder
    ) {
        return new SeedAdminRunner(properties, users, passwordEncoder);
    }

    private static class SeedAdminRunner implements CommandLineRunner {
        private final SeedAdminProperties properties;
        private final AppUserRepository users;
        private final PasswordEncoder passwordEncoder;

        private SeedAdminRunner(
                SeedAdminProperties properties,
                AppUserRepository users,
                PasswordEncoder passwordEncoder
        ) {
            this.properties = properties;
            this.users = users;
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public void run(String... args) {
            if (!properties.enabled()) {
                return;
            }
            if (properties.login().isBlank() || properties.password().isBlank()) {
                throw new IllegalStateException("Seed admin login and password must be configured when seed is enabled.");
            }
            String passwordHash = passwordEncoder.encode(properties.password());
            users.findByLoginIgnoreCase(properties.login())
                    .ifPresentOrElse(
                            user -> {
                                user.updateSeedData(properties.name(), passwordHash);
                                users.save(user);
                            },
                            () -> users.save(new AppUser(
                                    properties.name(),
                                    properties.login(),
                                    passwordHash,
                                    UserRole.ADMIN,
                                    true
                            ))
                    );
        }
    }
}
