package com.mercadoone.pdv.sales;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class OnlineSaleClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Transport transport;

    public OnlineSaleClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.transport = request -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return new ApiResponse(response.statusCode(), response.body());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Operacao interrompida antes da resposta da API.", exception);
            } catch (IOException exception) {
                throw new IllegalStateException("Nao foi possivel comunicar com a API.", exception);
            }
        };
    }

    OnlineSaleClient(Transport transport) {
        this.transport = transport;
    }

    public String login(String apiBaseUrl, String login, String password) {
        ApiResponse response = sendJson(
                apiBaseUrl,
                "/api/auth/login",
                null,
                toJson(Map.of("login", login == null ? "" : login, "password", password == null ? "" : password))
        );
        String token = data(response.body()).path("accessToken").asText("");
        if (token.isBlank()) {
            throw new IllegalStateException("Login concluido sem token de acesso.");
        }
        return token;
    }

    public List<CatalogProduct> searchProducts(String apiBaseUrl, String bearerToken, String search) {
        ApiResponse response = sendGet(
                apiBaseUrl,
                "/api/catalog/products?active=true&search=" + encode(search),
                bearerToken
        );
        return StreamSupport.stream(data(response.body()).spliterator(), false)
                .map(product -> new CatalogProduct(
                        product.path("id").asLong(),
                        product.path("name").asText(),
                        textOrNull(product.path("barcode")),
                        textOrNull(product.path("sku")),
                        product.path("unit").asText(),
                        product.path("salePrice").decimalValue()
                ))
                .toList();
    }

    public List<CustomerOption> searchCustomers(String apiBaseUrl, String bearerToken, String search) {
        ApiResponse response = sendGet(
                apiBaseUrl,
                "/api/customers/search?search=" + encode(search),
                bearerToken
        );
        return StreamSupport.stream(data(response.body()).spliterator(), false)
                .map(customer -> new CustomerOption(
                        customer.path("id").asLong(),
                        customer.path("name").asText(),
                        textOrNull(customer.path("phone")),
                        textOrNull(customer.path("document"))
                ))
                .toList();
    }

    public void finalizeSale(
            String apiBaseUrl,
            String bearerToken,
            Long customerId,
            List<SaleLine> items,
            PaymentMethod paymentMethod,
            BigDecimal amount
    ) {
        sendJson(apiBaseUrl, "/api/sales", bearerToken, buildRequestBody(customerId, items, paymentMethod, amount));
    }

    public OfflineSyncResult syncOfflineSale(String apiBaseUrl, String bearerToken, OfflineSaleSyncRequest request) {
        ApiResponse response = sendJson(apiBaseUrl, "/api/offline/sales/sync", bearerToken, toJson(request));
        JsonNode data = data(response.body());
        List<OfflineConflict> conflicts = StreamSupport.stream(data.path("conflicts").spliterator(), false)
                .map(conflict -> new OfflineConflict(
                        conflict.path("code").asText(),
                        conflict.path("message").asText()
                ))
                .toList();
        JsonNode sale = data.path("sale");
        Long saleId = sale.isMissingNode() || sale.isNull() ? null : sale.path("id").asLong();
        return new OfflineSyncResult(data.path("status").asText(), saleId, conflicts);
    }

    public void finalizeSale(
            String apiBaseUrl,
            String bearerToken,
            long productId,
            BigDecimal quantity,
            PaymentMethod paymentMethod,
            BigDecimal amount
    ) {
        finalizeSale(apiBaseUrl, bearerToken, null, List.of(new SaleLine(productId, quantity)), paymentMethod, amount);
    }

    static String buildRequestBody(Long customerId, List<SaleLine> items, PaymentMethod paymentMethod, BigDecimal amount) {
        String itemJson = items.stream()
                .map(item -> "{\"productId\":%d,\"quantity\":%s}".formatted(item.productId(), item.quantity().toPlainString()))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String customerJson = customerId == null ? "" : "\"customerId\":%d,".formatted(customerId);
        return """
                {%s"items":[%s],"payments":[{"method":"%s","amount":%s}]}
                """.formatted(customerJson, itemJson, paymentMethod.name(), amount.toPlainString()).trim();
    }

    static String buildRequestBody(
            long productId,
            BigDecimal quantity,
            PaymentMethod paymentMethod,
            BigDecimal amount
    ) {
        return buildRequestBody(null, List.of(new SaleLine(productId, quantity)), paymentMethod, amount);
    }

    private ApiResponse sendGet(String apiBaseUrl, String path, String bearerToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalize(apiBaseUrl) + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
        return requireSuccess(transport.send(request));
    }

    private ApiResponse sendJson(String apiBaseUrl, String path, String bearerToken, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalize(apiBaseUrl) + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return requireSuccess(transport.send(builder.build()));
    }

    private ApiResponse requireSuccess(ApiResponse response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(apiErrorMessage(response));
        }
        return response;
    }

    private String apiErrorMessage(ApiResponse response) {
        try {
            JsonNode error = objectMapper.readTree(response.body()).path("error");
            if (error.path("details").isArray() && !error.path("details").isEmpty()) {
                return StreamSupport.stream(error.path("details").spliterator(), false)
                        .map(JsonNode::asText)
                        .reduce((left, right) -> left + " " + right)
                        .orElse("API respondeu com status " + response.statusCode() + ".");
            }
            if (!error.path("message").asText("").isBlank()) {
                return error.path("message").asText();
            }
        } catch (JsonProcessingException exception) {
            return "API respondeu com status " + response.statusCode() + ".";
        }
        return "API respondeu com status " + response.statusCode() + ".";
    }

    private static String normalize(String apiBaseUrl) {
        return apiBaseUrl.replaceAll("/+$", "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value.trim(), StandardCharsets.UTF_8);
    }

    private JsonNode data(String body) {
        try {
            return objectMapper.readTree(body).path("data");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resposta JSON invalida da API.", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel montar requisicao JSON.", exception);
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    @FunctionalInterface
    interface Transport {
        ApiResponse send(HttpRequest request);
    }

    record ApiResponse(int statusCode, String body) {
    }

    public record CatalogProduct(Long id, String name, String barcode, String sku, String unit, BigDecimal salePrice) {
        @Override
        public String toString() {
            String code = sku != null ? sku : barcode;
            return code == null ? name : name + " (" + code + ")";
        }
    }

    public record CustomerOption(Long id, String name, String phone, String document) {
        @Override
        public String toString() {
            return phone == null ? name : name + " (" + phone + ")";
        }
    }

    public record SaleLine(Long productId, BigDecimal quantity) {
    }

    public record OfflineSaleSyncRequest(
            String localSaleId,
            Instant createdAt,
            Long customerId,
            List<OfflineSaleItem> items,
            List<OfflineSalePayment> payments
    ) {
    }

    public record OfflineSaleItem(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record OfflineSalePayment(PaymentMethod method, BigDecimal amount) {
    }

    public record OfflineSyncResult(String status, Long saleId, List<OfflineConflict> conflicts) {
        public String conflictMessage() {
            return conflicts.stream()
                    .map(OfflineConflict::message)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("Venda offline em conflito.");
        }
    }

    public record OfflineConflict(String code, String message) {
    }
}
