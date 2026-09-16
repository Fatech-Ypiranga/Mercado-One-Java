package com.mercadoone.backend.modules.sales.application;

import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.catalog.infrastructure.ProductRepository;
import com.mercadoone.backend.modules.customer.application.CustomerService;
import com.mercadoone.backend.modules.customer.domain.Customer;
import com.mercadoone.backend.modules.inventory.application.InventoryService;
import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.sales.domain.PaymentMethod;
import com.mercadoone.backend.modules.sales.domain.Sale;
import com.mercadoone.backend.modules.sales.domain.SaleItem;
import com.mercadoone.backend.modules.sales.domain.SalePayment;
import com.mercadoone.backend.modules.sales.domain.SaleStatus;
import com.mercadoone.backend.modules.sales.infrastructure.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesService {

    private final ProductRepository products;
    private final SaleRepository sales;
    private final InventoryService inventory;
    private final CustomerService customerService;
    private final AuditService auditService;

    SalesService(
            ProductRepository products,
            SaleRepository sales,
            InventoryService inventory,
            CustomerService customerService,
            AuditService auditService
    ) {
        this.products = products;
        this.sales = sales;
        this.inventory = inventory;
        this.customerService = customerService;
        this.auditService = auditService;
    }

    @Transactional
    public Sale finalizeOnlineSale(SaleData data, Long operatorUserId) {
        if (data.items().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um item.");
        }
        if (data.payments().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um pagamento.");
        }

        Customer customer = data.customerId() == null ? null : customerService.getActiveCustomer(data.customerId());
        Sale sale = new Sale(operatorUserId, customer);
        for (SaleItemData item : data.items()) {
            Product product = products.findById(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("Produto inativo nao pode ser vendido.");
            }
            sale.addItem(new SaleItem(product, item.quantity()));
        }
        for (SalePaymentData payment : data.payments()) {
            sale.addPayment(new SalePayment(payment.method(), payment.amount()));
        }
        if (sale.paidAmount().compareTo(sale.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Valor pago deve ser igual ao total da venda.");
        }

        Sale savedSale = sales.save(sale);
        for (SaleItem item : savedSale.getItems()) {
            inventory.registerSaleOut(
                    item.getProduct(),
                    item.getQuantity(),
                    operatorUserId,
                    "Venda " + savedSale.getId()
            );
        }
        auditService.record("SALE_CONFIRMED", "sale", savedSale.getId(), operatorUserId, Map.of(
                "totalAmount", savedSale.getTotalAmount(),
                "itemCount", savedSale.getItems().size(),
                "customerId", customer == null ? "" : customer.getId()
        ));
        return savedSale;
    }

    @Transactional(readOnly = true)
    public SalesReport listSales(SaleFilters filters) {
        int page = filters.page() == null ? 0 : Math.max(filters.page(), 0);
        int size = filters.size() == null ? 50 : Math.clamp(filters.size(), 1, 200);
        Specification<Sale> specification = matches(filters);
        Page<Sale> result = sales.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        result.getContent().forEach(this::initializeResponseGraph);
        SalesTotals totals = totalsFor(sales.findAll(specification));
        return new SalesReport(
                result.getContent(),
                new PageData(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()),
                totals
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(SaleFilters filters) {
        List<Sale> rows = sales.findAll(matches(filters), Sort.by(Sort.Direction.DESC, "createdAt"));
        rows.forEach(this::initializeResponseGraph);
        StringBuilder csv = new StringBuilder("id,createdAt,status,operatorUserId,customer,totalAmount,payments,items\n");
        for (Sale sale : rows) {
            csv.append(sale.getId()).append(',')
                    .append(sale.getCreatedAt()).append(',')
                    .append(sale.getStatus()).append(',')
                    .append(sale.getOperatorUserId()).append(',')
                    .append(csv(sale.getCustomer() == null ? "" : sale.getCustomer().getName())).append(',')
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
        List<Sale> rows = sales.findAll(matches(filters));
        rows.forEach(this::initializeResponseGraph);
        for (Sale sale : rows) {
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                ProductSalesTotal current = totals.get(product.getId());
                BigDecimal quantity = current == null ? item.getQuantity() : current.quantity().add(item.getQuantity());
                BigDecimal totalAmount = current == null ? item.getTotalAmount() : current.totalAmount().add(item.getTotalAmount());
                totals.put(product.getId(), new ProductSalesTotal(
                        product.getId(),
                        product.getName(),
                        product.getBarcode(),
                        product.getSku(),
                        product.getUnit(),
                        quantity,
                        totalAmount
                ));
            }
        }
        return totals.values().stream()
                .sorted(Comparator.comparing(ProductSalesTotal::quantity).reversed()
                        .thenComparing(ProductSalesTotal::totalAmount, Comparator.reverseOrder()))
                .limit(maxRows)
                .toList();
    }

    @Transactional
    public OfflineSaleSyncResult syncOfflineSale(OfflineSaleData data, Long operatorUserId) {
        List<OfflineSaleConflict> conflicts = validateOfflineSale(data);
        if (!conflicts.isEmpty()) {
            auditService.record("OFFLINE_SALE_CONFLICT", "offline_sale", data.localSaleId(), operatorUserId, Map.of(
                    "conflictCount", conflicts.size()
            ));
            return new OfflineSaleSyncResult("CONFLICT", null, conflicts);
        }

        try {
            Sale sale = finalizeOnlineSale(new SaleData(
                    data.customerId(),
                    data.items().stream().map(item -> new SaleItemData(item.productId(), item.quantity())).toList(),
                    data.payments()
            ), operatorUserId);
            return new OfflineSaleSyncResult("SENT", sale, List.of());
        } catch (IllegalArgumentException exception) {
            return new OfflineSaleSyncResult("CONFLICT", null, List.of(new OfflineSaleConflict(
                    "STOCK_OR_PAYMENT",
                    exception.getMessage()
            )));
        }
    }

    @Transactional(readOnly = true)
    public List<OfflineSaleConflict> validateOfflineSale(OfflineSaleData data) {
        List<OfflineSaleConflict> conflicts = new ArrayList<>();
        BigDecimal itemTotal = BigDecimal.ZERO;
        for (OfflineSaleItemData item : data.items()) {
            Product product = products.findById(item.productId()).orElse(null);
            if (product == null) {
                conflicts.add(new OfflineSaleConflict("PRODUCT_NOT_FOUND", "Produto " + item.productId() + " nao encontrado."));
                continue;
            }
            if (!product.isActive()) {
                conflicts.add(new OfflineSaleConflict("PRODUCT_INACTIVE", "Produto " + product.getName() + " esta inativo."));
            }
            if (product.getSalePrice().compareTo(item.unitPrice()) != 0) {
                conflicts.add(new OfflineSaleConflict("PRICE_CHANGED", "Preco vigente de " + product.getName() + " mudou."));
            }
            itemTotal = itemTotal.add(item.unitPrice().multiply(item.quantity()).setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal paidTotal = data.payments().stream()
                .map(SalePaymentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (itemTotal.compareTo(paidTotal) != 0) {
            conflicts.add(new OfflineSaleConflict("PAYMENT_TOTAL", "Valor pago difere do total da venda offline."));
        }
        return conflicts;
    }

    @Transactional
    public Sale finalizeOfflineSale(OfflineSaleData data, Long operatorUserId) {
        return finalizeOnlineSale(new SaleData(
                data.customerId(),
                data.items().stream().map(item -> new SaleItemData(item.productId(), item.quantity())).toList(),
                data.payments()
        ), operatorUserId);
    }

    @Transactional
    public Sale acceptConflictedOfflineSale(OfflineSaleData data, Long operatorUserId) {
        if (data.items().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um item.");
        }
        if (data.payments().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um pagamento.");
        }

        Customer customer = data.customerId() == null ? null : customerService.getActiveCustomer(data.customerId());
        Sale sale = new Sale(operatorUserId, customer);
        for (OfflineSaleItemData item : data.items()) {
            Product product = products.findById(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("Produto inativo nao pode ser vendido.");
            }
            sale.addItem(new SaleItem(product, item.quantity(), item.unitPrice()));
        }
        for (SalePaymentData payment : data.payments()) {
            sale.addPayment(new SalePayment(payment.method(), payment.amount()));
        }
        if (sale.paidAmount().compareTo(sale.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Valor pago deve ser igual ao total da venda.");
        }

        Sale savedSale = sales.save(sale);
        for (SaleItem item : savedSale.getItems()) {
            inventory.registerSaleOut(
                    item.getProduct(),
                    item.getQuantity(),
                    operatorUserId,
                    "Venda offline aceita " + savedSale.getId()
            );
        }
        auditService.record("OFFLINE_SALE_ACCEPTED", "sale", savedSale.getId(), operatorUserId, Map.of(
                "localSaleId", data.localSaleId(),
                "totalAmount", savedSale.getTotalAmount(),
                "itemCount", savedSale.getItems().size()
        ));
        return savedSale;
    }

    private SalesTotals totalsFor(List<Sale> reportSales) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalItems = BigDecimal.ZERO;
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        for (Sale sale : reportSales) {
            initializeResponseGraph(sale);
            totalAmount = totalAmount.add(sale.getTotalAmount());
            for (SaleItem item : sale.getItems()) {
                totalItems = totalItems.add(item.getQuantity());
            }
            for (SalePayment payment : sale.getPayments()) {
                byMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
            }
        }
        return new SalesTotals(reportSales.size(), totalAmount, totalItems, byMethod);
    }

    private void initializeResponseGraph(Sale sale) {
        if (sale.getCustomer() != null) {
            sale.getCustomer().getName();
        }
        sale.getItems().forEach(item -> item.getProduct().getName());
        sale.getPayments().size();
    }

    private Specification<Sale> matches(SaleFilters filters) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (filters.from() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filters.from()));
            }
            if (filters.to() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filters.to()));
            }
            if (filters.operatorUserId() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("operatorUserId"), filters.operatorUserId()));
            }
            if (filters.customerId() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("customer").get("id"), filters.customerId()));
            }
            if (filters.status() != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), filters.status()));
            }
            return predicate;
        };
    }

    private String paymentSummary(Sale sale) {
        return sale.getPayments().stream()
                .map(payment -> payment.getMethod() + " " + payment.getAmount())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private String itemSummary(Sale sale) {
        return sale.getItems().stream()
                .map(item -> item.getQuantity() + " " + item.getProduct().getUnit() + " " + item.getProduct().getName())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public record SaleData(Long customerId, List<SaleItemData> items, List<SalePaymentData> payments) {
    }

    public record SaleItemData(Long productId, BigDecimal quantity) {
    }

    public record SalePaymentData(PaymentMethod method, BigDecimal amount) {
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
            BigDecimal totalAmount,
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

    public record OfflineSaleData(
            String localSaleId,
            Instant createdAt,
            Long customerId,
            List<OfflineSaleItemData> items,
            List<SalePaymentData> payments
    ) {
    }

    public record OfflineSaleItemData(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record OfflineSaleConflict(String code, String message) {
    }

    public record OfflineSaleSyncResult(String status, Sale sale, List<OfflineSaleConflict> conflicts) {
    }
}
