package com.mercadoone.backend.modules.audit.infrastructure;

import com.mercadoone.backend.modules.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
