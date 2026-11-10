package com.mercadoone.backend.modules.sales.api;

import com.mercadoone.backend.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaleLifecycleTests extends ApiIntegrationTest {

    @Test
    void managerAppliesDiscountAndTotalsUseNetAmount() throws Exception {
        String admin = adminToken();
        String sku = unique("DESC");
        long productId = createProduct(admin, "Produto Desconto", sku, "20.00");
        registerEntry(admin, productId, "10");

        postJson("/api/sales", admin, """
                {"items":[{"productId":%d,"quantity":2}],
                 "discountAmount":5.00,
                 "payments":[{"method":"CASH","amount":20.00},{"method":"PIX","amount":15.00}]}
                """.formatted(productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grossAmount").value(40.0))
                .andExpect(jsonPath("$.data.discountAmount").value(5.0))
                .andExpect(jsonPath("$.data.totalAmount").value(35.0))
                .andExpect(jsonPath("$.data.payments", hasSize(2)));
    }

    @Test
    void operatorDiscountIsLimitedByConfiguredPercent() throws Exception {
        String admin = adminToken();
        String login = unique("op-desc");
        createUser(admin, login, "OPERADOR_CAIXA");
        String operator = loginToken(login, "senha123");
        long productId = createProduct(admin, "Produto Limite", unique("LIM"), "100.00");
        registerEntry(admin, productId, "10");

        mockMvc.perform(get("/api/sales/discount-policy").header("Authorization", "Bearer " + operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxDiscountPercent").value(10));

        postJson("/api/sales", operator, """
                {"items":[{"productId":%d,"quantity":1}],"discountAmount":15.00,
                 "payments":[{"method":"CASH","amount":85.00}]}
                """.formatted(productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));

        postJson("/api/sales", operator, """
                {"items":[{"productId":%d,"quantity":1}],"discountAmount":10.00,
                 "payments":[{"method":"CASH","amount":90.00}]}
                """.formatted(productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(90.0));
    }

    @Test
    void clientSaleIdMakesFinalizationIdempotent() throws Exception {
        String admin = adminToken();
        long productId = createProduct(admin, "Produto Idempotente", unique("IDEM"), "3.00");
        registerEntry(admin, productId, "5");
        String clientSaleId = unique("pdv-local");
        String body = """
                {"clientSaleId":"%s","items":[{"productId":%d,"quantity":1}],
                 "payments":[{"method":"CASH","amount":3.00}]}
                """.formatted(clientSaleId, productId);

        long firstId = data(postJson("/api/sales", admin, body).andExpect(status().isOk())).get("id").asLong();
        long secondId = data(postJson("/api/sales", admin, body).andExpect(status().isOk())).get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);

        mockMvc.perform(get("/api/inventory/balances").param("search", "IDEM").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data[?(@.product.id == %d)].quantity".formatted(productId)).value(4.0));
    }

    @Test
    void managerCancelsSaleReturningStockAndExcludingFromTotals() throws Exception {
        String admin = adminToken();
        String sku = unique("CANC");
        long productId = createProduct(admin, "Produto Cancelamento", sku, "4.00");
        registerEntry(admin, productId, "5");
        String customerLogin = unique("op-cancel");
        createUser(admin, customerLogin, "OPERADOR_CAIXA");
        String operator = loginToken(customerLogin, "senha123");

        long saleId = data(postJson("/api/sales", operator, """
                {"items":[{"productId":%d,"quantity":2}],"payments":[{"method":"CARD","amount":8.00}]}
                """.formatted(productId)).andExpect(status().isOk())).get("id").asLong();

        postJson("/api/sales/" + saleId + "/cancel", operator, """
                {"reason":"Cliente desistiu"}
                """).andExpect(status().isForbidden());

        postJson("/api/sales/" + saleId + "/cancel", admin, """
                {"reason":" "}
                """).andExpect(status().isBadRequest());

        postJson("/api/sales/" + saleId + "/cancel", admin, """
                {"reason":"Cliente desistiu"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("Cliente desistiu"))
                .andExpect(jsonPath("$.data.canceledAt").exists());

        postJson("/api/sales/" + saleId + "/cancel", admin, """
                {"reason":"De novo"}
                """).andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/inventory/movements")
                        .param("productId", Long.toString(productId))
                        .param("type", "SALE_CANCELLATION")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].quantityDelta").value(2.0))
                .andExpect(jsonPath("$.data[0].quantityAfter").value(5.0));

        mockMvc.perform(get("/api/sales/mine").header("Authorization", "Bearer " + operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.totals.saleCount").value(0))
                .andExpect(jsonPath("$.data.totals.canceledCount").value(1))
                .andExpect(jsonPath("$.data.totals.totalAmount").value(0));

        mockMvc.perform(get("/api/sales")
                        .param("status", "CANCELED")
                        .param("operatorUserId", data(mockMvc.perform(get("/api/auth/me")
                                .header("Authorization", "Bearer " + operator))).get("id").asText())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    void operatorSeesOnlyOwnSales() throws Exception {
        String admin = adminToken();
        long productId = createProduct(admin, "Produto Meu Caixa", unique("MINE"), "2.00");
        registerEntry(admin, productId, "10");
        String loginA = unique("op-a");
        String loginB = unique("op-b");
        createUser(admin, loginA, "OPERADOR_CAIXA");
        createUser(admin, loginB, "OPERADOR_CAIXA");
        String operatorA = loginToken(loginA, "senha123");
        String operatorB = loginToken(loginB, "senha123");
        String sale = """
                {"items":[{"productId":%d,"quantity":1}],"payments":[{"method":"CASH","amount":2.00}]}
                """.formatted(productId);
        postJson("/api/sales", operatorA, sale).andExpect(status().isOk());
        postJson("/api/sales", operatorA, sale).andExpect(status().isOk());
        postJson("/api/sales", operatorB, sale).andExpect(status().isOk());

        mockMvc.perform(get("/api/sales/mine").header("Authorization", "Bearer " + operatorA))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.totals.totalAmount").value(4.0));
    }

    @Test
    void operatorCanQuickRegisterCustomer() throws Exception {
        String admin = adminToken();
        String login = unique("op-cli");
        createUser(admin, login, "OPERADOR_CAIXA");
        String operator = loginToken(login, "senha123");

        mockMvc.perform(post("/api/customers/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + operator)
                        .content("""
                                {"name":"Cliente Balcao","phone":"11999990000","contactConsent":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Cliente Balcao"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + operator)
                        .content("""
                                {"name":"Cliente Completo","contactConsent":false,"active":true}
                                """))
                .andExpect(status().isForbidden());
    }
}
