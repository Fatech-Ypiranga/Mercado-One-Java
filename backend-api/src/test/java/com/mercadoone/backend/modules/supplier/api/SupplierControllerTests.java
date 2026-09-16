package com.mercadoone.backend.modules.supplier.api;

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
        "spring.datasource.url=jdbc:h2:mem:mercado_one_supplier_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mercado-one.security.jwt-secret=test-secret-with-at-least-32-characters",
        "mercado-one.security.seed-admin.enabled=true",
        "mercado-one.security.seed-admin.name=Administrador",
        "mercado-one.security.seed-admin.login=admin@mercado.one",
        "mercado-one.security.seed-admin.password=admin123"
})
@AutoConfigureMockMvc
class SupplierControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void managesSuppliersAndLinksInventoryEntries() throws Exception {
        String token = loginToken();
        long supplierId = createSupplier(token, "Distribuidora Sul");
        long productId = createProduct(token, "Produto Fornecedor", "SUP-001", true);

        mockMvc.perform(put("/api/suppliers/{id}", supplierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"Distribuidora Sul Atualizada",
                                  "document":"123",
                                  "phone":"11999990000",
                                  "email":"contato@example.com",
                                  "notes":"Preferencial",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Distribuidora Sul Atualizada"));

        mockMvc.perform(post("/api/inventory/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"productId":%d,"quantity":4.000,"supplierId":%d,"documentNumber":"NF-9"}
                                """.formatted(productId, supplierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplier.id").value(supplierId))
                .andExpect(jsonPath("$.data.supplierName").value("Distribuidora Sul Atualizada"));

        mockMvc.perform(get("/api/inventory/movements")
                        .param("supplierId", Long.toString(supplierId))
                        .param("type", "ENTRY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].supplier.id").value(supplierId));
    }

    private long createSupplier(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"%s",
                                  "document":"123",
                                  "phone":null,
                                  "email":null,
                                  "notes":null,
                                  "active":true
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private long createProduct(String token, String name, String sku, boolean active) throws Exception {
        long categoryId = createCategory(token, "Categoria " + sku, true);
        String response = mockMvc.perform(post("/api/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {
                                  "name":"%s",
                                  "barcode":null,
                                  "sku":"%s",
                                  "categoryId":%d,
                                  "unit":"UN",
                                  "salePrice":10.00,
                                  "active":%s
                                }
                                """.formatted(name, sku, categoryId, active)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("data").get("id").asLong();
    }

    private long createCategory(String token, String name, boolean active) throws Exception {
        String response = mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"%s","active":%s}
                                """.formatted(name, active)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin@mercado.one","password":"admin123"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("accessToken").asText();
    }
}
