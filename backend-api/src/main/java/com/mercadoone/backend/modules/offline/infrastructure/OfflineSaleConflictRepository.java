package com.mercadoone.backend.modules.offline.infrastructure;

import com.mercadoone.backend.modules.offline.domain.OfflineConflictStatus;
import com.mercadoone.backend.modules.offline.domain.OfflineSaleConflictRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfflineSaleConflictRepository extends JpaRepository<OfflineSaleConflictRecord, Long> {

    List<OfflineSaleConflictRecord> findByStatusOrderByUpdatedAtDesc(OfflineConflictStatus status);

    List<OfflineSaleConflictRecord> findAllByOrderByUpdatedAtDesc();

    Optional<OfflineSaleConflictRecord> findFirstByLocalSaleIdAndStatusOrderByUpdatedAtDesc(
            String localSaleId,
            OfflineConflictStatus status
    );
}
