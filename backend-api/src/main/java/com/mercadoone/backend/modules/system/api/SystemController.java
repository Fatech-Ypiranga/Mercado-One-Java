package com.mercadoone.backend.modules.system.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.domain.UserRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public ApiEnvelope<SystemInfoResponse> info() {
        return ApiEnvelope.ok(new SystemInfoResponse(
                "Mercado One Backend API",
                "0.1.0-SNAPSHOT",
                Arrays.stream(UserRole.values()).map(Enum::name).toList()
        ));
    }

    public record SystemInfoResponse(String name, String version, List<String> roles) {
    }
}
