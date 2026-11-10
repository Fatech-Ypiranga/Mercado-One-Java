package com.mercadoone.backend.modules.sales.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.access.application.JwtService.AuthenticatedUser;
import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.customer.domain.Customer;
import com.mercadoone.backend.modules.sales.application.SalesService;
import com.mercadoone.backend.modules.sales.application.SalesService.ProductSalesTotal;
import com.mercadoone.backend.modules.sales.application.SalesService.SaleData;
import com.mercadoone.backend.modules.sales.application.SalesService.SaleFilters;
import com.mercadoone.backend.modules.sales.application.SalesService.SaleItemData;
import com.mercadoone.backend.modules.sales.application.SalesService.SalePaymentData;
import com.mercadoone.backend.modules.sales.application.SalesService.SalesReport;
import com.mercadoone.backend.modules.sales.domain.PaymentMethod;
import com.mercadoone.backend.modules.sales.domain.Sale;
import com.mercadoone.backend.modules.sales.domain.SaleItem;
import com.mercadoone.backend.modules.sales.domain.SalePayment;
import com.mercadoone.backend.modules.sales.domain.SaleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;

    SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    public ApiEnvelope<SaleResponse> finalizeSale(
            @Valid @RequestBody FinalizeSaleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(SaleResponse.from(salesService.finalizeOnlineSale(request.toData(), user.id(), user.role())));
    }

    @PostMapping("/{id}/cancel")
    public ApiEnvelope<SaleResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelSaleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(SaleResponse.from(salesService.cancelSale(id, request.reason(), user.id())));
    }

    @GetMapping("/discount-policy")
    public ApiEnvelope<DiscountPolicyResponse> discountPolicy(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiEnvelope.ok(new DiscountPolicyResponse(user.role(), salesService.maxDiscountPercentFor(user.role())));
    }

    @GetMapping("/mine")
    public ApiEnvelope<SalesReportResponse> mine(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiEnvelope.ok(SalesReportResponse.from(salesService.listSales(
                new SaleFilters(from, to, user.id(), null, status, page, size)
        )));
    }

    @GetMapping
    public ApiEnvelope<SalesReportResponse> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiEnvelope.ok(SalesReportResponse.from(salesService.listSales(
                new SaleFilters(from, to, operatorUserId, customerId, status, page, size)
        )));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SaleStatus status
    ) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vendas.csv\"")
                .body(salesService.exportCsv(new SaleFilters(from, to, operatorUserId, customerId, status, 0, 200)));
    }

    @GetMapping("/top-products")
    public ApiEnvelope<List<ProductSalesTotal>> topProducts(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiEnvelope.ok(salesService.topProducts(
                new SaleFilters(from, to, operatorUserId, customerId, status, 0, 200),
                limit
        ));
    }

    public record FinalizeSaleRequest(
            Long customerId,
            @NotEmpty List<@Valid SaleItemRequest> items,
            @NotEmpty List<@Valid SalePaymentRequest> payments,
            @DecimalMin("0.00") BigDecimal discountAmount,
            @Size(max = 80) String clientSaleId
    ) {
        SaleData toData() {
            return new SaleData(
                    customerId,
                    items.stream().map(SaleItemRequest::toData).toList(),
                    payments.stream().map(SalePaymentRequest::toData).toList(),
                    discountAmount,
                    clientSaleId
            );
        }
    }

    public record CancelSaleRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record DiscountPolicyResponse(UserRole role, BigDecimal maxDiscountPercent) {
    }

    public record SalesReportResponse(
            List<SaleResponse> items,
            PageResponse page,
            TotalsResponse totals
    ) {
        static SalesReportResponse from(SalesReport report) {
            return new SalesReportResponse(
                    report.items().stream().map(SaleResponse::from).toList(),
                    new PageResponse(
                            report.page().page(),
                            report.page().size(),
                            report.page().totalElements(),
                            report.page().totalPages()
                    ),
                    new TotalsResponse(
                            report.totals().saleCount(),
                            report.totals().canceledCount(),
                            report.totals().totalAmount(),
                            report.totals().discountAmount(),
                            report.totals().totalItems(),
                            report.totals().totalsByPaymentMethod()
                    )
            );
        }
    }

    public record PageResponse(int page, int size, long totalElements, int totalPages) {
    }

    public record TotalsResponse(
            long saleCount,
            long canceledCount,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal totalItems,
            java.util.Map<PaymentMethod, BigDecimal> totalsByPaymentMethod
    ) {
    }

    public record SaleItemRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity
    ) {
        SaleItemData toData() {
            return new SaleItemData(productId, quantity);
        }
    }

    public record SalePaymentRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
        SalePaymentData toData() {
            return new SalePaymentData(method, amount);
        }
    }

    public record SaleResponse(
            Long id,
            Long operatorUserId,
            CustomerSummary customer,
            SaleStatus status,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            List<SaleItemResponse> items,
            List<SalePaymentResponse> payments,
            Instant createdAt,
            String clientSaleId,
            Instant canceledAt,
            Long canceledByUserId,
            String cancelReason
    ) {
        public static SaleResponse from(Sale sale) {
            return new SaleResponse(
                    sale.getId(),
                    sale.getOperatorUserId(),
                    CustomerSummary.from(sale.getCustomer()),
                    sale.getStatus(),
                    sale.getGrossAmount(),
                    sale.getDiscountAmount(),
                    sale.getTotalAmount(),
                    sale.getItems().stream().map(SaleItemResponse::from).toList(),
                    sale.getPayments().stream().map(SalePaymentResponse::from).toList(),
                    sale.getCreatedAt(),
                    sale.getClientSaleId(),
                    sale.getCanceledAt(),
                    sale.getCanceledByUserId(),
                    sale.getCancelReason()
            );
        }
    }

    public record SaleItemResponse(
            Long id,
            ProductSummary product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount
    ) {
        static SaleItemResponse from(SaleItem item) {
            return new SaleItemResponse(
                    item.getId(),
                    ProductSummary.from(item.getProduct()),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalAmount()
            );
        }
    }

    public record SalePaymentResponse(Long id, PaymentMethod method, BigDecimal amount) {
        static SalePaymentResponse from(SalePayment payment) {
            return new SalePaymentResponse(payment.getId(), payment.getMethod(), payment.getAmount());
        }
    }

    public record ProductSummary(Long id, String name, String barcode, String sku, String unit) {
        static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    product.getSku(),
                    product.getUnit()
            );
        }
    }

    public record CustomerSummary(Long id, String name, String phone, String document) {
        static CustomerSummary from(Customer customer) {
            if (customer == null) {
                return null;
            }
            return new CustomerSummary(
                    customer.getId(),
                    customer.getName(),
                    customer.getPhone(),
                    customer.getDocument()
            );
        }
    }
}
