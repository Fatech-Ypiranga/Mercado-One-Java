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

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

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
        this.totalAmount = BigDecimal.ZERO;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void addItem(SaleItem item) {
        item.attachTo(this);
        items.add(item);
        totalAmount = totalAmount.add(item.getTotalAmount());
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
