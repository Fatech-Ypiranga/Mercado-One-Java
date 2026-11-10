package com.mercadoone.backend.modules.offline.api;

import com.mercadoone.backend.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OfflineStatusTests extends ApiIntegrationTest {

    @Test
    void resendingSameLocalSaleDoesNotDuplicateSale() throws Exception {
        String admin = adminToken();
        long productId = createProduct(admin, "Offline Repetida", unique("OFFDUP"), "5.00");
        registerEntry(admin, productId, "10");
        String localId = unique("local");
        String body = syncBody(localId, productId, "5.00", "1", "0", "5.00");

        long first = data(postJson("/api/offline/sales/sync", admin, body)
                .andExpect(jsonPath("$.data.status").value("SENT"))).get("sale").get("id").asLong();
        long second = data(postJson("/api/offline/sales/sync", admin, body)
                .andExpect(jsonPath("$.data.status").value("SENT"))).get("sale").get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);

        postJson("/api/sales", admin, """
                {"clientSaleId":"%s","items":[{"productId":%d,"quantity":1}],"payments":[{"method":"CASH","amount":5.00}]}
                """.formatted(localId, productId))
                .andExpect(jsonPath("$.data.id").value(first));
    }

    @Test
    void offlineSaleWithDiscountSyncsWithNetTotal() throws Exception {
        String admin = adminToken();
        long productId = createProduct(admin, "Offline Desconto", unique("OFFDESC"), "10.00");
        registerEntry(admin, productId, "10");

        postJson("/api/offline/sales/sync", admin, syncBody(unique("local"), productId, "10.00", "2", "3.00", "17.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.sale.discountAmount").value(3.0))
                .andExpect(jsonPath("$.data.sale.totalAmount").value(17.0));
    }

    @Test
    void operatorOfflineDiscountAboveLimitBecomesConflict() throws Exception {
        String admin = adminToken();
        String login = unique("op-off");
        createUser(admin, login, "OPERADOR_CAIXA");
        String operator = loginToken(login, "senha123");
        long productId = createProduct(admin, "Offline Limite", unique("OFFLIM"), "10.00");
        registerEntry(admin, productId, "10");

        postJson("/api/offline/sales/sync", operator, syncBody(unique("local"), productId, "10.00", "1", "5.00", "5.00"))
                .andExpect(jsonPath("$.data.status").value("CONFLICT"))
                .andExpect(jsonPath("$.data.conflicts[0].code").value("DISCOUNT_NOT_ALLOWED"));
    }

    @Test
    void statusEndpointReflectsConflictResolution() throws Exception {
        String admin = adminToken();
        long productId = createProduct(admin, "Offline Status", unique("OFFST"), "10.00");
        registerEntry(admin, productId, "10");
        String acceptedId = unique("local-accept");
        String rejectedId = unique("local-reject");
        String sentId = unique("local-sent");

        postJson("/api/offline/sales/sync", admin, syncBody(acceptedId, productId, "9.00", "1", "0", "9.00"))
                .andExpect(jsonPath("$.data.status").value("CONFLICT"));
        postJson("/api/offline/sales/sync", admin, syncBody(rejectedId, productId, "9.00", "1", "0", "9.00"))
                .andExpect(jsonPath("$.data.status").value("CONFLICT"));
        postJson("/api/offline/sales/sync", admin, syncBody(sentId, productId, "10.00", "1", "0", "10.00"))
                .andExpect(jsonPath("$.data.status").value("SENT"));

        mockMvc.perform(get("/api/offline/sales/status")
                        .param("localSaleIds", acceptedId, rejectedId, sentId, "desconhecida")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].status").value("CONFLICT"))
                .andExpect(jsonPath("$.data[2].status").value("SENT"))
                .andExpect(jsonPath("$.data[3].status").value("UNKNOWN"));

        var conflicts = data(mockMvc.perform(get("/api/offline/sales/conflicts").header("Authorization", "Bearer " + admin)));
        long acceptConflictId = -1;
        long rejectConflictId = -1;
        for (var conflict : conflicts) {
            if (conflict.get("localSaleId").asText().equals(acceptedId)) {
                acceptConflictId = conflict.get("id").asLong();
            }
            if (conflict.get("localSaleId").asText().equals(rejectedId)) {
                rejectConflictId = conflict.get("id").asLong();
            }
        }
        postJson("/api/offline/sales/conflicts/" + acceptConflictId + "/resolve", admin, """
                {"action":"ACCEPT","note":"Preco antigo honrado"}
                """).andExpect(status().isOk());
        postJson("/api/offline/sales/conflicts/" + rejectConflictId + "/resolve", admin, """
                {"action":"REJECT","note":"Venda duplicada"}
                """).andExpect(status().isOk());

        mockMvc.perform(get("/api/offline/sales/status")
                        .param("localSaleIds", acceptedId + "," + rejectedId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data[0].status").value("SENT"))
                .andExpect(jsonPath("$.data[0].remoteSaleId").exists())
                .andExpect(jsonPath("$.data[1].status").value("REJECTED"))
                .andExpect(jsonPath("$.data[1].note").value("Venda duplicada"));
    }

    private static String syncBody(String localId, long productId, String unitPrice, String quantity, String discount, String paid) {
        return """
                {"localSaleId":"%s","createdAt":"2026-11-01T12:00:00Z",
                 "items":[{"productId":%d,"quantity":%s,"unitPrice":%s}],
                 "discountAmount":%s,
                 "payments":[{"method":"CASH","amount":%s}]}
                """.formatted(localId, productId, quantity, unitPrice, discount, paid);
    }
}
