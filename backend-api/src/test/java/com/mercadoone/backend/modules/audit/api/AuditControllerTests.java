package com.mercadoone.backend.modules.audit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mercadoone.backend.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditControllerTests extends ApiIntegrationTest {

    @Test
    void productCreationAndUpdateAreAuditedAndQueryable() throws Exception {
        String admin = adminToken();
        String sku = unique("AUD");
        long productId = createProduct(admin, "Produto Auditado", sku, "7.00");
        JsonNode product = data(mockMvc.perform(get("/api/catalog/products").param("search", sku)
                .header("Authorization", "Bearer " + admin))).get(0);

        mockMvc.perform(put("/api/catalog/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("""
                                {"name":"Produto Auditado","sku":"%s","categoryId":%d,"unit":"UN","salePrice":8.50,"active":true}
                                """.formatted(sku, product.get("category").get("id").asLong())))
                .andExpect(status().isOk());

        JsonNode page = data(mockMvc.perform(get("/api/audit/events")
                        .param("entityType", "catalog_product")
                        .param("entityId", Long.toString(productId))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()));

        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        JsonNode update = page.get("items").get(0);
        assertThat(update.get("action").asText()).isEqualTo("PRODUCT_UPDATED");
        assertThat(update.get("actorUserId").isNull()).isFalse();
        JsonNode summary = objectMapper.readTree(update.get("summaryJson").asText());
        assertThat(summary.get("changes").has("salePrice")).isTrue();
        assertThat(summary.get("changes").has("name")).isFalse();
        assertThat(page.get("items").get(1).get("action").asText()).isEqualTo("PRODUCT_CREATED");

        mockMvc.perform(get("/api/audit/actions").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasItem("PRODUCT_CREATED")));
    }

    @Test
    void auditIsRestrictedToAdmin() throws Exception {
        String admin = adminToken();
        String login = unique("gerente-aud");
        createUser(admin, login, "GERENTE");
        String manager = loginToken(login, "senha123");

        mockMvc.perform(get("/api/audit/events").header("Authorization", "Bearer " + manager))
                .andExpect(status().isForbidden());
    }

    @Test
    void entryDocumentRegistersAllItemsAtomically() throws Exception {
        String admin = adminToken();
        long first = createProduct(admin, "Doc Item 1", unique("DOC"), "1.00");
        long second = createProduct(admin, "Doc Item 2", unique("DOC"), "1.00");
        String documentNumber = unique("NF");

        postJson("/api/inventory/entry-documents", admin, """
                {"documentNumber":"%s","supplierName":"Distribuidora","items":[
                  {"productId":%d,"quantity":3},{"productId":%d,"quantity":4.5}]}
                """.formatted(documentNumber, first, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].documentNumber").value(documentNumber))
                .andExpect(jsonPath("$.data[1].quantityAfter").value(4.5));

        postJson("/api/inventory/entry-documents", admin, """
                {"documentNumber":"X","items":[{"productId":%d,"quantity":1},{"productId":999999,"quantity":1}]}
                """.formatted(first))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/inventory/movements").param("productId", Long.toString(first))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.length()").value(1));

        postJson("/api/inventory/entry-documents", admin, """
                {"items":[{"productId":%d,"quantity":1},{"productId":%d,"quantity":2}]}
                """.formatted(first, first))
                .andExpect(status().isBadRequest());
    }
}
