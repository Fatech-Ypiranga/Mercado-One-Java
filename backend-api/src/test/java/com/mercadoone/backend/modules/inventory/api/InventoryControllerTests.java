package com.mercadoone.backend.modules.inventory.api;

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
        "spring.datasource.url=jdbc:h2:mem:mercado_one_inventory_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersEntryAndListsBalance() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Cafe Estoque", "EST-001", true);

        mockMvc.perform(post("/api/inventory/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "productId":%d,
                                  "quantity":12.500,
                                  "supplierName":"Fornecedor Teste",
                                  "documentNumber":"NF-001",
                                  "note":"Entrada inicial"
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("ENTRY"))
                .andExpect(jsonPath("$.data.quantityBefore").value(0))
                .andExpect(jsonPath("$.data.quantityAfter").value(12.5))
                .andExpect(jsonPath("$.data.supplierName").value("Fornecedor Teste"));

        mockMvc.perform(get("/api/inventory/balances")
                        .param("search", "EST-001")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].product.id").value(productId))
                .andExpect(jsonPath("$.data[0].quantity").value(12.5));
    }

    @Test
    void registersAdjustmentAndKeepsMovementsImmutable() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Arroz Estoque", "EST-002", true);
        registerEntry(token, productId, "9.000");

        mockMvc.perform(post("/api/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "productId":%d,
                                  "newQuantity":7.000,
                                  "reason":"Conferencia fisica",
                                  "note":"Divergencia no saldo"
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.data.quantityDelta").value(-2))
                .andExpect(jsonPath("$.data.quantityBefore").value(9))
                .andExpect(jsonPath("$.data.quantityAfter").value(7))
                .andExpect(jsonPath("$.data.reason").value("Conferencia fisica"));

        mockMvc.perform(get("/api/inventory/movements")
                        .param("productId", Long.toString(productId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/inventory/movements")
                        .param("productId", Long.toString(productId))
                        .param("type", "ADJUSTMENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].quantityAfter").value(7));
    }

    @Test
    void rejectsAdjustmentWithoutReason() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Feijao Estoque", "EST-003", true);

        mockMvc.perform(post("/api/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"productId":%d,"newQuantity":3.000,"reason":""}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInactiveProductMovement() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Produto Inativo", "EST-004", false);

        mockMvc.perform(post("/api/inventory/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"productId":%d,"quantity":1.000}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
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

    private long createProduct(String token, String name, String sku, boolean active) throws Exception {
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
                                  "salePrice":10.00,
                                  "active":%s
                                }
                                """.formatted(name, sku, categoryId, active)))
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
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin@mercado.one","password":"admin123"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("accessToken").asText();
    }
}
