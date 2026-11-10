package com.mercadoone.backend.modules.audit.application;

import com.mercadoone.backend.modules.audit.domain.AuditEvent;
import com.mercadoone.backend.modules.audit.infrastructure.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditQueryService {

    private final AuditEventRepository events;

    AuditQueryService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(AuditFilters filters) {
        int page = filters.page() == null ? 0 : Math.max(filters.page(), 0);
        int size = filters.size() == null ? 50 : Math.clamp(filters.size(), 1, 200);
        return events.findAll(matches(filters), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public List<String> actions() {
        return events.findDistinctActions();
    }

    private static Specification<AuditEvent> matches(AuditFilters filters) {
        Specification<AuditEvent> spec = Specification.unrestricted();
        if (!isBlank(filters.action())) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("action"), filters.action().trim()));
        }
        if (!isBlank(filters.entityType())) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("entityType"), filters.entityType().trim()));
        }
        if (!isBlank(filters.entityId())) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("entityId"), filters.entityId().trim()));
        }
        if (filters.actorUserId() != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("actorUserId"), filters.actorUserId()));
        }
        if (filters.from() != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), filters.from()));
        }
        if (filters.to() != null) {
            spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), filters.to()));
        }
        return spec;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AuditFilters(
            String action,
            String entityType,
            String entityId,
            Long actorUserId,
            Instant from,
            Instant to,
            Integer page,
            Integer size
    ) {
    }
}
