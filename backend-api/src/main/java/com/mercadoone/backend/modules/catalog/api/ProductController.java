package com.mercadoone.backend.modules.catalog.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.catalog.application.CatalogService;
import com.mercadoone.backend.modules.catalog.domain.Category;
import com.mercadoone.backend.modules.catalog.domain.Product;
import com.mercadoone.backend.modules.catalog.domain.Product.ProductData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductController {

    private final CatalogService catalogService;

    ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiEnvelope<List<ProductResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(catalogService.listProducts(search, categoryId, active).stream()
                .map(ProductResponse::from)
                .toList());
    }

    @PostMapping
    public ApiEnvelope<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiEnvelope.ok(ProductResponse.from(catalogService.createProduct(request.toData(), request.categoryId())));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiEnvelope.ok(ProductResponse.from(catalogService.updateProduct(id, request.toData(), request.categoryId())));
    }

    public record ProductRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String barcode,
            @Size(max = 80) String sku,
            @NotNull Long categoryId,
            @NotBlank @Size(max = 24) String unit,
            @NotNull @DecimalMin("0.01") BigDecimal salePrice,
            boolean active,
            @Size(max = 16) String ncm,
            @Size(max = 16) String cest,
            @Size(max = 16) String defaultCfop,
            @Size(max = 80) String merchandiseOrigin,
            @Size(max = 120) String taxClassification
    ) {
        ProductData toData() {
            return new ProductData(
                    name.trim(),
                    blankToNull(barcode),
                    blankToNull(sku),
                    unit.trim(),
                    salePrice,
                    active,
                    blankToNull(ncm),
                    blankToNull(cest),
                    blankToNull(defaultCfop),
                    blankToNull(merchandiseOrigin),
                    blankToNull(taxClassification)
            );
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public record ProductResponse(
            Long id,
            String name,
            String barcode,
            String sku,
            CategorySummary category,
            String unit,
            BigDecimal salePrice,
            boolean active,
            String ncm,
            String cest,
            String defaultCfop,
            String merchandiseOrigin,
            String taxClassification,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    product.getSku(),
                    CategorySummary.from(product.getCategory()),
                    product.getUnit(),
                    product.getSalePrice(),
                    product.isActive(),
                    product.getNcm(),
                    product.getCest(),
                    product.getDefaultCfop(),
                    product.getMerchandiseOrigin(),
                    product.getTaxClassification(),
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            );
        }
    }

    public record CategorySummary(Long id, String name, boolean active) {
        static CategorySummary from(Category category) {
            return new CategorySummary(category.getId(), category.getName(), category.isActive());
        }
    }
}
