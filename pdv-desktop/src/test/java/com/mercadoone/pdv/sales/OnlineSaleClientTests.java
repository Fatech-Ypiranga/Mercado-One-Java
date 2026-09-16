package com.mercadoone.pdv.sales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnlineSaleClientTests {

    @Test
    void buildsBackendSalesContractBody() {
        String body = OnlineSaleClient.buildRequestBody(
                10L,
                new BigDecimal("2.000"),
                PaymentMethod.PIX,
                new BigDecimal("15.00")
        );

        assertEquals(
                "{\"items\":[{\"productId\":10,\"quantity\":2.000}],\"payments\":[{\"method\":\"PIX\",\"amount\":15.00}]}",
                body
        );
    }

    @Test
    void buildsSaleBodyWithCustomerAndMultipleItems() {
        String body = OnlineSaleClient.buildRequestBody(
                5L,
                List.of(
                        new OnlineSaleClient.SaleLine(10L, new BigDecimal("2.000")),
                        new OnlineSaleClient.SaleLine(11L, new BigDecimal("1.000"))
                ),
                PaymentMethod.CASH,
                new BigDecimal("24.50")
        );

        assertEquals(
                "{\"customerId\":5,\"items\":[{\"productId\":10,\"quantity\":2.000},{\"productId\":11,\"quantity\":1.000}],\"payments\":[{\"method\":\"CASH\",\"amount\":24.50}]}",
                body
        );
    }

    @Test
    void logsInAndParsesProductsAndCustomers() {
        List<HttpRequest> requests = new ArrayList<>();
        OnlineSaleClient client = new OnlineSaleClient(request -> {
            requests.add(request);
            String path = request.uri().getPath();
            if (path.endsWith("/api/auth/login")) {
                return new OnlineSaleClient.ApiResponse(200, "{\"success\":true,\"data\":{\"accessToken\":\"token-123\"}}");
            }
            if (path.endsWith("/api/catalog/products")) {
                return new OnlineSaleClient.ApiResponse(200, """
                        {"success":true,"data":[{"active":true,"salePrice":8.90,"unit":"UN","category":{"id":1,"name":"Mercearia","active":true},"sku":"CAF-1","barcode":null,"name":"Cafe \\"Premium\\"","id":7}]}
                        """);
            }
            return new OnlineSaleClient.ApiResponse(200, """
                    {"success":true,"data":[{"document":"123","phone":"11999990000","name":"Maria \\"Cliente\\"","id":3}]}
                    """);
        });

        assertEquals("token-123", client.login("http://localhost:8080", "operador", "senha"));
        assertEquals("Cafe \"Premium\"", client.searchProducts("http://localhost:8080", "token-123", "caf").getFirst().name());
        assertEquals("Maria \"Cliente\"", client.searchCustomers("http://localhost:8080", "token-123", "maria").getFirst().name());
        assertEquals(3, requests.size());
        assertEquals("Bearer token-123", requests.get(1).headers().firstValue("Authorization").orElse(""));
        assertEquals("/api/customers/search", requests.get(2).uri().getPath());
    }
}
