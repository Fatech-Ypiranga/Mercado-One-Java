package com.mercadoone.backend.modules.offline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "offline_sale_conflicts")
public class OfflineSaleConflictRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_sale_id", nullable = false, length = 80)
    private String localSaleId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OfflineConflictStatus status;

    @Column(name = "conflict_summary", nullable = false, columnDefinition = "text")
    private String conflictSummary;

    @Column(name = "sale_payload", nullable = false, columnDefinition = "text")
    private String salePayload;

    @Column(name = "remote_sale_id")
    private Long remoteSaleId;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OfflineSaleConflictRecord() {
    }

    public OfflineSaleConflictRecord(
            String localSaleId,
            Instant createdAt,
            Long customerId,
            Long operatorUserId,
            String conflictSummary,
            String salePayload
    ) {
        this.localSaleId = localSaleId;
        this.createdAt = createdAt;
        this.customerId = customerId;
        this.operatorUserId = operatorUserId;
        this.status = OfflineConflictStatus.PENDING;
        this.conflictSummary = conflictSummary;
        this.salePayload = salePayload;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void refresh(String conflictSummary, String salePayload) {
        this.conflictSummary = conflictSummary;
        this.salePayload = salePayload;
        this.status = OfflineConflictStatus.PENDING;
        this.remoteSaleId = null;
        this.resolutionNote = null;
        this.resolvedByUserId = null;
        this.resolvedAt = null;
    }

    public void accept(Long remoteSaleId, Long resolvedByUserId, String resolutionNote) {
        this.status = OfflineConflictStatus.ACCEPTED;
        this.remoteSaleId = remoteSaleId;
        resolve(resolvedByUserId, resolutionNote);
    }

    public void reject(Long resolvedByUserId, String resolutionNote) {
        this.status = OfflineConflictStatus.REJECTED;
        resolve(resolvedByUserId, resolutionNote);
    }

    private void resolve(Long resolvedByUserId, String resolutionNote) {
        this.resolvedByUserId = resolvedByUserId;
        this.resolutionNote = resolutionNote;
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getLocalSaleId() {
        return localSaleId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public OfflineConflictStatus getStatus() {
        return status;
    }

    public String getConflictSummary() {
        return conflictSummary;
    }

    public String getSalePayload() {
        return salePayload;
    }

    public Long getRemoteSaleId() {
        return remoteSaleId;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Long getResolvedByUserId() {
        return resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
