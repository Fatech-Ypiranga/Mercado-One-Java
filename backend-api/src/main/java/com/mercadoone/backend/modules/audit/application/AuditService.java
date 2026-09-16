package com.mercadoone.backend.modules.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadoone.backend.modules.audit.domain.AuditEvent;
import com.mercadoone.backend.modules.audit.infrastructure.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository events;
    private final ObjectMapper objectMapper = new ObjectMapper();

    AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Object entityId, Long actorUserId, Map<String, ?> summary) {
        events.save(new AuditEvent(
                action,
                entityType,
                entityId == null ? null : entityId.toString(),
                actorUserId,
                toJson(summary)
        ));
    }

    private String toJson(Map<String, ?> summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel registrar auditoria.", exception);
        }
    }
}
