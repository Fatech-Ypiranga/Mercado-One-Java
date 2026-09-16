package com.mercadoone.backend.modules.customer.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.customer.application.CustomerService;
import com.mercadoone.backend.modules.customer.domain.Customer;
import com.mercadoone.backend.modules.customer.domain.Customer.CustomerData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiEnvelope<List<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(customerService.listCustomers(search, active).stream()
                .map(CustomerResponse::from)
                .toList());
    }

    @GetMapping("/search")
    public ApiEnvelope<List<CustomerSearchResponse>> search(@RequestParam(required = false) String search) {
        return ApiEnvelope.ok(customerService.listCustomers(search, true).stream()
                .map(CustomerSearchResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiEnvelope<CustomerResponse> get(@PathVariable Long id) {
        return ApiEnvelope.ok(CustomerResponse.from(customerService.getCustomer(id)));
    }

    @PostMapping
    public ApiEnvelope<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ApiEnvelope.ok(CustomerResponse.from(customerService.createCustomer(request.toData())));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        return ApiEnvelope.ok(CustomerResponse.from(customerService.updateCustomer(id, request.toData())));
    }

    public record CustomerRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 40) String phone,
            @Email @Size(max = 160) String email,
            @Size(max = 40) String document,
            boolean contactConsent,
            boolean active
    ) {
        CustomerData toData() {
            return new CustomerData(
                    name.trim(),
                    blankToNull(phone),
                    blankToNull(email),
                    blankToNull(document),
                    contactConsent,
                    active
            );
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public record CustomerResponse(
            Long id,
            String name,
            String phone,
            String email,
            String document,
            boolean contactConsent,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getPhone(),
                    customer.getEmail(),
                    customer.getDocument(),
                    customer.isContactConsent(),
                    customer.isActive(),
                    customer.getCreatedAt(),
                    customer.getUpdatedAt()
            );
        }
    }

    public record CustomerSearchResponse(Long id, String name, String phone, String document) {
        static CustomerSearchResponse from(Customer customer) {
            return new CustomerSearchResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getPhone(),
                    customer.getDocument()
            );
        }
    }
}
