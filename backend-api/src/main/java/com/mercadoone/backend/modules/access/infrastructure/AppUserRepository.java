package com.mercadoone.backend.modules.access.infrastructure;

import com.mercadoone.backend.modules.access.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findByLoginIgnoreCase(String login);
}
