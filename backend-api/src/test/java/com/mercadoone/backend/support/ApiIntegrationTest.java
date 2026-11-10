package com.mercadoone.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para testes de API que compartilham um contexto Spring com H2.
 * Cada teste deve criar dados com identificadores unicos via {@link #unique(String)}.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_shared_api_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin@mercado.one",
        "mercado-one.security.seed-admin.password=admin123",
        "mercado-one.sales.operator-max-discount-percent=10"
})
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected static String unique(String prefix) {
        return prefix + "-" + SEQUENCE.incrementAndGet() + "-" + System.nanoTime() % 100000;
    }

    protected String adminToken() throws Exception {
        return loginToken("admin@mercado.one", "admin123");
    }

    protected JsonNode data(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("data");
    }

    protected ResultActions postJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body));
    }

    protected long createProduct(String token, String name, String sku, String salePrice) throws Exception {
        long categoryId = data(postJson("/api/catalog/categories", token, """
                {"name":"%s","active":true}
                """.formatted(unique("Categoria"))).andExpect(status().isOk())).get("id").asLong();
        return data(postJson("/api/catalog/products", token, """
                {"name":"%s","barcode":null,"sku":"%s","categoryId":%d,"unit":"UN","salePrice":%s,"active":true}
                """.formatted(name, sku, categoryId, salePrice)).andExpect(status().isOk())).get("id").asLong();
    }

    protected void registerEntry(String token, long productId, String quantity) throws Exception {
        postJson("/api/inventory/entries", token, """
                {"productId":%d,"quantity":%s}
                """.formatted(productId, quantity)).andExpect(status().isOk());
    }

    protected long createUser(String token, String login, String role) throws Exception {
        return data(postJson("/api/access/users", token, """
                {"nome":"%s","login":"%s","password":"senha123","perfil":"%s","active":true}
                """.formatted("Usuario " + login, login, role)).andExpect(status().isOk())).get("id").asLong();
    }

    protected String loginToken(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s"}
                                """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("accessToken").asText();
    }
}
