package com.mercadoone.backend.modules.customer.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_customer_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsUpdatesAndFiltersCustomers() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        long customerId = createCustomer(adminToken, "Maria Silva", "11999990000", "maria@example.com", true);
        createCustomer(adminToken, "Joao Souza", "11888880000", "joao@example.com", false);

        mockMvc.perform(get("/api/customers")
                        .param("search", "maria")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(customerId));

        mockMvc.perform(put("/api/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "name":"Maria Silva Prime",
                                  "phone":"11999990000",
                                  "email":"maria.prime@example.com",
                                  "document":"12345678900",
                                  "contactConsent":false,
                                  "active":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maria Silva Prime"))
                .andExpect(jsonPath("$.data.contactConsent").value(false))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void operatorCanUseSafeSearchButCannotListOrCreateCustomers() throws Exception {
        String adminToken = loginToken("admin@mercado.one", "admin123");
        createUser(adminToken, "Operador Caixa Cliente", "operador-cliente", "OPERADOR_CAIXA", true);
        String operatorToken = loginToken("operador-cliente", "senha123");
        createCustomer(adminToken, "Cliente Balcao", "11777770000", "balcao@example.com", true);

        mockMvc.perform(get("/api/customers/search")
                        .param("search", "balcao")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.data[0].contactConsent").doesNotExist());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + operatorToken)
                        .content("""
                                {"name":"Sem permissao","contactConsent":false,"active":true}
                                """))
                .andExpect(status().isForbidden());
    }

    private long createCustomer(String token, String name, String phone, String email, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"%s",
                                  "phone":"%s",
                                  "email":"%s",
                                  "document":null,
                                  "contactConsent":true,
                                  "active":%s
                                }
                                """.formatted(name, phone, email, active)))
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
