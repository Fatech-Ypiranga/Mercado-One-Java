package com.mercadoone.backend.modules.sales.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_sales_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin@mercado.one",
        "mercado-one.security.seed-admin.password=admin123"
})
@AutoConfigureMockMvc
class SalesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void operatorFinalizesOnlineSaleAndStockIsReduced() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        createUser(adminToken, "Operador Caixa", "operador", "OPERADOR_CAIXA", true);
        String operatorToken = loginToken("operador", "senha123");
        long productId = createProduct(adminToken, "Cafe Venda", "SALE-001", true, "7.50");
        long customerId = createCustomer(adminToken, "Cliente Venda", "11911112222");
        registerEntry(adminToken, productId, "5.000");

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + operatorToken)
                        .content("""
                                {
                                  "customerId":%d,
                                  "items":[{"productId":%d,"quantity":2.000}],
                                  "payments":[{"method":"CASH","amount":15.00}]
                                }
                                """.formatted(customerId, productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.operatorUserId").exists())
                .andExpect(jsonPath("$.data.customer.id").value(customerId))
                .andExpect(jsonPath("$.data.totalAmount").value(15.0))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.payments[0].method").value("CASH"));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("search", "SALE-001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].quantity").value(3.0));

        mockMvc.perform(get("/api/inventory/movements")
                        .param("productId", Long.toString(productId))
                        .param("type", "SALE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].quantityDelta").value(-2.0));

        mockMvc.perform(get("/api/sales")
                        .param("customerId", Long.toString(customerId))
                        .param("size", "20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].customer.name").value("Cliente Venda"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.totals.saleCount").value(1))
                .andExpect(jsonPath("$.data.totals.totalAmount").value(15.0))
                .andExpect(jsonPath("$.data.totals.totalsByPaymentMethod.CASH").value(15.0));

        mockMvc.perform(get("/api/sales/export.csv")
                        .param("customerId", Long.toString(customerId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .contains("id,createdAt,status,operatorUserId,customer,totalAmount,payments,items")
                        .contains("Cliente Venda"));
    }

    @Test
    void rejectsSaleWhenStockIsInsufficient() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        long productId = createProduct(adminToken, "Arroz Venda", "SALE-002", true, "12.00");

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "items":[{"productId":%d,"quantity":1.000}],
                                  "payments":[{"method":"PIX","amount":12.00}]
                                }
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
    }

    @Test
    void operatorCannotListSalesReport() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        createUser(adminToken, "Operador Relatorio", "operador-relatorio", "OPERADOR_CAIXA", true);
        String operatorToken = loginToken("operador-relatorio", "senha123");

        mockMvc.perform(get("/api/sales")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void roundsFractionalSaleTotalsToCurrencyScale() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        long productId = createProduct(adminToken, "Queijo Fracionado", "SALE-003", true, "9.99");
        registerEntry(adminToken, productId, "1.000");

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "items":[{"productId":%d,"quantity":0.250}],
                                  "payments":[{"method":"CARD","amount":2.50}]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(2.50))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(2.50));
    }

    @Test
    void listsTopSoldProducts() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        long coffeeId = createProduct(adminToken, "Cafe Ranking", "SALE-004", true, "5.00");
        long riceId = createProduct(adminToken, "Arroz Ranking", "SALE-005", true, "10.00");
        registerEntry(adminToken, coffeeId, "10.000");
        registerEntry(adminToken, riceId, "10.000");

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "items":[{"productId":%d,"quantity":3.000},{"productId":%d,"quantity":1.000}],
                                  "payments":[{"method":"PIX","amount":25.00}]
                                }
                                """.formatted(coffeeId, riceId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sales/top-products")
                        .param("limit", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value(coffeeId))
                .andExpect(jsonPath("$.data[0].quantity").value(3.0))
                .andExpect(jsonPath("$.data[0].totalAmount").value(15.0))
                .andExpect(jsonPath("$.data[1].productId").value(riceId));
    }

    private void registerEntry(String token, long productId, String quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"productId":%d,"quantity":%s}
                                """.formatted(productId, quantity)))
                .andExpect(status().isOk());
    }

    private long createProduct(String token, String name, String sku, boolean active, String salePrice) throws Exception {
        long categoryId = createCategory(token, "Categoria " + sku, true);
        String response = mockMvc.perform(post("/api/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"%s",
                                  "barcode":null,
                                  "sku":"%s",
                                  "categoryId":%d,
                                  "unit":"UN",
                                  "salePrice":%s,
                                  "active":%s
                                }
                                """.formatted(name, sku, categoryId, salePrice, active)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private long createCustomer(String token, String name, String phone) throws Exception {
        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"%s",
                                  "phone":"%s",
                                  "email":null,
                                  "document":null,
                                  "contactConsent":true,
                                  "active":true
                                }
                                """.formatted(name, phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private long createCategory(String token, String name, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"%s","active":%s}
                                """.formatted(name, active)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private void createUser(String token, String name, String login, String role, boolean active) throws Exception {
        mockMvc.perform(post("/api/access/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "nome":"%s",
                                  "login":"%s",
                                  "password":"senha123",
                                  "perfil":"%s",
                                  "active":%s
                                }
                                """.formatted(name, login, role, active)))
                .andExpect(status().isOk());
    }

    private String loginToken(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s"}
                                """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("accessToken").asText();
    }
}
