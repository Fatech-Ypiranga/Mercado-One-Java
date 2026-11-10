package com.mercadoone.pdv.sales;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class OnlineSaleClient {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
                throw new ApiUnavailableException("Operacao interrompida antes da resposta da API.", exception);
            } catch (IOException exception) {
                throw new ApiUnavailableException("Nao foi possivel comunicar com a API.", exception);
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
                .map(OnlineSaleClient::customerOption)
                .toList();
    }

    public CustomerOption quickCreateCustomer(
            String apiBaseUrl,
            String bearerToken,
            String name,
            String phone,
            String document,
            boolean contactConsent
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("phone", phone);
        body.put("document", document);
        body.put("contactConsent", contactConsent);
        ApiResponse response = sendJson(apiBaseUrl, "/api/customers/quick", bearerToken, toJson(body));
        return customerOption(data(response.body()));
    }

    /**
     * Percentual maximo de desconto do operador autenticado. {@code null} significa sem limite alem do total.
     */
    public DiscountLimit discountPolicy(String apiBaseUrl, String bearerToken) {
        JsonNode data = data(sendGet(apiBaseUrl, "/api/sales/discount-policy", bearerToken).body());
        JsonNode max = data.path("maxDiscountPercent");
        return new DiscountLimit(max.isMissingNode() || max.isNull() ? null : max.decimalValue());
    }

    /**
     * Confirma a venda online em {@code POST /api/sales}. O {@code clientSaleId} torna a chamada idempotente.
     */
    public Long finalizeOnlineSale(String apiBaseUrl, String bearerToken, OnlineSaleRequest request) {
        ApiResponse response = sendJson(apiBaseUrl, "/api/sales", bearerToken, toJson(request));
        return data(response.body()).path("id").asLong();
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

    public List<RemoteSaleStatus> remoteStatus(String apiBaseUrl, String bearerToken, List<String> localSaleIds) {
        if (localSaleIds.isEmpty()) {
            return List.of();
        }
        String query = localSaleIds.stream()
                .map(id -> "localSaleIds=" + encode(id))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        JsonNode data = data(sendGet(apiBaseUrl, "/api/offline/sales/status?" + query, bearerToken).body());
        return StreamSupport.stream(data.spliterator(), false)
                .map(node -> new RemoteSaleStatus(
                        node.path("localSaleId").asText(),
                        node.path("status").asText(),
                        node.path("remoteSaleId").isNull() || node.path("remoteSaleId").isMissingNode()
                                ? null
                                : node.path("remoteSaleId").asLong(),
                        textOrNull(node.path("note"))
                ))
                .toList();
    }

    String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel montar requisicao JSON.", exception);
        }
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
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response;
        }
        if (status >= 500) {
            throw new ApiUnavailableException("API indisponivel (status " + status + ").", null, true);
        }
        if (status == 401) {
            throw new ApiRejectedException(status, "Sessao expirada ou credenciais invalidas. Entre novamente.");
        }
        if (status == 403) {
            throw new ApiRejectedException(status, "Perfil sem permissao para esta operacao.");
        }
        throw new ApiRejectedException(status, apiErrorMessage(response));
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
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return "API respondeu com status " + response.statusCode() + ".";
        }
        return "API respondeu com status " + response.statusCode() + ".";
    }

    private static CustomerOption customerOption(JsonNode customer) {
        return new CustomerOption(
                customer.path("id").asLong(),
                customer.path("name").asText(),
                textOrNull(customer.path("phone")),
                textOrNull(customer.path("document"))
        );
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

    /** Falha de comunicacao ou erro 5xx: a venda deve seguir para a fila offline. */
    public static class ApiUnavailableException extends IllegalStateException {
        private final boolean serverError;

        public ApiUnavailableException(String message, Throwable cause) {
            this(message, cause, false);
        }

        public ApiUnavailableException(String message, Throwable cause, boolean serverError) {
            super(message, cause);
            this.serverError = serverError;
        }

        /** A API respondeu 5xx: o servidor esta no ar, a falha pode ser especifica da requisicao. */
        public boolean serverError() {
            return serverError;
        }
    }

    /** A API respondeu e recusou a operacao (4xx). */
    public static class ApiRejectedException extends IllegalStateException {
        private final int statusCode;

        public ApiRejectedException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }

        public boolean isAuthenticationFailure() {
            return statusCode == 401;
        }
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

    public record DiscountLimit(BigDecimal maxPercent) {
        public static final DiscountLimit NONE = new DiscountLimit(BigDecimal.ZERO);

        public boolean unlimited() {
            return maxPercent == null;
        }
    }

    public record SaleLine(Long productId, BigDecimal quantity) {
    }

    public record OnlineSaleRequest(
            String clientSaleId,
            Long customerId,
            List<SaleLine> items,
            List<OfflineSalePayment> payments,
            BigDecimal discountAmount
    ) {
    }

    /**
     * Payload de {@code POST /api/offline/sales/sync}. {@code createdAt} vai como texto ISO-8601
     * para nao depender do modulo jsr310 do Jackson.
     */
    public record OfflineSaleSyncRequest(
            String localSaleId,
            String createdAt,
            Long customerId,
            List<OfflineSaleItem> items,
            List<OfflineSalePayment> payments,
            BigDecimal discountAmount
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

    public record RemoteSaleStatus(String localSaleId, String status, Long remoteSaleId, String note) {
    }
}
