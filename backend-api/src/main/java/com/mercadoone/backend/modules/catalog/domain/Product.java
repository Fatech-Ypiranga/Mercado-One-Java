package com.mercadoone.backend.modules.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "catalog_products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "barcode", length = 80)
    private String barcode;

    @Column(name = "sku", length = 80)
    private String sku;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit", nullable = false, length = 24)
    private String unit;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "ncm", length = 16)
    private String ncm;

    @Column(name = "cest", length = 16)
    private String cest;

    @Column(name = "default_cfop", length = 16)
    private String defaultCfop;

    @Column(name = "merchandise_origin", length = 80)
    private String merchandiseOrigin;

    @Column(name = "tax_classification", length = 120)
    private String taxClassification;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(ProductData data, Category category) {
        update(data, category);
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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getSku() {
        return sku;
    }

    public Category getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public boolean isActive() {
        return active;
    }

    public String getNcm() {
        return ncm;
    }

    public String getCest() {
        return cest;
    }

    public String getDefaultCfop() {
        return defaultCfop;
    }

    public String getMerchandiseOrigin() {
        return merchandiseOrigin;
    }

    public String getTaxClassification() {
        return taxClassification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(ProductData data, Category category) {
        this.name = data.name();
        this.barcode = data.barcode();
        this.sku = data.sku();
        this.category = category;
        this.unit = data.unit();
        this.salePrice = data.salePrice();
        this.active = data.active();
        this.ncm = data.ncm();
        this.cest = data.cest();
        this.defaultCfop = data.defaultCfop();
        this.merchandiseOrigin = data.merchandiseOrigin();
        this.taxClassification = data.taxClassification();
    }

    public record ProductData(
            String name,
            String barcode,
            String sku,
            String unit,
            BigDecimal salePrice,
            boolean active,
            String ncm,
            String cest,
            String defaultCfop,
            String merchandiseOrigin,
            String taxClassification
    ) {
    }
}
