package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.support.ApiIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordAndOriginTests extends ApiIntegrationTest {

    @Test
    void adminResetsPasswordOfAnotherUser() throws Exception {
        String admin = adminToken();
        String login = unique("reset");
        long userId = createUser(admin, login, "ESTOQUISTA");

        postJson("/api/access/users/" + userId + "/password", admin, """
                {"password":"nova-senha-1"}
                """).andExpect(status().isOk());

        loginToken(login, "nova-senha-1");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"senha123"}
                                """.formatted(login)))
                .andExpect(status().isUnauthorized());

        String stock = loginToken(login, "nova-senha-1");
        postJson("/api/access/users/" + userId + "/password", stock, """
                {"password":"outra-senha"}
                """).andExpect(status().isForbidden());
    }

    @Test
    void userChangesOwnPasswordWithCurrentPassword() throws Exception {
        String admin = adminToken();
        String login = unique("self");
        createUser(admin, login, "OPERADOR_CAIXA");
        String token = loginToken(login, "senha123");

        postJson("/api/auth/password", token, """
                {"currentPassword":"errada","newPassword":"trocada-123"}
                """).andExpect(status().isBadRequest());
        postJson("/api/auth/password", token, """
                {"currentPassword":"senha123","newPassword":"123"}
                """).andExpect(status().isBadRequest());
        postJson("/api/auth/password", token, """
                {"currentPassword":"senha123","newPassword":"trocada-123"}
                """).andExpect(status().isOk());

        loginToken(login, "trocada-123");
    }

    @Test
    void adminCannotDeactivateOrDemoteSelf() throws Exception {
        String admin = adminToken();
        long adminId = data(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/auth/me").header("Authorization", "Bearer " + admin))).get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/access/users/" + adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("""
                                {"nome":"Administrador","login":"admin@mercado.one","perfil":"ADMIN","active":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cookieAuthenticatedWriteFromUntrustedOriginIsRejected() throws Exception {
        String admin = adminToken();
        Cookie session = new Cookie("mercado_one_admin_session", admin);

        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.test")
                        .content("""
                                {"name":"%s","active":true}
                                """.formatted(unique("Csrf"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(session)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .content("""
                                {"name":"%s","active":true}
                                """.formatted(unique("Csrf"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + admin)
                        .content("""
                                {"name":"%s","active":true}
                                """.formatted(unique("Bearer"))))
                .andExpect(status().isOk());
    }
}
