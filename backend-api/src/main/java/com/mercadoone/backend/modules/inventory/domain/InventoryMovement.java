package com.mercadoone.backend.modules.inventory.domain;

import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.supplier.domain.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private InventoryMovementType type;

    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityDelta;

    @Column(name = "quantity_before", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityAfter;

    @Column(name = "reason", nullable = false, length = 160)
    private String reason;

    @Column(name = "supplier_name", length = 160)
    private String supplierName;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "document_number", length = 80)
    private String documentNumber;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(MovementData data) {
        this.product = data.product();
        this.type = data.type();
        this.quantityDelta = data.quantityDelta();
        this.quantityBefore = data.quantityBefore();
        this.quantityAfter = data.quantityAfter();
        this.reason = data.reason();
        this.supplierName = data.supplierName();
        this.supplier = data.supplier();
        this.documentNumber = data.documentNumber();
        this.note = data.note();
        this.createdByUserId = data.createdByUserId();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public InventoryMovementType getType() {
        return type;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getNote() {
        return note;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public record MovementData(
            Product product,
            InventoryMovementType type,
            BigDecimal quantityDelta,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            String reason,
            String supplierName,
            Supplier supplier,
            String documentNumber,
            String note,
            Long createdByUserId
    ) {
    }
}
