package com.mercadoone.backend.modules.access.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_remote_admin_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin@mercado.one",
        "mercado-one.security.seed-admin.password=admin123",
        "mercado-one.security.cookie-secure=true",
        "mercado-one.security.cors-allowed-origins=https://admin.example.test"
})
@AutoConfigureMockMvc
class RemoteAdminSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void secureSessionCookieIsAlsoClearedOnLogout() throws Exception {
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin@mercado.one","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        allOf(containsString("Secure"), containsString("SameSite=None"))))
                .andReturn().getResponse().getCookie("mercado_one_admin_session");

        org.assertj.core.api.Assertions.assertThat(login).isNotNull();
        org.assertj.core.api.Assertions.assertThat(login.getSecure()).isTrue();

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://admin.example.test")
                        .cookie(new Cookie(login.getName(), login.getValue())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        allOf(containsString("Max-Age=0"), containsString("Secure"), containsString("SameSite=None"))));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://other.example.test")
                        .cookie(new Cookie(login.getName(), login.getValue())))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void allowsOnlyConfiguredRemoteAdminOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://admin.example.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.example.test"));

        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
