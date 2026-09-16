package com.mercadoone.backend.modules.supplier.api;

import com.mercadoone.backend.api.common.ApiEnvelope;
import com.mercadoone.backend.modules.supplier.application.SupplierService;
import com.mercadoone.backend.modules.supplier.domain.Supplier;
import com.mercadoone.backend.modules.supplier.domain.Supplier.SupplierData;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ApiEnvelope<List<SupplierResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiEnvelope.ok(supplierService.listSuppliers(search, active).stream()
                .map(SupplierResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiEnvelope<SupplierResponse> get(@PathVariable Long id) {
        return ApiEnvelope.ok(SupplierResponse.from(supplierService.getSupplier(id)));
    }

    @PostMapping
    public ApiEnvelope<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiEnvelope.ok(SupplierResponse.from(supplierService.createSupplier(request.toData())));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request
    ) {
        return ApiEnvelope.ok(SupplierResponse.from(supplierService.updateSupplier(id, request.toData())));
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 40) String document,
            @Size(max = 40) String phone,
            @Email @Size(max = 160) String email,
            @Size(max = 500) String notes,
            boolean active
    ) {
        SupplierData toData() {
            return new SupplierData(
                    name.trim(),
                    blankToNull(document),
                    blankToNull(phone),
                    blankToNull(email),
                    blankToNull(notes),
                    active
            );
        }
    }

    public record SupplierResponse(
            Long id,
            String name,
            String document,
            String phone,
            String email,
            String notes,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        static SupplierResponse from(Supplier supplier) {
            return new SupplierResponse(
                    supplier.getId(),
                    supplier.getName(),
                    supplier.getDocument(),
                    supplier.getPhone(),
                    supplier.getEmail(),
                    supplier.getNotes(),
                    supplier.isActive(),
                    supplier.getCreatedAt(),
                    supplier.getUpdatedAt()
            );
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
