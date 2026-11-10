package com.mercadoone.backend.modules.sales.application;

import com.mercadoone.backend.modules.access.domain.UserRole;
import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.catalog.application.CatalogService;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.customer.application.CustomerService;
import com.mercadoone.backend.modules.customer.domain.Customer;
import com.mercadoone.backend.modules.inventory.application.InventoryService;
import com.mercadoone.backend.modules.sales.domain.PaymentMethod;
import com.mercadoone.backend.modules.sales.domain.Sale;
import com.mercadoone.backend.modules.sales.domain.SaleItem;
import com.mercadoone.backend.modules.sales.domain.SalePayment;
import com.mercadoone.backend.modules.sales.infrastructure.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Confirmacao e cancelamento de vendas. Relatorios ficam em {@link SalesReportService}. */
@Service
public class SalesService {

    private final CatalogService catalog;
    private final SaleRepository sales;
    private final InventoryService inventory;
    private final CustomerService customerService;
    private final AuditService auditService;
    private final DiscountPolicy discountPolicy;

    SalesService(
            CatalogService catalog,
            SaleRepository sales,
            InventoryService inventory,
            CustomerService customerService,
            AuditService auditService,
            DiscountPolicy discountPolicy
    ) {
        this.catalog = catalog;
        this.sales = sales;
        this.inventory = inventory;
        this.customerService = customerService;
        this.auditService = auditService;
        this.discountPolicy = discountPolicy;
    }

    @Transactional(readOnly = true)
    public Optional<Sale> findByClientSaleId(String clientSaleId) {
        if (clientSaleId == null || clientSaleId.isBlank()) {
            return Optional.empty();
        }
        return sales.findByClientSaleId(clientSaleId.trim()).map(SalesService::initializeResponseGraph);
    }

    /**
     * Confirma uma venda. Idempotente por {@code clientSaleId}.
     *
     * @param command venda; itens sem {@code unitPrice} usam o preco vigente do catalogo
     * @param operatorUserId operador que realizou a venda
     * @param discountCheckRole perfil cujo limite de desconto se aplica; {@code null} quando o desconto ja foi aprovado
     * @param allowNegativeStock permite saldo negativo (venda offline ja entregue e aceita no admin)
     */
    @Transactional
    public Sale confirmSale(SaleCommand command, Long operatorUserId, UserRole discountCheckRole, boolean allowNegativeStock) {
        Optional<Sale> existing = findByClientSaleId(command.clientSaleId());
        if (existing.isPresent()) {
            return existing.get();
        }
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um item.");
        }
        if (command.payments().isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um pagamento.");
        }

        Customer customer = command.customerId() == null ? null : customerService.getActiveCustomer(command.customerId());
        Sale sale = new Sale(operatorUserId, customer, blankToNull(command.clientSaleId()), command.occurredAt());
        for (SaleItemData item : command.items()) {
            Product product = catalog.findProduct(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("Produto inativo nao pode ser vendido.");
            }
            sale.addItem(item.unitPrice() == null
                    ? new SaleItem(product, item.quantity())
                    : new SaleItem(product, item.quantity(), item.unitPrice()));
        }
        if (discountCheckRole != null && !discountPolicy.allows(discountCheckRole, command.discountAmount(), sale.getGrossAmount())) {
            throw new IllegalArgumentException("Desconto acima do permitido para o perfil.");
        }
        sale.applyDiscount(command.discountAmount());
        for (SalePaymentData payment : command.payments()) {
            sale.addPayment(new SalePayment(payment.method(), payment.amount()));
        }
        if (sale.paidAmount().compareTo(sale.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Valor pago deve ser igual ao total da venda.");
        }

        Sale savedSale = sales.save(sale);
        for (SaleItem item : savedSale.getItems()) {
            inventory.registerSaleOut(item.getProduct(), item.getQuantity(), operatorUserId,
                    "Venda " + savedSale.getId(), allowNegativeStock);
        }
        auditService.record("SALE_CONFIRMED", "sale", savedSale.getId(), operatorUserId, Map.of(
                "totalAmount", savedSale.getTotalAmount(),
                "discountAmount", savedSale.getDiscountAmount(),
                "itemCount", savedSale.getItems().size(),
                "customerId", customer == null ? "" : customer.getId(),
                "clientSaleId", savedSale.getClientSaleId() == null ? "" : savedSale.getClientSaleId()
        ));
        return savedSale;
    }

    @Transactional
    public Sale cancelSale(Long saleId, String reason, Long userId) {
        String cleanReason = blankToNull(reason);
        if (cleanReason == null) {
            throw new IllegalArgumentException("Informe o motivo do cancelamento.");
        }
        Sale sale = sales.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada."));
        sale.cancel(userId, cleanReason);
        for (SaleItem item : sale.getItems()) {
            inventory.registerSaleReturn(item.getProduct(), item.getQuantity(), userId, "Cancelamento venda " + sale.getId());
        }
        auditService.record("SALE_CANCELED", "sale", sale.getId(), userId, Map.of(
                "reason", cleanReason,
                "totalAmount", sale.getTotalAmount()
        ));
        return initializeResponseGraph(sale);
    }

    public BigDecimal maxDiscountPercentFor(UserRole role) {
        return discountPolicy.maxPercentFor(role);
    }

    public boolean discountAllowed(UserRole role, BigDecimal discount, BigDecimal grossAmount) {
        return discountPolicy.allows(role, discount, grossAmount);
    }

    static Sale initializeResponseGraph(Sale sale) {
        if (sale.getCustomer() != null) {
            sale.getCustomer().getName();
        }
        sale.getItems().forEach(item -> item.getProduct().getName());
        sale.getPayments().size();
        return sale;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** {@code occurredAt} nulo usa o instante da gravacao. */
    public record SaleCommand(
            Long customerId,
            List<SaleItemData> items,
            List<SalePaymentData> payments,
            BigDecimal discountAmount,
            String clientSaleId,
            Instant occurredAt
    ) {
    }

    /** {@code unitPrice} nulo usa o preco vigente do catalogo. */
    public record SaleItemData(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        public SaleItemData(Long productId, BigDecimal quantity) {
            this(productId, quantity, null);
        }
    }

    public record SalePaymentData(PaymentMethod method, BigDecimal amount) {
    }
}
