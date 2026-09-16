package com.mercadoone.backend.modules.catalog.application;

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

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    CatalogService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
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
    public Product createProduct(ProductData data, Long categoryId) {
        return products.save(new Product(data, findCategory(categoryId)));
    }

    @Transactional
    public Product updateProduct(Long id, ProductData data, Long categoryId) {
        Product product = products.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado."));
        product.update(data, findCategory(categoryId));
        return product;
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
