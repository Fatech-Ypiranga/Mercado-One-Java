package com.mercadoone.backend.modules.audit.infrastructure;

import com.mercadoone.backend.modules.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    @Query("select distinct e.action from AuditEvent e order by e.action")
    List<String> findDistinctActions();
}
