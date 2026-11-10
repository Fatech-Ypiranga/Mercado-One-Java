package com.mercadoone.backend.modules.sales.application;

import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.sales.domain.PaymentMethod;
import com.mercadoone.backend.modules.sales.domain.Sale;
import com.mercadoone.backend.modules.sales.domain.SaleItem;
import com.mercadoone.backend.modules.sales.domain.SalePayment;
import com.mercadoone.backend.modules.sales.domain.SaleStatus;
import com.mercadoone.backend.modules.sales.infrastructure.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consultas, totais e exportacao de vendas. */
@Service
public class SalesReportService {

    private final SaleRepository sales;

    SalesReportService(SaleRepository sales) {
        this.sales = sales;
    }

    @Transactional(readOnly = true)
    public SalesReport listSales(SaleFilters filters) {
        int page = filters.page() == null ? 0 : Math.max(filters.page(), 0);
        int size = filters.size() == null ? 50 : Math.clamp(filters.size(), 1, 200);
        Specification<Sale> specification = matches(filters);
        Page<Sale> result = sales.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        result.getContent().forEach(SalesService::initializeResponseGraph);
        return new SalesReport(
                result.getContent(),
                new PageData(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()),
                totalsFor(sales.findAll(specification))
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(SaleFilters filters) {
        List<Sale> rows = sales.findAll(matches(filters), Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder csv = new StringBuilder("id,createdAt,status,operatorUserId,customer,grossAmount,discountAmount,totalAmount,payments,items\n");
        for (Sale sale : rows) {
            csv.append(sale.getId()).append(',')
                    .append(sale.getCreatedAt()).append(',')
                    .append(sale.getStatus()).append(',')
                    .append(sale.getOperatorUserId()).append(',')
                    .append(csv(sale.getCustomer() == null ? "" : sale.getCustomer().getName())).append(',')
                    .append(sale.getGrossAmount()).append(',')
                    .append(sale.getDiscountAmount()).append(',')
                    .append(sale.getTotalAmount()).append(',')
                    .append(csv(paymentSummary(sale))).append(',')
                    .append(csv(itemSummary(sale))).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public List<ProductSalesTotal> topProducts(SaleFilters filters, Integer limit) {
        int maxRows = limit == null ? 10 : Math.clamp(limit, 1, 50);
        Map<Long, ProductSalesTotal> totals = new HashMap<>();
        for (Sale sale : sales.findAll(matches(filters))) {
            if (sale.getStatus() != SaleStatus.CONFIRMED) {
                continue;
            }
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                ProductSalesTotal current = totals.get(product.getId());
                totals.put(product.getId(), new ProductSalesTotal(
                        product.getId(),
                        product.getName(),
                        product.getBarcode(),
                        product.getSku(),
                        product.getUnit(),
                        current == null ? item.getQuantity() : current.quantity().add(item.getQuantity()),
                        current == null ? item.getTotalAmount() : current.totalAmount().add(item.getTotalAmount())
                ));
            }
        }
        return totals.values().stream()
                .sorted(Comparator.comparing(ProductSalesTotal::quantity).reversed()
                        .thenComparing(ProductSalesTotal::totalAmount, Comparator.reverseOrder()))
                .limit(maxRows)
                .toList();
    }

    private static SalesTotals totalsFor(List<Sale> reportSales) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalItems = BigDecimal.ZERO;
        long confirmedCount = 0;
        long canceledCount = 0;
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        for (Sale sale : reportSales) {
            if (sale.getStatus() != SaleStatus.CONFIRMED) {
                canceledCount++;
                continue;
            }
            confirmedCount++;
            totalAmount = totalAmount.add(sale.getTotalAmount());
            discountAmount = discountAmount.add(sale.getDiscountAmount());
            for (SaleItem item : sale.getItems()) {
                totalItems = totalItems.add(item.getQuantity());
            }
            for (SalePayment payment : sale.getPayments()) {
                byMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
            }
        }
        return new SalesTotals(confirmedCount, canceledCount, totalAmount, discountAmount, totalItems, byMethod);
    }

    private static Specification<Sale> matches(SaleFilters filters) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (filters.from() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), filters.from()));
            }
            if (filters.to() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), filters.to()));
            }
            if (filters.operatorUserId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("operatorUserId"), filters.operatorUserId()));
            }
            if (filters.customerId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("customer").get("id"), filters.customerId()));
            }
            if (filters.status() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), filters.status()));
            }
            return predicate;
        };
    }

    private static String paymentSummary(Sale sale) {
        return String.join("; ", sale.getPayments().stream()
                .map(payment -> payment.getMethod() + " " + payment.getAmount())
                .toList());
    }

    private static String itemSummary(Sale sale) {
        return String.join("; ", sale.getItems().stream()
                .map(item -> item.getQuantity() + " " + item.getProduct().getUnit() + " " + item.getProduct().getName())
                .toList());
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public record SaleFilters(
            Instant from,
            Instant to,
            Long operatorUserId,
            Long customerId,
            SaleStatus status,
            Integer page,
            Integer size
    ) {
    }

    public record SalesReport(List<Sale> items, PageData page, SalesTotals totals) {
    }

    public record PageData(int page, int size, long totalElements, int totalPages) {
    }

    public record SalesTotals(
            long saleCount,
            long canceledCount,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal totalItems,
            Map<PaymentMethod, BigDecimal> totalsByPaymentMethod
    ) {
    }

    public record ProductSalesTotal(
            Long productId,
            String name,
            String barcode,
            String sku,
            String unit,
            BigDecimal quantity,
            BigDecimal totalAmount
    ) {
    }
}
