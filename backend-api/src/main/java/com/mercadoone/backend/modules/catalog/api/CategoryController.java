package com.mercadoone.backend.modules.catalog.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.catalog.application.CatalogService;
import com.mercadoone.backend.modules.catalog.domain.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/catalog/categories")
public class CategoryController {

    private final CatalogService catalogService;

    CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiEnvelope<List<CategoryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(catalogService.listCategories(search, active).stream()
                .map(CategoryResponse::from)
                .toList());
    }

    @PostMapping
    public ApiEnvelope<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiEnvelope.ok(CategoryResponse.from(catalogService.createCategory(request.name(), request.active())));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ApiEnvelope.ok(CategoryResponse.from(catalogService.updateCategory(id, request.name(), request.active())));
    }

    public record CategoryRequest(@NotBlank @Size(max = 120) String name, boolean active) {
    }

    public record CategoryResponse(Long id, String name, boolean active, Instant createdAt, Instant updatedAt) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.isActive(),
                    category.getCreatedAt(),
                    category.getUpdatedAt()
            );
        }
    }
}
