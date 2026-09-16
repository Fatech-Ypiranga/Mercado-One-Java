package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.UserManagementService;
import com.mercadoone.backend.modules.access.application.UserManagementService.UserData;
import com.mercadoone.backend.modules.access.domain.AppUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.access.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/access")
public class UserController {

    private final UserManagementService userManagementService;

    UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/roles")
    public ApiEnvelope<List<RoleResponse>> roles() {
        return ApiEnvelope.ok(Arrays.stream(UserRole.values())
                .map(RoleResponse::from)
                .toList());
    }

    @GetMapping("/users")
    public ApiEnvelope<List<UserResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(userManagementService.listUsers(search, role, active).stream()
                .map(UserResponse::from)
                .toList());
    }

    @PostMapping("/users")
    public ApiEnvelope<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiEnvelope.ok(UserResponse.from(userManagementService.createUser(request.toData())));
    }

    @PutMapping("/users/{id}")
    public ApiEnvelope<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiEnvelope.ok(UserResponse.from(userManagementService.updateUser(id, request.toData())));
    }

    public record RoleResponse(UserRole value, String label) {
        static RoleResponse from(UserRole role) {
            return new RoleResponse(role, switch (role) {
                case ADMIN -> "Administrador";
                case GERENTE -> "Gerente";
                case OPERADOR_CAIXA -> "Operador de caixa";
                case ESTOQUISTA -> "Estoquista";
            });
        }
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 120) String login,
            @NotBlank @Size(min = 6, max = 120) String password,
            @NotNull UserRole perfil,
            boolean active
    ) {
        UserData toData() {
            return new UserData(nome, login, password, perfil, active);
        }
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 120) String login,
            @Size(min = 6, max = 120) String password,
            @NotNull UserRole perfil,
            boolean active
    ) {
        UserData toData() {
            return new UserData(nome, login, password, perfil, active);
        }
    }

    public record UserResponse(
            Long id,
            String nome,
            String login,
            UserRole perfil,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        static UserResponse from(AppUser user) {
            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getLogin(),
                    user.getRole(),
                    user.isActive() ? UserStatus.ACTIVE : UserStatus.INACTIVE,
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
