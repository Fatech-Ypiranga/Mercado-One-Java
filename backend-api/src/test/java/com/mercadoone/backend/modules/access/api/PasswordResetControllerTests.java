package com.mercadoone.backend.modules.access.api;

import com.mercadoone.backend.modules.access.application.PasswordResetMailer;
import com.mercadoone.backend.modules.access.domain.PasswordResetToken;
import com.mercadoone.backend.modules.access.infrastructure.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mercado_one_reset_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin@mercado.one",
        "mercado-one.security.seed-admin.password=admin123",
        "mercado-one.password-reset.app-url=http://app.teste"
})
@AutoConfigureMockMvc
class PasswordResetControllerTests {

    /** Enviador falso: guarda cada e-mail que seria enviado. */
    record Sent(String to, String name, String link) {
        String token() {
            Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(link);
            assertTrue(matcher.find(), link);
            return matcher.group(1);
        }
    }

    static final List<Sent> OUTBOX = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeMail {
        @Bean
        @Primary
        PasswordResetMailer fakeMailer() {
            return new PasswordResetMailer() {
                @Override
                public boolean enabled() {
                    return true;
                }

                @Override
                public void send(String toEmail, String name, String link, Duration validity) {
                    OUTBOX.add(new Sent(toEmail, name, link));
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository tokens;

    @Autowired
    private com.mercadoone.backend.modules.access.infrastructure.AppUserRepository users;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        OUTBOX.clear();
        adminToken = login("admin@mercado.one", "admin123");
    }

    @Test
    void statusSaysTheFeatureIsOnWhenAMailerIsConfigured() throws Exception {
        mockMvc.perform(get("/api/auth/password-reset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void resetByLoginSendsALinkAndTheNewPasswordWorksOnlyOnce() throws Exception {
        String login = "ana.reset";
        createUser(login, "ana.reset@mercado.test", "Ana Reset", true);

        requestReset(login);
        Sent mail = awaitMail(1).getFirst();

        assertEquals("ana.reset@mercado.test", mail.to());
        assertTrue(mail.link().startsWith("http://app.teste/nova-senha?token="), mail.link());
        mockMvc.perform(post("/api/auth/password-reset/check").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", mail.token())))
                .andExpect(jsonPath("$.data.valid").value(true));

        confirm(mail.token(), "nova-senha-9").andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"nova-senha-9\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"senha-inicial-1\"}"))
                .andExpect(status().isUnauthorized());
        // O link e de uso unico.
        confirm(mail.token(), "outra-senha-8")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESET_LINK_INVALID"));
    }

    @Test
    void resetByEmailWorksAndIsCaseInsensitive() throws Exception {
        createUser("bia.reset", "Bia.Reset@Mercado.Test", "Bia", true);

        requestReset("BIA.RESET@mercado.test");

        assertEquals("bia.reset@mercado.test", awaitMail(1).getFirst().to());
    }

    @Test
    void unknownInactiveAndEmaillessAccountsGetTheSameAnswerAndNoMail() throws Exception {
        createUser("sem.email", null, "Sem Email", true);
        createUser("inativo.reset", "inativo.reset@mercado.test", "Inativo", false);

        String known = requestReset("admin@mercado.one");
        String unknown = requestReset("ninguem.assim");
        String noEmail = requestReset("sem.email");
        String inactive = requestReset("inativo.reset");

        assertEquals(known, unknown);
        assertEquals(known, noEmail);
        assertEquals(known, inactive);
        Thread.sleep(400);
        assertEquals(0, OUTBOX.size());
    }

    @Test
    void aNewRequestInvalidatesTheOlderLink() throws Exception {
        createUser("caio.reset", "caio.reset@mercado.test", "Caio", true);

        requestReset("caio.reset");
        Sent first = awaitMail(1).getFirst();
        requestReset("caio.reset");
        Sent second = awaitMail(2).get(1);

        confirm(first.token(), "senha-nova-77").andExpect(status().isBadRequest());
        confirm(second.token(), "senha-nova-77").andExpect(status().isOk());
    }

    @Test
    void atMostThreeRequestsPerHourPerUser() throws Exception {
        createUser("duda.reset", "duda.reset@mercado.test", "Duda", true);

        for (int i = 0; i < 5; i++) {
            requestReset("duda.reset");
        }
        Thread.sleep(600);

        assertEquals(3, OUTBOX.size());
    }

    @Test
    void aTooShortPasswordDoesNotBurnTheLink() throws Exception {
        createUser("edu.reset", "edu.reset@mercado.test", "Edu", true);
        requestReset("edu.reset");
        Sent mail = awaitMail(1).getFirst();

        confirm(mail.token(), "123").andExpect(status().isBadRequest());

        confirm(mail.token(), "senha-boa-123").andExpect(status().isOk());
    }

    @Test
    void anExpiredLinkIsRefusedAndReportedAsInvalid() throws Exception {
        long userId = createUser("fabi.reset", "fabi.reset@mercado.test", "Fabi", true);
        String raw = "token-vencido-de-teste-0123456789";
        Instant past = Instant.now().minus(Duration.ofHours(3));
        tokens.save(new PasswordResetToken(userId, sha256(raw), past.plus(Duration.ofHours(1)), past));

        mockMvc.perform(post("/api/auth/password-reset/check").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", raw)))
                .andExpect(jsonPath("$.data.valid").value(false));
        confirm(raw, "senha-nova-55")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESET_LINK_INVALID"));
    }

    @Test
    void anInventedTokenIsRefused() throws Exception {
        confirm("nao-existe-nenhum-token-assim", "senha-nova-55")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESET_LINK_INVALID"));
    }

    @Test
    void theDatabaseKeepsOnlyTheTokenHash() throws Exception {
        long userId = createUser("gabi.reset", "gabi.reset@mercado.test", "Gabi", true);
        requestReset("gabi.reset");
        Sent mail = awaitMail(1).getFirst();

        List<PasswordResetToken> stored = tokens.findAll().stream().filter(t -> t.getUserId().equals(userId)).toList();

        assertEquals(1, stored.size());
        assertTrue(tokens.findByTokenHash(mail.token()).isEmpty(), "o token puro nao pode estar gravado");
        assertTrue(tokens.findByTokenHash(sha256(mail.token())).isPresent());
    }

    @Test
    void twoUsersCannotShareAnEmail() throws Exception {
        createUser("hugo.reset", "mesmo@mercado.test", "Hugo", true);

        mockMvc.perform(post("/api/access/users").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(userBody("iris.reset", "MESMO@mercado.test", "Iris", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("E-mail ja esta em uso por outro usuario."));
    }

    @Test
    void anInvalidEmailIsRejectedWhenCreatingAUser() throws Exception {
        mockMvc.perform(post("/api/access/users").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(userBody("joao.reset", "isto-nao-e-email", "Joao", true)))
                .andExpect(status().isBadRequest());
    }

    // ---- ajudantes ----------------------------------------------------------

    private String requestReset(String identifier) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content(json("identifier", identifier)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
    }

    private List<Sent> awaitMail(int count) throws Exception {
        for (int i = 0; i < 100 && OUTBOX.size() < count; i++) {
            Thread.sleep(50);
        }
        assertTrue(OUTBOX.size() >= count, "e-mail nao chegou, caixa de saida: " + OUTBOX.size());
        return List.copyOf(OUTBOX);
    }

    private long createUser(String login, String email, String name, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/access/users").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(userBody(login, email, name, active)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private static String userBody(String login, String email, String name, boolean active) {
        return "{\"nome\":\"" + name + "\",\"login\":\"" + login + "\",\"password\":\"senha-inicial-1\","
                + "\"perfil\":\"OPERADOR_CAIXA\",\"active\":" + active
                + (email == null ? "" : ",\"email\":\"" + email + "\"") + "}";
    }

    private String login(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("accessToken").asText();
    }

    private static String json(String key, String value) {
        return "{\"" + key + "\":\"" + value + "\"}";
    }

    private static String sha256(String raw) throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
