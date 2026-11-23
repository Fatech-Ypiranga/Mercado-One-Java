package com.mercadoone.backend.modules.access.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem MERCADO_ONE_MAIL_HOST/FROM o sistema nao promete e-mail: a tela cai na orientacao do administrador. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_reset_off_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.mail.host=",
        "mercado-one.mail.from="
})
@AutoConfigureMockMvc
class PasswordResetDisabledTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusSaysOffAndRequestsAreRefusedClearly() throws Exception {
        mockMvc.perform(get("/api/auth/password-reset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("RESET_UNAVAILABLE"));
    }
}
