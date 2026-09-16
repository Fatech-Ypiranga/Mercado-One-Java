package com.mercadoone.backend.modules.offline.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_offline_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class OfflineSalesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void syncsValidOfflineSaleAndReturnsConflictForChangedPrice() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Offline Cafe", "OFF-001", true, "8.00");
        registerEntry(token, productId, "3.000");

        mockMvc.perform(post("/api/offline/sales/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(syncBody("local-ok", productId, "1.000", "8.00", "8.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.sale.id").exists());

        mockMvc.perform(post("/api/offline/sales/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(syncBody("local-conflict", productId, "1.000", "7.00", "7.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFLICT"))
                .andExpect(jsonPath("$.data.conflicts[0].code").value("PRICE_CHANGED"));

        String conflictsResponse = mockMvc.perform(get("/api/offline/sales/conflicts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long conflictId = pendingConflictByLocalSale(conflictsResponse, "local-conflict").get("id").asLong();

        mockMvc.perform(post("/api/offline/sales/conflicts/%d/resolve".formatted(conflictId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"action":"ACCEPT","note":"Preco praticado no PDV validado."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.remoteSaleId").exists());
    }

    @Test
    void rejectsOfflineConflictWithRequiredNote() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Offline Arroz", "OFF-002", true, "10.00");
        registerEntry(token, productId, "3.000");

        mockMvc.perform(post("/api/offline/sales/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(syncBody("local-reject", productId, "1.000", "9.00", "9.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFLICT"));

        String conflictsResponse = mockMvc.perform(get("/api/offline/sales/conflicts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long conflictId = pendingConflictByLocalSale(conflictsResponse, "local-reject").get("id").asLong();

        mockMvc.perform(post("/api/offline/sales/conflicts/%d/resolve".formatted(conflictId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"action":"REJECT","note":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));

        mockMvc.perform(post("/api/offline/sales/conflicts/%d/resolve".formatted(conflictId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"action":"REJECT","note":"Venda duplicada no PDV."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.resolutionNote").value("Venda duplicada no PDV."));

        mockMvc.perform(get("/api/offline/sales/conflicts")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.localSaleId == 'local-reject')].status").value("REJECTED"));

        mockMvc.perform(post("/api/offline/sales/conflicts/%d/resolve".formatted(conflictId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"action":"ACCEPT","note":"Tentativa posterior."}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
    }

    @Test
    void refreshesExistingPendingConflictForSameLocalSale() throws Exception {
        String token = loginToken();
        long productId = createProduct(token, "Offline Feijao", "OFF-003", true, "6.00");
        registerEntry(token, productId, "3.000");

        mockMvc.perform(post("/api/offline/sales/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(syncBody("local-duplicate", productId, "1.000", "5.00", "5.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFLICT"));

        mockMvc.perform(post("/api/offline/sales/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(syncBody("local-duplicate", productId, "2.000", "5.00", "10.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFLICT"));

        String conflictsResponse = mockMvc.perform(get("/api/offline/sales/conflicts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode matching = pendingConflictByLocalSale(conflictsResponse, "local-duplicate");
        int count = 0;
        for (JsonNode conflict : objectMapper.readTree(conflictsResponse).get("data")) {
            if ("local-duplicate".equals(conflict.get("localSaleId").asText())) {
                count++;
            }
        }
        JsonNode salePayload = objectMapper.readTree(matching.get("salePayload").asText());

        org.junit.jupiter.api.Assertions.assertEquals(1, count);
        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                new java.math.BigDecimal("2.000").compareTo(salePayload.get("items").get(0).get("quantity").decimalValue())
        );
    }

    private String syncBody(String localSaleId, long productId, String quantity, String unitPrice, String amount) {
        return """
                {
                  "localSaleId":"%s",
                  "createdAt":"%s",
                  "items":[{"productId":%d,"quantity":%s,"unitPrice":%s}],
                  "payments":[{"method":"CASH","amount":%s}]
                }
                """.formatted(localSaleId, Instant.now(), productId, quantity, unitPrice, amount);
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
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
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
        return objectMapper.readTree(response).get("data").get("id").asLong();
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

    private JsonNode pendingConflictByLocalSale(String response, String localSaleId) throws Exception {
        for (JsonNode conflict : objectMapper.readTree(response).get("data")) {
            if (localSaleId.equals(conflict.get("localSaleId").asText())) {
                return conflict;
            }
        }
        throw new AssertionError("Conflito offline nao encontrado: " + localSaleId);
    }
}
