package com.mercadoone.backend.migration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe PostgreSQL real, aplica todas as migrations Flyway e valida o mapeamento JPA (ddl-auto=validate).
 * E ignorado automaticamente quando nao ha Docker disponivel.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin",
        "mercado-one.security.seed-admin.password=admin123"
})
@AutoConfigureMockMvc
class PostgresMigrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void migrationsMatchEntitiesAndCoreSaleFlowWorksOnPostgres() throws Exception {
        String token = data(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"admin","password":"admin123"}
                        """))).get("accessToken").asText();

        long categoryId = data(postJson("/api/catalog/categories", token, """
                {"name":"Mercearia PG","active":true}
                """)).get("id").asLong();
        long productId = data(postJson("/api/catalog/products", token, """
                {"name":"Cafe PG","sku":"PG-001","categoryId":%d,"unit":"UN","salePrice":10.00,"active":true}
                """.formatted(categoryId))).get("id").asLong();
        postJson("/api/inventory/entry-documents", token, """
                {"documentNumber":"NF-PG","items":[{"productId":%d,"quantity":5}]}
                """.formatted(productId)).andExpect(status().isOk());

        long saleId = data(postJson("/api/sales", token, """
                {"clientSaleId":"pg-local-1","items":[{"productId":%d,"quantity":2}],"discountAmount":1.00,
                 "payments":[{"method":"PIX","amount":19.00}]}
                """.formatted(productId))
                .andExpect(jsonPath("$.data.totalAmount").value(19.0))).get("id").asLong();

        postJson("/api/sales/" + saleId + "/cancel", token, """
                {"reason":"Teste PostgreSQL"}
                """).andExpect(jsonPath("$.data.status").value("CANCELED"));

        mockMvc.perform(get("/api/audit/events").param("entityType", "sale").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/audit/actions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/offline/sales/status").param("localSaleIds", "pg-local-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));
    }

    private ResultActions postJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body));
    }

    private JsonNode data(ResultActions actions) throws Exception {
        String content = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).get("data");
    }
}
