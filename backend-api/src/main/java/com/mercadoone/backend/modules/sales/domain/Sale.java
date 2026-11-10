package com.mercadoone.backend.modules.sales.domain;

import com.mercadoone.backend.modules.customer.domain.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SaleStatus status;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "client_sale_id", length = 80, unique = true)
    private String clientSaleId;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "canceled_by_user_id")
    private Long canceledByUserId;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<SalePayment> payments = new ArrayList<>();

    protected Sale() {
    }

    public Sale(Long operatorUserId, Customer customer) {
        this.operatorUserId = operatorUserId;
        this.customer = customer;
        this.status = SaleStatus.CONFIRMED;
        this.grossAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public Sale(Long operatorUserId, Customer customer, String clientSaleId, Instant occurredAt) {
        this(operatorUserId, customer);
        this.clientSaleId = clientSaleId;
        this.createdAt = occurredAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addItem(SaleItem item) {
        item.attachTo(this);
        items.add(item);
        grossAmount = grossAmount.add(item.getTotalAmount());
        totalAmount = grossAmount.subtract(discountAmount);
    }

    public void applyDiscount(BigDecimal discount) {
        BigDecimal normalized = discount == null ? BigDecimal.ZERO : discount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("Desconto nao pode ser negativo.");
        }
        if (normalized.signum() > 0 && normalized.compareTo(grossAmount) >= 0) {
            throw new IllegalArgumentException("Desconto deve ser menor que o total bruto da venda.");
        }
        discountAmount = normalized;
        totalAmount = grossAmount.subtract(discountAmount);
    }

    public void cancel(Long userId, String reason) {
        if (status == SaleStatus.CANCELED) {
            throw new IllegalArgumentException("Venda ja esta cancelada.");
        }
        status = SaleStatus.CANCELED;
        canceledAt = Instant.now();
        canceledByUserId = userId;
        cancelReason = reason;
    }

    public void addPayment(SalePayment payment) {
        payment.attachTo(this);
        payments.add(payment);
    }

    public BigDecimal paidAmount() {
        return payments.stream()
                .map(SalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getClientSaleId() {
        return clientSaleId;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public Long getCanceledByUserId() {
        return canceledByUserId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<SalePayment> getPayments() {
        return Collections.unmodifiableList(payments);
    }
}
