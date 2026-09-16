package com.mercadoone.backend.modules.customer.application;

import com.mercadoone.backend.modules.customer.domain.Customer;
import com.mercadoone.backend.modules.customer.domain.Customer.CustomerData;
import com.mercadoone.backend.modules.customer.infrastructure.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers(String search, Boolean active) {
        return customers.findAll(matches(search, active), Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Customer getActiveCustomer(Long id) {
        Customer customer = getCustomer(id);
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Cliente inativo nao pode ser vinculado a venda.");
        }
        return customer;
    }

    @Transactional
    public Customer createCustomer(CustomerData data) {
        return customers.save(new Customer(data));
    }

    @Transactional
    public Customer updateCustomer(Long id, CustomerData data) {
        Customer customer = getCustomer(id);
        customer.update(data);
        return customer;
    }

    private Specification<Customer> matches(String search, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (active != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("document")), term)
                ));
            }
            return predicate;
        };
    }
}
