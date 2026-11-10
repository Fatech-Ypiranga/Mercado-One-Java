package com.mercadoone.pdv.offline;

import com.mercadoone.pdv.sales.PaymentMethod;

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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OfflineSaleQueue {

    private final OfflineStorageConfig config;

    public OfflineSaleQueue(OfflineStorageConfig config) {
        this.config = config;
    }

    public void initialize() {
        try {
            Files.createDirectories(config.databasePath().getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel preparar armazenamento offline.", exception);
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists offline_sales (
                        local_sale_id text primary key,
                        created_at text not null,
                        customer_id integer,
                        status text not null,
                        remote_sale_id integer,
                        last_error text,
                        sync_attempts integer not null default 0,
                        updated_at text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists offline_sale_items (
                        id integer primary key autoincrement,
                        local_sale_id text not null references offline_sales(local_sale_id),
                        product_id integer not null,
                        quantity text not null,
                        unit_price text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists offline_sale_payments (
                        id integer primary key autoincrement,
                        local_sale_id text not null references offline_sales(local_sale_id),
                        method text not null,
                        amount text not null
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists offline_sale_sync_attempts (
                        id integer primary key autoincrement,
                        local_sale_id text not null references offline_sales(local_sale_id),
                        status text not null,
                        message text,
                        created_at text not null
                    )
                    """);
            if (!hasColumn(connection, "offline_sales", "discount_amount")) {
                statement.executeUpdate("alter table offline_sales add column discount_amount text not null default '0'");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel inicializar fila offline.", exception);
        }
    }

    public void enqueue(LocalSale sale) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                insertSale(connection, sale);
                insertItems(connection, sale);
                insertPayments(connection, sale);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel salvar venda offline.", exception);
        }
    }

    public void markSent(String localSaleId, Long remoteSaleId) {
        updateStatus(localSaleId, LocalSaleStatus.SENT, remoteSaleId, null);
    }

    public void markConflict(String localSaleId, String message) {
        updateStatus(localSaleId, LocalSaleStatus.CONFLICT, null, message);
    }

    public void markError(String localSaleId, String message) {
        updateStatus(localSaleId, LocalSaleStatus.ERROR, null, message);
    }

    public void markRejected(String localSaleId, String message) {
        updateStatus(localSaleId, LocalSaleStatus.REJECTED, null, message);
    }

    public List<String> conflictSaleIds() {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     select local_sale_id from offline_sales where status = 'CONFLICT' order by created_at asc
                     """);
             ResultSet rows = statement.executeQuery()) {
            List<String> ids = new ArrayList<>();
            while (rows.next()) {
                ids.add(rows.getString(1));
            }
            return ids;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler conflitos offline.", exception);
        }
    }

    public Map<LocalSaleStatus, Integer> countByStatus() {
        Map<LocalSaleStatus, Integer> counts = new EnumMap<>(LocalSaleStatus.class);
        for (LocalSaleStatus status : LocalSaleStatus.values()) {
            counts.put(status, 0);
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     select status, count(*) from offline_sales group by status
                     """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                counts.put(LocalSaleStatus.valueOf(rows.getString(1)), rows.getInt(2));
            }
            return counts;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel contar fila offline.", exception);
        }
    }

    public List<LocalSaleSummary> recentSales(int limit) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     select s.local_sale_id, s.created_at, s.status, s.remote_sale_id, s.last_error, s.discount_amount,
                            (select coalesce(sum(cast(i.quantity as real) * cast(i.unit_price as real)), 0)
                             from offline_sale_items i where i.local_sale_id = s.local_sale_id) as gross
                     from offline_sales s
                     order by s.created_at desc
                     limit ?
                     """)) {
            statement.setInt(1, limit);
            List<LocalSaleSummary> summaries = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    BigDecimal gross = BigDecimal.valueOf(rows.getDouble("gross")).setScale(2, java.math.RoundingMode.HALF_UP);
                    summaries.add(new LocalSaleSummary(
                            rows.getString("local_sale_id"),
                            Instant.parse(rows.getString("created_at")),
                            LocalSaleStatus.valueOf(rows.getString("status")),
                            nullableLong(rows, "remote_sale_id"),
                            rows.getString("last_error"),
                            gross.subtract(new BigDecimal(rows.getString("discount_amount")))
                    ));
                }
            }
            return summaries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler fila offline.", exception);
        }
    }

    public List<LocalSale> pendingSales() {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("""
                     select local_sale_id, created_at, customer_id, status, discount_amount
                     from offline_sales
                     where status in ('PENDING', 'ERROR')
                     order by created_at asc
                     """)) {
            List<LocalSale> sales = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String localSaleId = rows.getString("local_sale_id");
                    sales.add(new LocalSale(
                            localSaleId,
                            Instant.parse(rows.getString("created_at")),
                            nullableLong(rows, "customer_id"),
                            LocalSaleStatus.valueOf(rows.getString("status")),
                            items(connection, localSaleId),
                            payments(connection, localSaleId),
                            new BigDecimal(rows.getString("discount_amount"))
                    ));
                }
            }
            return sales;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel ler fila offline.", exception);
        }
    }

    private void insertSale(Connection connection, LocalSale sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into offline_sales(local_sale_id, created_at, customer_id, status, updated_at, discount_amount)
                values (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, sale.localSaleId());
            statement.setString(2, sale.createdAt().toString());
            if (sale.customerId() == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, sale.customerId());
            }
            statement.setString(4, sale.status().name());
            statement.setString(5, Instant.now().toString());
            statement.setString(6, sale.discountAmount().toPlainString());
            statement.executeUpdate();
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("pragma table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void insertItems(Connection connection, LocalSale sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into offline_sale_items(local_sale_id, product_id, quantity, unit_price)
                values (?, ?, ?, ?)
                """)) {
            for (LocalSaleItem item : sale.items()) {
                statement.setString(1, sale.localSaleId());
                statement.setLong(2, item.productId());
                statement.setString(3, item.quantity().toPlainString());
                statement.setString(4, item.unitPrice().toPlainString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertPayments(Connection connection, LocalSale sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into offline_sale_payments(local_sale_id, method, amount)
                values (?, ?, ?)
                """)) {
            for (LocalSalePayment payment : sale.payments()) {
                statement.setString(1, sale.localSaleId());
                statement.setString(2, payment.method().name());
                statement.setString(3, payment.amount().toPlainString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void updateStatus(String localSaleId, LocalSaleStatus status, Long remoteSaleId, String message) {
        try (Connection connection = connect();
             PreparedStatement update = connection.prepareStatement("""
                     update offline_sales
                     set status = ?, remote_sale_id = coalesce(?, remote_sale_id), last_error = ?, sync_attempts = sync_attempts + 1, updated_at = ?
                     where local_sale_id = ?
                     """);
             PreparedStatement attempt = connection.prepareStatement("""
                     insert into offline_sale_sync_attempts(local_sale_id, status, message, created_at)
                     values (?, ?, ?, ?)
                     """)) {
            update.setString(1, status.name());
            if (remoteSaleId == null) {
                update.setObject(2, null);
            } else {
                update.setLong(2, remoteSaleId);
            }
            update.setString(3, message);
            update.setString(4, Instant.now().toString());
            update.setString(5, localSaleId);
            update.executeUpdate();

            attempt.setString(1, localSaleId);
            attempt.setString(2, status.name());
            attempt.setString(3, message);
            attempt.setString(4, Instant.now().toString());
            attempt.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Nao foi possivel atualizar status offline.", exception);
        }
    }

    private List<LocalSaleItem> items(Connection connection, String localSaleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select product_id, quantity, unit_price
                from offline_sale_items
                where local_sale_id = ?
                order by id asc
                """)) {
            statement.setString(1, localSaleId);
            List<LocalSaleItem> items = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    items.add(new LocalSaleItem(
                            rows.getLong("product_id"),
                            new BigDecimal(rows.getString("quantity")),
                            new BigDecimal(rows.getString("unit_price"))
                    ));
                }
            }
            return items;
        }
    }

    private List<LocalSalePayment> payments(Connection connection, String localSaleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select method, amount
                from offline_sale_payments
                where local_sale_id = ?
                order by id asc
                """)) {
            statement.setString(1, localSaleId);
            List<LocalSalePayment> payments = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    payments.add(new LocalSalePayment(
                            PaymentMethod.valueOf(rows.getString("method")),
                            new BigDecimal(rows.getString("amount"))
                    ));
                }
            }
            return payments;
        }
    }

    private Long nullableLong(ResultSet rows, String column) throws SQLException {
        long value = rows.getLong(column);
        return rows.wasNull() ? null : value;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl());
    }

    public record LocalSale(
            String localSaleId,
            Instant createdAt,
            Long customerId,
            LocalSaleStatus status,
            List<LocalSaleItem> items,
            List<LocalSalePayment> payments,
            BigDecimal discountAmount
    ) {
        public LocalSale {
            discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        }

        public LocalSale(
                String localSaleId,
                Instant createdAt,
                Long customerId,
                LocalSaleStatus status,
                List<LocalSaleItem> items,
                List<LocalSalePayment> payments
        ) {
            this(localSaleId, createdAt, customerId, status, items, payments, BigDecimal.ZERO);
        }

        public BigDecimal grossAmount() {
            return items.stream()
                    .map(LocalSaleItem::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal netAmount() {
            return grossAmount().subtract(discountAmount);
        }
    }

    public record LocalSaleSummary(
            String localSaleId,
            Instant createdAt,
            LocalSaleStatus status,
            Long remoteSaleId,
            String lastMessage,
            BigDecimal netAmount
    ) {
    }

    public record LocalSaleItem(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        public BigDecimal totalAmount() {
            return unitPrice.multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public record LocalSalePayment(PaymentMethod method, BigDecimal amount) {
    }
}
