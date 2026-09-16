package com.mercadoone.backend.modules.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "document", length = 40)
    private String document;

    @Column(name = "contact_consent", nullable = false)
    private boolean contactConsent;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    public Customer(CustomerData data) {
        update(data);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void update(CustomerData data) {
        name = data.name();
        phone = data.phone();
        email = data.email();
        document = data.document();
        contactConsent = data.contactConsent();
        active = data.active();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getDocument() {
        return document;
    }

    public boolean isContactConsent() {
        return contactConsent;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public record CustomerData(
            String name,
            String phone,
            String email,
            String document,
            boolean contactConsent,
            boolean active
    ) {
    }
}
