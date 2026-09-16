package com.mercadoone.backend.modules.supplier.application;

import com.mercadoone.backend.modules.supplier.domain.Supplier;
import com.mercadoone.backend.modules.supplier.domain.Supplier.SupplierData;
import com.mercadoone.backend.modules.supplier.infrastructure.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;

    SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers(String search, Boolean active) {
        return suppliers.findAll(matches(search, active), Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public Supplier getSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Supplier getActiveSupplier(Long id) {
        Supplier supplier = getSupplier(id);
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Fornecedor inativo nao pode ser vinculado a entrada.");
        }
        return supplier;
    }

    @Transactional
    public Supplier createSupplier(SupplierData data) {
        return suppliers.save(new Supplier(data));
    }

    @Transactional
    public Supplier updateSupplier(Long id, SupplierData data) {
        Supplier supplier = getSupplier(id);
        supplier.update(data);
        return supplier;
    }

    private Specification<Supplier> matches(String search, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (active != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("document")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), term)
                ));
            }
            return predicate;
        };
    }
}
