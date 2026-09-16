package com.mercadoone.backend.modules.inventory.application;

import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.catalog.infrastructure.ProductRepository;
import com.mercadoone.backend.modules.inventory.domain.InventoryBalance;
import com.mercadoone.backend.modules.inventory.domain.InventoryMovement;
import com.mercadoone.backend.modules.inventory.domain.InventoryMovement.MovementData;
import com.mercadoone.backend.modules.inventory.domain.InventoryMovementType;
import com.mercadoone.backend.modules.inventory.infrastructure.InventoryBalanceRepository;
import com.mercadoone.backend.modules.inventory.infrastructure.InventoryMovementRepository;
import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.supplier.application.SupplierService;
import com.mercadoone.backend.modules.supplier.domain.Supplier;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository products;
    private final InventoryBalanceRepository balances;
    private final InventoryMovementRepository movements;
    private final SupplierService supplierService;
    private final AuditService auditService;

    InventoryService(
            ProductRepository products,
            InventoryBalanceRepository balances,
            InventoryMovementRepository movements,
            SupplierService supplierService,
            AuditService auditService
    ) {
        this.products = products;
        this.balances = balances;
        this.movements = movements;
        this.supplierService = supplierService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<InventoryBalance> listBalances(String search, Long categoryId, Boolean active) {
        return balances.findAll(balanceFilters(search, categoryId, active), Sort.by("product.name").ascending());
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> listMovements(
            Long productId,
            InventoryMovementType type,
            Instant from,
            Instant to,
            Long supplierId
    ) {
        return movements.findAll(movementFilters(productId, type, from, to, supplierId), Sort.by("createdAt").descending());
    }

    @Transactional
    public InventoryMovement registerEntry(EntryData data, Long createdByUserId) {
        Product product = findActiveProduct(data.productId());
        Supplier supplier = data.supplierId() == null ? null : supplierService.getActiveSupplier(data.supplierId());
        String supplierName = supplier == null ? data.supplierName() : supplier.getName();
        InventoryBalance balance = findOrCreateBalance(product);
        BigDecimal quantityBefore = balance.getQuantity();
        BigDecimal quantityAfter = balance.add(data.quantity());

        InventoryMovement movement = movements.save(new InventoryMovement(new MovementData(
                product,
                InventoryMovementType.ENTRY,
                data.quantity(),
                quantityBefore,
                quantityAfter,
                "Entrada de estoque",
                supplierName,
                supplier,
                data.documentNumber(),
                data.note(),
                createdByUserId
        )));
        auditService.record("INVENTORY_ENTRY", "inventory_movement", movement.getId(), createdByUserId, java.util.Map.of(
                "productId", product.getId(),
                "quantity", data.quantity(),
                "supplierId", supplier == null ? "" : supplier.getId()
        ));
        return movement;
    }

    @Transactional
    public InventoryMovement registerAdjustment(AdjustmentData data, Long createdByUserId) {
        Product product = findActiveProduct(data.productId());
        InventoryBalance balance = findOrCreateBalance(product);
        BigDecimal quantityBefore = balance.getQuantity();
        BigDecimal quantityAfter = balance.adjustTo(data.newQuantity());
        BigDecimal quantityDelta = quantityAfter.subtract(quantityBefore);

        InventoryMovement movement = movements.save(new InventoryMovement(new MovementData(
                product,
                InventoryMovementType.ADJUSTMENT,
                quantityDelta,
                quantityBefore,
                quantityAfter,
                data.reason(),
                null,
                null,
                null,
                data.note(),
                createdByUserId
        )));
        auditService.record("INVENTORY_ADJUSTMENT", "inventory_movement", movement.getId(), createdByUserId, java.util.Map.of(
                "productId", product.getId(),
                "quantityBefore", quantityBefore,
                "quantityAfter", quantityAfter
        ));
        return movement;
    }

    @Transactional
    public InventoryMovement registerSaleOut(Product product, BigDecimal quantity, Long createdByUserId, String saleReference) {
        if (!product.isActive()) {
            throw new IllegalArgumentException("Produto inativo nao pode ser vendido.");
        }
        InventoryBalance balance = findOrCreateBalance(product);
        BigDecimal quantityBefore = balance.getQuantity();
        BigDecimal quantityAfter = quantityBefore.subtract(quantity);
        if (quantityAfter.signum() < 0) {
            throw new IllegalArgumentException("Estoque insuficiente para finalizar a venda.");
        }
        balance.adjustTo(quantityAfter);

        InventoryMovement movement = movements.save(new InventoryMovement(new MovementData(
                product,
                InventoryMovementType.SALE,
                quantity.negate(),
                quantityBefore,
                quantityAfter,
                saleReference,
                null,
                null,
                null,
                null,
                createdByUserId
        )));
        auditService.record("INVENTORY_SALE_OUT", "inventory_movement", movement.getId(), createdByUserId, java.util.Map.of(
                "productId", product.getId(),
                "quantity", quantity
        ));
        return movement;
    }

    private InventoryBalance findOrCreateBalance(Product product) {
        return balances.findLockedByProductId(product.getId())
                .orElseGet(() -> balances.save(new InventoryBalance(product)));
    }

    private Product findActiveProduct(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Produto inativo nao recebe movimentacao de estoque.");
        }
        return product;
    }

    private static Specification<InventoryBalance> balanceFilters(String search, Long categoryId, Boolean active) {
        Specification<InventoryBalance> filters = Specification.unrestricted();
        if (categoryId != null) {
            filters = filters.and((root, query, builder) ->
                    builder.equal(root.get("product").get("category").get("id"), categoryId));
        }
        if (active != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("product").get("active"), active));
        }
        if (!isBlank(search)) {
            filters = filters.and((root, query, builder) -> {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                return builder.or(
                        builder.like(builder.lower(root.get("product").get("name")), pattern),
                        builder.like(builder.lower(root.get("product").get("sku")), pattern),
                        builder.like(builder.lower(root.get("product").get("barcode")), pattern)
                );
            });
        }
        return filters;
    }

    private static Specification<InventoryMovement> movementFilters(
            Long productId,
            InventoryMovementType type,
            Instant from,
            Instant to,
            Long supplierId
    ) {
        Specification<InventoryMovement> filters = Specification.unrestricted();
        if (productId != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("product").get("id"), productId));
        }
        if (type != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        if (from != null) {
            filters = filters.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            filters = filters.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (supplierId != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("supplier").get("id"), supplierId));
        }
        return filters;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record EntryData(
            Long productId,
            BigDecimal quantity,
            Long supplierId,
            String supplierName,
            String documentNumber,
            String note
    ) {
    }

    public record AdjustmentData(Long productId, BigDecimal newQuantity, String reason, String note) {
    }
}
