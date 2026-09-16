package com.mercadoone.backend.modules.inventory.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.JwtService.AuthenticatedUser;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.inventory.application.InventoryService;
import com.mercadoone.backend.modules.inventory.application.InventoryService.AdjustmentData;
import com.mercadoone.backend.modules.inventory.application.InventoryService.EntryData;
import com.mercadoone.backend.modules.inventory.domain.InventoryBalance;
import com.mercadoone.backend.modules.inventory.domain.InventoryMovement;
import com.mercadoone.backend.modules.inventory.domain.InventoryMovementType;
import com.mercadoone.backend.modules.supplier.domain.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    public ApiEnvelope<List<BalanceResponse>> balances(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(inventoryService.listBalances(search, categoryId, active).stream()
                .map(BalanceResponse::from)
                .toList());
    }

    @GetMapping("/movements")
    public ApiEnvelope<List<MovementResponse>> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryMovementType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long supplierId
    ) {
        return ApiEnvelope.ok(inventoryService.listMovements(productId, type, from, to, supplierId).stream()
                .map(MovementResponse::from)
                .toList());
    }

    @PostMapping("/entries")
    public ApiEnvelope<MovementResponse> entry(
            @Valid @RequestBody EntryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(MovementResponse.from(inventoryService.registerEntry(request.toData(), user.id())));
    }

    @PostMapping("/adjustments")
    public ApiEnvelope<MovementResponse> adjustment(
            @Valid @RequestBody AdjustmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(MovementResponse.from(inventoryService.registerAdjustment(request.toData(), user.id())));
    }

    public record EntryRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            Long supplierId,
            @Size(max = 160) String supplierName,
            @Size(max = 80) String documentNumber,
            @Size(max = 500) String note
    ) {
        EntryData toData() {
            return new EntryData(productId, quantity, supplierId, blankToNull(supplierName), blankToNull(documentNumber), blankToNull(note));
        }
    }

    public record AdjustmentRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.000") BigDecimal newQuantity,
            @NotBlank @Size(max = 160) String reason,
            @Size(max = 500) String note
    ) {
        AdjustmentData toData() {
            return new AdjustmentData(productId, newQuantity, reason.trim(), blankToNull(note));
        }
    }

    public record BalanceResponse(
            Long id,
            ProductSummary product,
            BigDecimal quantity,
            Instant createdAt,
            Instant updatedAt
    ) {
        static BalanceResponse from(InventoryBalance balance) {
            return new BalanceResponse(
                    balance.getId(),
                    ProductSummary.from(balance.getProduct()),
                    balance.getQuantity(),
                    balance.getCreatedAt(),
                    balance.getUpdatedAt()
            );
        }
    }

    public record MovementResponse(
            Long id,
            ProductSummary product,
            InventoryMovementType type,
            BigDecimal quantityDelta,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            String reason,
            String supplierName,
            SupplierSummary supplier,
            String documentNumber,
            String note,
            Long createdByUserId,
            Instant createdAt
    ) {
        static MovementResponse from(InventoryMovement movement) {
            return new MovementResponse(
                    movement.getId(),
                    ProductSummary.from(movement.getProduct()),
                    movement.getType(),
                    movement.getQuantityDelta(),
                    movement.getQuantityBefore(),
                    movement.getQuantityAfter(),
                    movement.getReason(),
                    movement.getSupplierName(),
                    SupplierSummary.from(movement.getSupplier()),
                    movement.getDocumentNumber(),
                    movement.getNote(),
                    movement.getCreatedByUserId(),
                    movement.getCreatedAt()
            );
        }
    }

    public record SupplierSummary(Long id, String name, String document) {
        static SupplierSummary from(Supplier supplier) {
            if (supplier == null) {
                return null;
            }
            return new SupplierSummary(supplier.getId(), supplier.getName(), supplier.getDocument());
        }
    }

    public record ProductSummary(
            Long id,
            String name,
            String barcode,
            String sku,
            String unit,
            boolean active
    ) {
        static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    product.getSku(),
                    product.getUnit(),
                    product.isActive()
            );
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
