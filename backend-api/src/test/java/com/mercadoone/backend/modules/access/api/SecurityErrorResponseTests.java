package com.mercadoone.backend.modules.access.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Usa um servidor HTTP de verdade. O MockMvc não repete o despacho de erro do contêiner, então
 * não enxerga o defeito que estes testes cobrem: uma negação de permissão (403) virar 401 e o
 * admin web tratar isso como sessão expirada.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mercado_one_security_errors_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
                "mercado-one.security.seed-admin.enabled=true",
                "mercado-one.security.seed-admin.name=Administrador",
                "mercado-one.security.seed-admin.login=admin",
                "mercado-one.security.seed-admin.password=admin123"
        })
class SecurityErrorResponseTests {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticatedUserWithoutPermissionGets403NotSessionExpired() throws Exception {
        String adminToken = login("admin", "admin123");
        send("POST", "/api/access/users", adminToken, """
                {"nome":"Operador Caixa","login":"operador","password":"senha123","perfil":"OPERADOR_CAIXA","active":true}
                """);
        String operatorToken = login("operador", "senha123");

        HttpResponse<String> response = send("GET", "/api/access/users", operatorToken, null);

        assertEquals(403, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("FORBIDDEN", body.path("error").path("code").asString());
        assertEquals(false, body.path("success").asBoolean());
    }

    @Test
    void requestWithoutTokenGets401WithEnvelope() throws Exception {
        HttpResponse<String> response = send("GET", "/api/catalog/products", null, null);

        assertEquals(401, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("UNAUTHENTICATED", body.path("error").path("code").asString());
    }

    private String login(String login, String password) throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/login", null,
                "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body()).path("data").path("accessToken").asString();
    }

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
