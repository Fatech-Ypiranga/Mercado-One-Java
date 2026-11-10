package com.mercadoone.backend.modules.sales.application;

import com.mercadoone.backend.modules.access.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de desconto simples por perfil.
 * ADMIN e GERENTE podem conceder qualquer desconto menor que o total bruto.
 * OPERADOR_CAIXA fica limitado ao percentual configurado (padrao 0, ou seja, sem desconto).
 */
@Component
public class DiscountPolicy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal operatorMaxPercent;

    DiscountPolicy(@Value("${mercado-one.sales.operator-max-discount-percent:0}") BigDecimal operatorMaxPercent) {
        this.operatorMaxPercent = operatorMaxPercent.max(BigDecimal.ZERO).min(HUNDRED);
    }

    /**
     * Percentual maximo de desconto para o perfil, ou {@code null} quando nao ha limite alem do total bruto.
     */
    public BigDecimal maxPercentFor(UserRole role) {
        return switch (role) {
            case ADMIN, GERENTE -> null;
            case OPERADOR_CAIXA -> operatorMaxPercent;
            case ESTOQUISTA -> BigDecimal.ZERO;
        };
    }

    public boolean allows(UserRole role, BigDecimal discount, BigDecimal grossAmount) {
        if (discount == null || discount.signum() == 0) {
            return true;
        }
        BigDecimal maxPercent = maxPercentFor(role);
        if (maxPercent == null) {
            return true;
        }
        BigDecimal limit = grossAmount.multiply(maxPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return discount.compareTo(limit) <= 0;
    }
}
