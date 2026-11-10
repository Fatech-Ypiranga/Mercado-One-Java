package com.mercadoone.backend.modules.audit.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.audit.application.AuditQueryService;
import com.mercadoone.backend.modules.audit.application.AuditQueryService.AuditFilters;
import com.mercadoone.backend.modules.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;

    AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/events")
    public ApiEnvelope<AuditPageResponse> events(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Page<AuditEvent> result = auditQueryService.search(
                new AuditFilters(action, entityType, entityId, actorUserId, from, to, page, size)
        );
        return ApiEnvelope.ok(new AuditPageResponse(
                result.getContent().stream().map(AuditEventResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }

    @GetMapping("/actions")
    public ApiEnvelope<List<String>> actions() {
        return ApiEnvelope.ok(auditQueryService.actions());
    }

    public record AuditPageResponse(
            List<AuditEventResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record AuditEventResponse(
            Long id,
            String action,
            String entityType,
            String entityId,
            Long actorUserId,
            String summaryJson,
            Instant createdAt
    ) {
        static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(
                    event.getId(),
                    event.getAction(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getActorUserId(),
                    event.getSummaryJson(),
                    event.getCreatedAt()
            );
        }
    }
}
