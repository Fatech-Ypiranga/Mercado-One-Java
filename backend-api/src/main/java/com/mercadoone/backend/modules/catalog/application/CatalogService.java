package com.mercadoone.backend.modules.catalog.application;

import com.mercadoone.backend.modules.audit.application.AuditService;
import com.mercadoone.backend.modules.catalog.domain.Category;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.catalog.domain.Product.ProductData;
import com.mercadoone.backend.modules.catalog.infrastructure.CategoryRepository;
import com.mercadoone.backend.modules.catalog.infrastructure.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final AuditService auditService;

    CatalogService(CategoryRepository categories, ProductRepository products, AuditService auditService) {
        this.categories = categories;
        this.products = products;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories(String search, Boolean active) {
        return categories.findAll(categoryFilters(search, active), Sort.by("name").ascending());
    }

    @Transactional
    public Category createCategory(String name, boolean active) {
        return categories.save(new Category(name.trim(), active));
    }

    @Transactional
    public Category updateCategory(Long id, String name, boolean active) {
        Category category = findCategory(id);
        category.update(name.trim(), active);
        return category;
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts(String search, Long categoryId, Boolean active) {
        return products.findAll(productFilters(search, categoryId, active), Sort.by("name").ascending());
    }

    @Transactional
    public Product createProduct(ProductData data, Long categoryId, Long actorUserId) {
        Product product = products.save(new Product(data, findCategory(categoryId)));
        auditService.record("PRODUCT_CREATED", "catalog_product", product.getId(), actorUserId, snapshot(product));
        return product;
    }

    @Transactional
    public Product updateProduct(Long id, ProductData data, Long categoryId, Long actorUserId) {
        Product product = products.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
        Map<String, Object> before = snapshot(product);
        product.update(data, findCategory(categoryId));
        Map<String, Object> after = snapshot(product);
        Map<String, Object> changes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            Object previous = before.get(entry.getKey());
            if (!Objects.equals(normalize(previous), normalize(entry.getValue()))) {
                changes.put(entry.getKey(), Map.of(
                        "from", previous == null ? "" : previous,
                        "to", entry.getValue() == null ? "" : entry.getValue()
                ));
            }
        }
        auditService.record("PRODUCT_UPDATED", "catalog_product", product.getId(), actorUserId, Map.of(
                "name", product.getName(),
                "changes", changes
        ));
        return product;
    }

    private static Map<String, Object> snapshot(Product product) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", product.getName());
        values.put("barcode", product.getBarcode());
        values.put("sku", product.getSku());
        values.put("categoryId", product.getCategory().getId());
        values.put("unit", product.getUnit());
        values.put("salePrice", product.getSalePrice());
        values.put("active", product.isActive());
        values.put("ncm", product.getNcm());
        values.put("cest", product.getCest());
        values.put("defaultCfop", product.getDefaultCfop());
        values.put("merchandiseOrigin", product.getMerchandiseOrigin());
        values.put("taxClassification", product.getTaxClassification());
        return values;
    }

    private static Object normalize(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        return value;
    }

    private Category findCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada."));
    }

    private static Specification<Category> categoryFilters(String search, Boolean active) {
        Specification<Category> filters = Specification.unrestricted();
        if (active != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (!isBlank(search)) {
            filters = filters.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("name")), "%" + search.toLowerCase().trim() + "%"));
        }
        return filters;
    }

    private static Specification<Product> productFilters(String search, Long categoryId, Boolean active) {
        Specification<Product> filters = Specification.unrestricted();
        if (active != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (categoryId != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("category").get("id"), categoryId));
        }
        if (!isBlank(search)) {
            filters = filters.and((root, query, builder) -> {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                return builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("sku")), pattern),
                        builder.like(builder.lower(root.get("barcode")), pattern)
                );
            });
        }
        return filters;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
