package com.mercadoone.backend.modules.offline.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.offline.domain.OfflineConflictStatus;
import com.mercadoone.backend.modules.offline.domain.OfflineSaleConflictRecord;
import com.mercadoone.backend.modules.offline.infrastructure.OfflineSaleConflictRepository;
import com.mercadoone.backend.modules.sales.application.SalesService;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleConflict;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleData;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleItemData;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleSyncResult;
import com.mercadoone.backend.modules.sales.application.SalesService.SalePaymentData;
import com.mercadoone.backend.modules.sales.domain.Sale;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OfflineSalesService {

    private final SalesService salesService;
    private final OfflineSaleConflictRepository conflicts;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    OfflineSalesService(
            SalesService salesService,
            OfflineSaleConflictRepository conflicts,
            AuditService auditService
    ) {
        this.salesService = salesService;
        this.conflicts = conflicts;
        this.auditService = auditService;
    }

    @Transactional
    public OfflineSaleSyncResult syncOfflineSale(OfflineSaleData data, Long operatorUserId) {
        List<OfflineSaleConflict> saleConflicts = salesService.validateOfflineSale(data);
        if (!saleConflicts.isEmpty()) {
            saveConflict(data, operatorUserId, saleConflicts);
            return new OfflineSaleSyncResult("CONFLICT", null, saleConflicts);
        }

        try {
            Sale sale = salesService.finalizeOfflineSale(data, operatorUserId);
            return new OfflineSaleSyncResult("SENT", sale, List.of());
        } catch (IllegalArgumentException exception) {
            List<OfflineSaleConflict> generatedConflicts = List.of(new OfflineSaleConflict(
                    "STOCK_OR_PAYMENT",
                    exception.getMessage()
            ));
            saveConflict(data, operatorUserId, generatedConflicts);
            return new OfflineSaleSyncResult("CONFLICT", null, generatedConflicts);
        }
    }

    @Transactional(readOnly = true)
    public List<OfflineSaleConflictRecord> listConflicts(OfflineConflictStatus status) {
        if (status == null) {
            return conflicts.findAllByOrderByUpdatedAtDesc();
        }
        return conflicts.findByStatusOrderByUpdatedAtDesc(status);
    }

    @Transactional
    public OfflineSaleConflictRecord resolveConflict(
            Long conflictId,
            ConflictResolutionAction action,
            String note,
            Long resolvedByUserId
    ) {
        OfflineSaleConflictRecord conflict = conflicts.findById(conflictId)
                .orElseThrow(() -> new EntityNotFoundException("Conflito offline nao encontrado."));
        if (conflict.getStatus() != OfflineConflictStatus.PENDING) {
            throw new IllegalArgumentException("Conflito offline ja foi resolvido.");
        }
        if (action == ConflictResolutionAction.ACCEPT) {
            OfflineSaleData data = readPayload(conflict.getSalePayload());
            Sale sale = salesService.acceptConflictedOfflineSale(data, resolvedByUserId);
            conflict.accept(sale.getId(), resolvedByUserId, clean(note));
            auditService.record("OFFLINE_CONFLICT_ACCEPTED", "offline_sale_conflict", conflict.getId(), resolvedByUserId, Map.of(
                    "localSaleId", conflict.getLocalSaleId(),
                    "remoteSaleId", sale.getId()
            ));
        } else {
            String rejectionNote = clean(note);
            if (rejectionNote == null) {
                throw new IllegalArgumentException("Informe uma observacao para rejeitar o conflito offline.");
            }
            conflict.reject(resolvedByUserId, rejectionNote);
            auditService.record("OFFLINE_CONFLICT_REJECTED", "offline_sale_conflict", conflict.getId(), resolvedByUserId, Map.of(
                    "localSaleId", conflict.getLocalSaleId()
            ));
        }
        return conflict;
    }

    private void saveConflict(OfflineSaleData data, Long operatorUserId, List<OfflineSaleConflict> saleConflicts) {
        String summary = conflictSummary(saleConflicts);
        String payload = writePayload(data);
        OfflineSaleConflictRecord conflict = conflicts
                .findFirstByLocalSaleIdAndStatusOrderByUpdatedAtDesc(data.localSaleId(), OfflineConflictStatus.PENDING)
                .orElseGet(() -> new OfflineSaleConflictRecord(
                        data.localSaleId(),
                        data.createdAt(),
                        data.customerId(),
                        operatorUserId,
                        summary,
                        payload
                ));
        conflict.refresh(summary, payload);
        conflicts.save(conflict);
        auditService.record("OFFLINE_SALE_CONFLICT", "offline_sale", data.localSaleId(), operatorUserId, Map.of(
                "conflictCount", saleConflicts.size()
        ));
    }

    private String conflictSummary(List<OfflineSaleConflict> saleConflicts) {
        return saleConflicts.stream()
                .map(conflict -> conflict.code() + ": " + conflict.message())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Conflito offline.");
    }

    private String writePayload(OfflineSaleData data) {
        try {
            return objectMapper.writeValueAsString(OfflineSalePayload.from(data));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel registrar venda offline em conflito.", exception);
        }
    }

    private OfflineSaleData readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, OfflineSalePayload.class).toData();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel ler venda offline em conflito.", exception);
        }
    }

    private String clean(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }

    public enum ConflictResolutionAction {
        ACCEPT,
        REJECT
    }

    private record OfflineSalePayload(
            String localSaleId,
            String createdAt,
            Long customerId,
            List<OfflineSaleItemData> items,
            List<SalePaymentData> payments
    ) {
        static OfflineSalePayload from(OfflineSaleData data) {
            return new OfflineSalePayload(
                    data.localSaleId(),
                    data.createdAt().toString(),
                    data.customerId(),
                    data.items(),
                    data.payments()
            );
        }

        OfflineSaleData toData() {
            return new OfflineSaleData(
                    localSaleId,
                    Instant.parse(createdAt),
                    customerId,
                    items,
                    payments
            );
        }
    }
}
