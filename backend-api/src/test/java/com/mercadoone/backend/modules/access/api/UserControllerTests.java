package com.mercadoone.backend.modules.access.api;

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
        "spring.datasource.url=jdbc:h2:mem:mercado_one_users_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin",
        "mercado-one.security.seed-admin.password=admin123"
})
@AutoConfigureMockMvc
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listsKnownRoles() throws Exception {
        String token = loginToken("admin", "admin123");

        mockMvc.perform(get("/api/access/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].value").value("ADMIN"));
    }

    @Test
    void createsListsAndFiltersUsers() throws Exception {
        String token = loginToken("admin", "admin123");
        createUser(token, "Gerente Loja", "gerente", "GERENTE", true);

        mockMvc.perform(get("/api/access/users")
                        .param("search", "gerente")
                        .param("role", "GERENTE")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nome").value("Gerente Loja"))
                .andExpect(jsonPath("$.data[0].login").value("gerente"))
                .andExpect(jsonPath("$.data[0].perfil").value("GERENTE"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
    }

    @Test
    void updatesUserStatusAndOptionalPassword() throws Exception {
        String token = loginToken("admin", "admin123");
        long userId = createUser(token, "Operador Um", "operador1", "OPERADOR_CAIXA", true);

        mockMvc.perform(put("/api/access/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "nome":"Operador Caixa",
                                  "login":"operador1",
                                  "perfil":"ESTOQUISTA",
                                  "active":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Operador Caixa"))
                .andExpect(jsonPath("$.data.perfil").value("ESTOQUISTA"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void rejectsDuplicateLogin() throws Exception {
        String token = loginToken("admin", "admin123");

        mockMvc.perform(post("/api/access/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "nome":"Administrador duplicado",
                                  "login":"ADMIN",
                                  "password":"admin123",
                                  "perfil":"ADMIN",
                                  "active":true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
    }

    @Test
    void validatesUserFields() throws Exception {
        String token = loginToken("admin", "admin123");

        mockMvc.perform(post("/api/access/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"nome":"","login":"","password":"123","active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsOperatorAccessToUserManagement() throws Exception {
        String adminToken = loginToken("admin", "admin123");
        createUser(adminToken, "Operador Caixa", "operador", "OPERADOR_CAIXA", true);
        String operatorToken = loginToken("operador", "senha123");

        mockMvc.perform(get("/api/access/users")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleTokenUsesCurrentRoleFromDatabase() throws Exception {
        String adminToken = loginToken("admin", "admin123");
        long userId = createUser(adminToken, "Admin Temporario", "admin-temp", "ADMIN", true);
        String staleAdminToken = loginToken("admin-temp", "senha123");

        mockMvc.perform(put("/api/access/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "nome":"Operador Rebaixado",
                                  "login":"admin-temp",
                                  "perfil":"OPERADOR_CAIXA",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/access/users")
                        .header("Authorization", "Bearer " + staleAdminToken))
                .andExpect(status().isForbidden());
    }

    private long createUser(String token, String name, String login, String role, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/access/users")
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
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private String loginToken(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s"}
                                """.formatted(login, password)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("accessToken").asText();
    }
}
