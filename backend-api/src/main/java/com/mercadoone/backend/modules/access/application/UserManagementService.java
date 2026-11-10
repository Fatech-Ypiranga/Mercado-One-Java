package com.mercadoone.backend.modules.access.application;

import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.access.infrastructure.AppUserRepository;
import com.mercadoone.backend.modules.audit.application.AuditService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    UserManagementService(AppUserRepository users, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listUsers(String search, UserRole role, Boolean active) {
        return users.findAll(userFilters(search, role, active), Sort.by("name").ascending());
    }

    @Transactional
    public AppUser createUser(UserData data, Long actorUserId) {
        requireUniqueLogin(data.login(), null);
        AppUser user = users.save(new AppUser(
                data.name().trim(),
                data.login().trim(),
                passwordEncoder.encode(data.password()),
                data.role(),
                data.active()
        ));
        auditService.record("USER_CREATED", "app_user", user.getId(), actorUserId, java.util.Map.of(
                "login", user.getLogin(),
                "role", user.getRole()
        ));
        return user;
    }

    @Transactional
    public AppUser updateUser(Long id, UserData data, Long actorUserId) {
        AppUser user = findUser(id);
        requireUniqueLogin(data.login(), id);
        if (id.equals(actorUserId) && (!data.active() || data.role() != user.getRole())) {
            throw new IllegalArgumentException("Voce nao pode inativar nem trocar o proprio perfil.");
        }
        user.updateProfile(data.name().trim(), data.login().trim(), data.role(), data.active());
        boolean passwordChanged = !isBlank(data.password());
        if (passwordChanged) {
            user.updatePassword(passwordEncoder.encode(data.password()));
        }
        auditService.record("USER_UPDATED", "app_user", user.getId(), actorUserId, java.util.Map.of(
                "login", user.getLogin(),
                "role", user.getRole(),
                "active", user.isActive(),
                "passwordChanged", passwordChanged
        ));
        return user;
    }

    @Transactional
    public AppUser resetPassword(Long id, String newPassword, Long actorUserId) {
        requireValidPassword(newPassword);
        AppUser user = findUser(id);
        user.updatePassword(passwordEncoder.encode(newPassword));
        auditService.record("USER_PASSWORD_RESET", "app_user", user.getId(), actorUserId, java.util.Map.of(
                "login", user.getLogin()
        ));
        return user;
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Senha atual nao confere.");
        }
        requireValidPassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
        auditService.record("USER_PASSWORD_CHANGED", "app_user", user.getId(), userId, java.util.Map.of(
                "login", user.getLogin()
        ));
    }

    private static void requireValidPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 120) {
            throw new IllegalArgumentException("Senha deve ter entre 6 e 120 caracteres.");
        }
    }

    private AppUser findUser(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    private void requireUniqueLogin(String login, Long currentUserId) {
        users.findByLoginIgnoreCase(login.trim())
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Login ja esta em uso.");
                });
    }

    private static Specification<AppUser> userFilters(String search, UserRole role, Boolean active) {
        Specification<AppUser> filters = Specification.unrestricted();
        if (role != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("role"), role));
        }
        if (active != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (!isBlank(search)) {
            filters = filters.and((root, query, builder) -> {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                return builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("login")), pattern)
                );
            });
        }
        return filters;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record UserData(String name, String login, String password, UserRole role, boolean active) {
    }
}
