package com.mercadoone.pdv.offline;

import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OfflineCatalogStore {

    private final OfflineStorageConfig config;

    public OfflineCatalogStore(OfflineStorageConfig config) {
        this.config = config;
    }

    public void initialize() {
        try {
            Files.createDirectories(config.databasePath().getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel preparar catalogo local.", exception);
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists local_catalog_products (
                        product_id integer primary key,
                        name text not null,
                        barcode text,
                        sku text,
                        unit text not null,
                        sale_price text not null,
                        updated_at text not null
                    )
                    """);
            statement.executeUpdate("""
                    create index if not exists ix_local_catalog_products_name
                    on local_catalog_products(name)
                    """);
            statement.executeUpdate("""
                    create index if not exists ix_local_catalog_products_sku
                    on local_catalog_products(sku)
                    """);
            statement.executeUpdate("""
                    create index if not exists ix_local_catalog_products_barcode
                    on local_catalog_products(barcode)
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel inicializar catalogo local.", exception);
        }
    }

    public void replaceAll(List<CatalogProduct> products) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement("""
                         insert into local_catalog_products(product_id, name, barcode, sku, unit, sale_price, updated_at)
                         values (?, ?, ?, ?, ?, ?, ?)
                         """)) {
                clear.executeUpdate("delete from local_catalog_products");
                String now = Instant.now().toString();
                for (CatalogProduct product : products) {
                    insert.setLong(1, product.id());
                    insert.setString(2, product.name());
                    insert.setString(3, product.barcode());
                    insert.setString(4, product.sku());
                    insert.setString(5, product.unit());
                    insert.setString(6, product.salePrice().toPlainString());
                    insert.setString(7, now);
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel atualizar catalogo local.", exception);
        }
    }

    public List<CatalogProduct> search(String term) {
        String normalized = term == null ? "" : term.trim().toLowerCase();
        String pattern = "%" + normalized + "%";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     select product_id, name, barcode, sku, unit, sale_price
                     from local_catalog_products
                     where ? = ''
                        or lower(name) like ?
                        or lower(coalesce(sku, '')) like ?
                        or lower(coalesce(barcode, '')) like ?
                     order by name asc
                     limit 50
                     """)) {
            statement.setString(1, normalized);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            List<CatalogProduct> products = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    products.add(new CatalogProduct(
                            rows.getLong("product_id"),
                            rows.getString("name"),
                            rows.getString("barcode"),
                            rows.getString("sku"),
                            rows.getString("unit"),
                            new BigDecimal(rows.getString("sale_price"))
                    ));
                }
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel consultar catalogo local.", exception);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl());
    }
}
