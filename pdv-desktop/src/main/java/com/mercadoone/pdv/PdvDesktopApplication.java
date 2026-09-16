package com.mercadoone.pdv;

import com.mercadoone.pdv.offline.OfflineStorageConfig;
import com.mercadoone.pdv.offline.OfflineCatalogStore;
import com.mercadoone.pdv.offline.LocalSaleStatus;
import com.mercadoone.pdv.offline.OfflineSaleQueue;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSale;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSaleItem;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;
import com.mercadoone.pdv.sales.OnlineSaleClient.CustomerOption;
import com.mercadoone.pdv.sales.OnlineSaleClient;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSaleItem;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSalePayment;
import com.mercadoone.pdv.sales.OnlineSaleClient.OfflineSaleSyncRequest;
import com.mercadoone.pdv.sales.PaymentMethod;
import com.mercadoone.pdv.sync.SyncStatus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PdvDesktopApplication extends Application {

    private final OfflineStorageConfig storageConfig = OfflineStorageConfig.defaultConfig();
    private final OfflineSaleQueue offlineSaleQueue = new OfflineSaleQueue(storageConfig);
    private final OfflineCatalogStore offlineCatalogStore = new OfflineCatalogStore(storageConfig);
    private final OnlineSaleClient onlineSaleClient = new OnlineSaleClient();
    private final List<CartLine> cart = new ArrayList<>();
    private SyncStatus syncStatus = SyncStatus.OFFLINE_READY;
    private String bearerToken = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        offlineSaleQueue.initialize();
        offlineCatalogStore.initialize();
        stage.setTitle("Mercado One PDV");
        Scene scene = new Scene(createRoot(), 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/pdv.css").toExternalForm());

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.show();
    }

    private BorderPane createRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(createHeader());
        root.setCenter(createPlaceholder());

        return root;
    }

    private HBox createHeader() {
        Label brand = new Label("Mercado One PDV");
        brand.getStyleClass().add("brand");

        Label status = new Label(syncStatus.displayName());
        status.getStyleClass().add("status");

        HBox header = new HBox(16, brand, status);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        return header;
    }

    private VBox createPlaceholder() {
        Label title = new Label("Venda presencial");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Estrutura inicial do PDV desktop com armazenamento local preparado em " + storageConfig.databasePath());
        subtitle.getStyleClass().add("subtitle");

        TextField apiUrl = new TextField("http://localhost:8080");
        TextField login = new TextField("operador");
        PasswordField password = new PasswordField();
        TextField productSearch = new TextField();
        ComboBox<CatalogProduct> products = new ComboBox<>();
        TextField customerSearch = new TextField();
        ComboBox<CustomerOption> customers = new ComboBox<>();
        TextField quantity = new TextField("1.000");
        ListView<String> cartView = new ListView<>();
        cartView.setPrefHeight(150);
        ComboBox<PaymentMethod> paymentMethod = new ComboBox<>();
        paymentMethod.getItems().setAll(PaymentMethod.values());
        paymentMethod.setValue(PaymentMethod.CASH);
        TextField amount = new TextField("0.00");
        Label total = new Label("Total: R$ 0,00");
        total.getStyleClass().add("title");
        Label feedback = new Label("Entre com um operador, busque produtos e finalize a venda online.");
        feedback.getStyleClass().add("subtitle");
        TextArea receipt = new TextArea();
        receipt.setEditable(false);
        receipt.setPrefRowCount(8);
        receipt.setPromptText("Comprovante simples nao fiscal");

        Button loginButton = new Button("Entrar");
        loginButton.setOnAction(event -> {
            loginButton.setDisable(true);
            feedback.setText("Autenticando operador...");
            CompletableFuture.supplyAsync(() -> onlineSaleClient.login(apiUrl.getText(), login.getText(), password.getText()))
                    .whenComplete((token, error) -> Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        if (error == null) {
                            bearerToken = token;
                            feedback.setText("Operador autenticado. Busca de produtos liberada.");
                            password.clear();
                            refreshLocalCatalog(apiUrl.getText(), feedback);
                            synchronizePending(apiUrl.getText(), feedback);
                        } else {
                            feedback.setText(errorMessage(error));
                        }
                    }));
        });

        Button searchProducts = new Button("Buscar produtos");
        searchProducts.setOnAction(event -> {
            searchProducts.setDisable(true);
            feedback.setText("Buscando produtos...");
            CompletableFuture.supplyAsync(() -> searchProducts(apiUrl.getText(), productSearch.getText()))
                    .whenComplete((result, error) -> Platform.runLater(() -> {
                        searchProducts.setDisable(false);
                        if (error == null) {
                            products.getItems().setAll(result);
                            feedback.setText(result.isEmpty() ? "Nenhum produto ativo encontrado." : "Selecione um produto para adicionar.");
                        } else {
                            feedback.setText(errorMessage(error));
                        }
                    }));
        });

        Button searchCustomers = new Button("Buscar clientes");
        searchCustomers.setOnAction(event -> {
            searchCustomers.setDisable(true);
            feedback.setText("Buscando clientes...");
            CompletableFuture.supplyAsync(() -> onlineSaleClient.searchCustomers(apiUrl.getText(), bearerToken, customerSearch.getText()))
                    .whenComplete((result, error) -> Platform.runLater(() -> {
                        searchCustomers.setDisable(false);
                        if (error == null) {
                            customers.getItems().setAll(result);
                            feedback.setText(result.isEmpty() ? "Nenhum cliente ativo encontrado." : "Cliente opcional selecionavel.");
                        } else {
                            feedback.setText(errorMessage(error));
                        }
                    }));
        });

        Button clearCustomer = new Button("Consumidor nao identificado");
        clearCustomer.setOnAction(event -> customers.setValue(null));

        Button refreshCatalog = new Button("Sincronizar catalogo local");
        refreshCatalog.setOnAction(event -> refreshLocalCatalog(apiUrl.getText(), feedback));

        Button addItem = new Button("Adicionar item");
        addItem.setOnAction(event -> {
            CatalogProduct product = products.getValue();
            if (product == null) {
                feedback.setText("Selecione um produto.");
                return;
            }
            BigDecimal parsedQuantity;
            try {
                parsedQuantity = new BigDecimal(quantity.getText());
            } catch (NumberFormatException exception) {
                feedback.setText("Informe uma quantidade valida.");
                return;
            }
            if (parsedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                feedback.setText("Informe uma quantidade maior que zero.");
                return;
            }
            cart.add(new CartLine(product, parsedQuantity));
            refreshCart(cartView, amount, total);
            feedback.setText("Item adicionado ao carrinho.");
        });

        Button removeItem = new Button("Remover ultimo item");
        removeItem.setOnAction(event -> {
            if (!cart.isEmpty()) {
                cart.remove(cart.size() - 1);
                refreshCart(cartView, amount, total);
            }
        });

        Button clearCart = new Button("Limpar carrinho");
        clearCart.setOnAction(event -> {
            cart.clear();
            refreshCart(cartView, amount, total);
            feedback.setText("Carrinho limpo.");
        });

        Button finalizeSale = new Button("Finalizar venda online");
        finalizeSale.setOnAction(event -> {
            if (cart.isEmpty()) {
                feedback.setText("Adicione ao menos um item.");
                return;
            }
            finalizeSale.setDisable(true);
            String localSaleId = UUID.randomUUID().toString();
            LocalSale localSale = new LocalSale(
                    localSaleId,
                    Instant.now(),
                    customers.getValue() == null ? null : customers.getValue().id(),
                    LocalSaleStatus.PENDING,
                    cart.stream()
                            .map(line -> new LocalSaleItem(line.product().id(), line.quantity(), line.product().salePrice()))
                            .toList(),
                    List.of(new LocalSalePayment(paymentMethod.getValue(), new BigDecimal(amount.getText())))
            );
            receipt.setText(receiptText(localSale));
            try {
                offlineSaleQueue.enqueue(localSale);
            } catch (RuntimeException exception) {
                finalizeSale.setDisable(false);
                feedback.setText(errorMessage(exception));
                return;
            }
            cart.clear();
            refreshCart(cartView, amount, total);
            feedback.setText("Venda salva localmente. Sincronizando...");
            CompletableFuture.supplyAsync(() -> onlineSaleClient.syncOfflineSale(
                    apiUrl.getText(),
                    bearerToken,
                    toSyncRequest(localSale)
            )).whenComplete((result, error) -> Platform.runLater(() -> {
                finalizeSale.setDisable(false);
                if (error == null) {
                    if ("SENT".equals(result.status())) {
                        offlineSaleQueue.markSent(localSaleId, result.saleId());
                        feedback.setText("Venda salva localmente e sincronizada com a API.");
                    } else {
                        offlineSaleQueue.markConflict(localSaleId, result.conflictMessage());
                        feedback.setText(result.conflictMessage());
                    }
                } else {
                    offlineSaleQueue.markError(localSaleId, errorMessage(error));
                    feedback.setText("Venda salva localmente. " + errorMessage(error));
                }
            }));
        });

        VBox content = new VBox(
                14,
                title,
                subtitle,
                labeled("API", apiUrl),
                labeled("Login", login),
                labeled("Senha", password),
                loginButton,
                labeled("Buscar produto", productSearch),
                searchProducts,
                refreshCatalog,
                labeled("Produto", products),
                labeled("Quantidade", quantity),
                addItem,
                removeItem,
                labeled("Buscar cliente opcional", customerSearch),
                searchCustomers,
                labeled("Cliente", customers),
                clearCustomer,
                labeled("Carrinho", cartView),
                clearCart,
                total,
                labeled("Pagamento", paymentMethod),
                labeled("Valor pago", amount),
                finalizeSale,
                labeled("Comprovante", receipt),
                feedback
        );
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_LEFT);

        return content;
    }

    private VBox labeled(String label, javafx.scene.Node field) {
        Label text = new Label(label);
        text.getStyleClass().add("subtitle");
        VBox box = new VBox(6, text, field);
        box.setMaxWidth(420);
        return box;
    }

    private void refreshCart(ListView<String> cartView, TextField amount, Label total) {
        cartView.getItems().setAll(cart.stream()
                .map(line -> "%s x %s = R$ %s".formatted(
                        line.quantity().toPlainString(),
                        line.product().name(),
                        line.total().toPlainString()
                ))
                .toList());
        BigDecimal cartTotal = cart.stream()
                .map(CartLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        amount.setText(cartTotal.toPlainString());
        total.setText("Total: R$ " + cartTotal.toPlainString().replace(".", ","));
    }

    private String errorMessage(Throwable error) {
        Throwable cause = error.getCause();
        return cause == null ? error.getMessage() : cause.getMessage();
    }

    private OfflineSaleSyncRequest toSyncRequest(LocalSale sale) {
        return new OfflineSaleSyncRequest(
                sale.localSaleId(),
                sale.createdAt(),
                sale.customerId(),
                sale.items().stream()
                        .map(item -> new OfflineSaleItem(item.productId(), item.quantity(), item.unitPrice()))
                        .toList(),
                sale.payments().stream()
                        .map(payment -> new OfflineSalePayment(payment.method(), payment.amount()))
                        .toList()
        );
    }

    private List<CatalogProduct> searchProducts(String apiBaseUrl, String term) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return offlineCatalogStore.search(term);
        }
        try {
            List<CatalogProduct> onlineProducts = onlineSaleClient.searchProducts(apiBaseUrl, bearerToken, term);
            if (term == null || term.isBlank()) {
                offlineCatalogStore.replaceAll(onlineProducts);
            }
            return onlineProducts;
        } catch (RuntimeException exception) {
            return offlineCatalogStore.search(term);
        }
    }

    private void refreshLocalCatalog(String apiBaseUrl, Label feedback) {
        if (bearerToken == null || bearerToken.isBlank()) {
            feedback.setText("Entre com um operador para sincronizar o catalogo local.");
            return;
        }
        CompletableFuture.supplyAsync(() -> onlineSaleClient.searchProducts(apiBaseUrl, bearerToken, ""))
                .whenComplete((products, error) -> Platform.runLater(() -> {
                    if (error == null) {
                        offlineCatalogStore.replaceAll(products);
                        feedback.setText("Catalogo local atualizado com " + products.size() + " produto(s).");
                    } else {
                        feedback.setText("Catalogo local mantido. " + errorMessage(error));
                    }
                }));
    }

    private void synchronizePending(String apiBaseUrl, Label feedback) {
        CompletableFuture.runAsync(() -> {
            for (LocalSale sale : offlineSaleQueue.pendingSales()) {
                try {
                    var result = onlineSaleClient.syncOfflineSale(apiBaseUrl, bearerToken, toSyncRequest(sale));
                    if ("SENT".equals(result.status())) {
                        offlineSaleQueue.markSent(sale.localSaleId(), result.saleId());
                    } else {
                        offlineSaleQueue.markConflict(sale.localSaleId(), result.conflictMessage());
                    }
                } catch (RuntimeException exception) {
                    offlineSaleQueue.markError(sale.localSaleId(), errorMessage(exception));
                }
            }
        }).whenComplete((ignored, error) -> Platform.runLater(() -> {
            if (error == null) {
                feedback.setText("Operador autenticado. Fila offline verificada.");
            }
        }));
    }

    private String receiptText(LocalSale sale) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Mercado One\n");
        receipt.append("Comprovante simples nao fiscal\n");
        receipt.append("Venda local: ").append(sale.localSaleId()).append('\n');
        receipt.append("Data: ").append(sale.createdAt()).append('\n');
        receipt.append("------------------------------\n");
        for (CartLine line : cart) {
            receipt.append(line.product().name())
                    .append("  ")
                    .append(line.quantity().toPlainString())
                    .append(" x ")
                    .append(line.product().salePrice().toPlainString())
                    .append(" = ")
                    .append(line.total().toPlainString())
                    .append('\n');
        }
        BigDecimal totalAmount = sale.items().stream()
                .map(item -> item.quantity().multiply(item.unitPrice()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receipt.append("------------------------------\n");
        receipt.append("Total: R$ ").append(totalAmount.toPlainString()).append('\n');
        receipt.append("Pagamento: ").append(sale.payments().getFirst().method()).append('\n');
        return receipt.toString();
    }

    private record CartLine(CatalogProduct product, BigDecimal quantity) {
        BigDecimal total() {
            return product.salePrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
