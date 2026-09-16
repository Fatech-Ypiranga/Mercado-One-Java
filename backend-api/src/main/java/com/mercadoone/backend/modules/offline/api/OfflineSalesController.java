package com.mercadoone.backend.modules.offline.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.JwtService.AuthenticatedUser;
import com.mercadoone.backend.modules.offline.application.OfflineSalesService;
import com.mercadoone.backend.modules.offline.application.OfflineSalesService.ConflictResolutionAction;
import com.mercadoone.backend.modules.offline.domain.OfflineConflictStatus;
import com.mercadoone.backend.modules.offline.domain.OfflineSaleConflictRecord;
import com.mercadoone.backend.modules.sales.api.SalesController.SaleResponse;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleConflict;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleData;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleItemData;
import com.mercadoone.backend.modules.sales.application.SalesService.OfflineSaleSyncResult;
import com.mercadoone.backend.modules.sales.application.SalesService.SalePaymentData;
import com.mercadoone.backend.modules.sales.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/offline/sales")
public class OfflineSalesController {

    private final OfflineSalesService offlineSalesService;

    OfflineSalesController(OfflineSalesService offlineSalesService) {
        this.offlineSalesService = offlineSalesService;
    }

    @PostMapping("/sync")
    public ApiEnvelope<OfflineSaleSyncResponse> sync(
            @Valid @RequestBody OfflineSaleSyncRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(OfflineSaleSyncResponse.from(offlineSalesService.syncOfflineSale(request.toData(), user.id())));
    }

    @GetMapping("/conflicts")
    public ApiEnvelope<List<OfflineConflictResponse>> conflicts(
            @RequestParam(required = false, defaultValue = "PENDING") OfflineConflictStatus status
    ) {
        return ApiEnvelope.ok(offlineSalesService.listConflicts(status).stream()
                .map(OfflineConflictResponse::from)
                .toList());
    }

    @PostMapping("/conflicts/{id}/resolve")
    public ApiEnvelope<OfflineConflictResponse> resolve(
            @PathVariable Long id,
            @Valid @RequestBody OfflineConflictResolutionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(OfflineConflictResponse.from(offlineSalesService.resolveConflict(
                id,
                request.action(),
                request.note(),
                user.id()
        )));
    }

    public record OfflineSaleSyncRequest(
            @NotBlank @jakarta.validation.constraints.Size(max = 80) String localSaleId,
            @NotNull Instant createdAt,
            Long customerId,
            @NotEmpty List<@Valid OfflineSaleItemRequest> items,
            @NotEmpty List<@Valid OfflineSalePaymentRequest> payments
    ) {
        OfflineSaleData toData() {
            return new OfflineSaleData(
                    localSaleId.trim(),
                    createdAt,
                    customerId,
                    items.stream().map(OfflineSaleItemRequest::toData).toList(),
                    payments.stream().map(OfflineSalePaymentRequest::toData).toList()
            );
        }
    }

    public record OfflineSaleItemRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice
    ) {
        OfflineSaleItemData toData() {
            return new OfflineSaleItemData(productId, quantity, unitPrice);
        }
    }

    public record OfflineSalePaymentRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
        SalePaymentData toData() {
            return new SalePaymentData(method, amount);
        }
    }

    public record OfflineSaleSyncResponse(
            String status,
            SaleResponse sale,
            List<OfflineSaleConflict> conflicts
    ) {
        static OfflineSaleSyncResponse from(OfflineSaleSyncResult result) {
            return new OfflineSaleSyncResponse(
                    result.status(),
                    result.sale() == null ? null : SaleResponse.from(result.sale()),
                    result.conflicts()
            );
        }
    }

    public record OfflineConflictResolutionRequest(
            @NotNull ConflictResolutionAction action,
            @Size(max = 500) String note
    ) {
    }

    public record OfflineConflictResponse(
            Long id,
            String localSaleId,
            java.time.Instant createdAt,
            Long customerId,
            Long operatorUserId,
            OfflineConflictStatus status,
            String conflictSummary,
            String salePayload,
            Long remoteSaleId,
            String resolutionNote,
            Long resolvedByUserId,
            java.time.Instant resolvedAt,
            java.time.Instant updatedAt
    ) {
        static OfflineConflictResponse from(OfflineSaleConflictRecord conflict) {
            return new OfflineConflictResponse(
                    conflict.getId(),
                    conflict.getLocalSaleId(),
                    conflict.getCreatedAt(),
                    conflict.getCustomerId(),
                    conflict.getOperatorUserId(),
                    conflict.getStatus(),
                    conflict.getConflictSummary(),
                    conflict.getSalePayload(),
                    conflict.getRemoteSaleId(),
                    conflict.getResolutionNote(),
                    conflict.getResolvedByUserId(),
                    conflict.getResolvedAt(),
                    conflict.getUpdatedAt()
            );
        }
    }
}
