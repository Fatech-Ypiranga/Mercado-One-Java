package com.mercadoone.backend.modules.sales.infrastructure;

import com.mercadoone.backend.modules.sales.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findByClientSaleId(String clientSaleId);
}
