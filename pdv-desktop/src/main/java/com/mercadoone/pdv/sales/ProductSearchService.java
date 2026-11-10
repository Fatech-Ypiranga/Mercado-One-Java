package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.OfflineCatalogStore;
import com.mercadoone.pdv.sales.CheckoutService.Session;
import com.mercadoone.pdv.sales.OnlineSaleClient.ApiUnavailableException;
import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;

import java.util.List;

/**
 * Busca de produtos com fallback para o catalogo local. Busca vazia bem-sucedida atualiza o catalogo local.
 */
public class ProductSearchService {

    private final OfflineCatalogStore catalogStore;
    private final OnlineSaleClient client;
    private final CheckoutService checkoutService;

    public ProductSearchService(OfflineCatalogStore catalogStore, OnlineSaleClient client, CheckoutService checkoutService) {
        this.catalogStore = catalogStore;
        this.client = client;
        this.checkoutService = checkoutService;
    }

    public List<CatalogProduct> search(Session session, String term) {
        if (!session.authenticated()) {
            return catalogStore.search(term);
        }
        try {
            List<CatalogProduct> products = client.searchProducts(session.apiBaseUrl(), session.token(), term);
            checkoutService.markApiReachable(true);
            if (term == null || term.isBlank()) {
                catalogStore.replaceAll(products);
            }
            return products;
        } catch (ApiUnavailableException exception) {
            checkoutService.markApiReachable(false);
            return catalogStore.search(term);
        }
    }

    /** Substitui o catalogo local pelos produtos ativos da API e devolve a quantidade gravada. */
    public int refreshLocalCatalog(Session session) {
        List<CatalogProduct> products = client.searchProducts(session.apiBaseUrl(), session.token(), "");
        catalogStore.replaceAll(products);
        checkoutService.markApiReachable(true);
        return products.size();
    }
}
