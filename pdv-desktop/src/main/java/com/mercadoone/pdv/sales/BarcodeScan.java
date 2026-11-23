package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;

import java.util.List;
import java.util.Optional;

/**
 * Regras da leitura por leitor de código de barras.
 *
 * <p>O leitor funciona como um teclado: "digita" o código e aperta Enter. Quando o código bate
 * exatamente com um único produto, o PDV adiciona o item sozinho; busca por nome continua exigindo
 * que o operador escolha.
 */
public final class BarcodeScan {

    private BarcodeScan() {
    }

    /**
     * O produto cujo código de barras ou SKU é exatamente o termo lido (sem diferenciar maiúsculas
     * nem espaços e quebras de linha nas pontas). Vazio se não houver, ou se mais de um produto
     * tiver o mesmo código, caso em que o operador deve escolher.
     */
    public static Optional<CatalogProduct> exactMatch(String term, List<CatalogProduct> results) {
        if (term == null || results == null) {
            return Optional.empty();
        }
        String code = term.trim();
        if (code.isEmpty()) {
            return Optional.empty();
        }
        List<CatalogProduct> matches = results.stream()
                .filter(product -> code.equalsIgnoreCase(trimmed(product.barcode())) || code.equalsIgnoreCase(trimmed(product.sku())))
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    /**
     * Se o texto parece um código de barras de varejo: só dígitos, de 8 a 14 (EAN-8, UPC-A,
     * EAN-13, GTIN-14). Usado para avisar "código não encontrado" em vez de "nenhum produto".
     */
    public static boolean looksLikeBarcode(String term) {
        if (term == null) {
            return false;
        }
        String code = term.trim();
        return code.length() >= 8 && code.length() <= 14 && code.chars().allMatch(Character::isDigit);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
