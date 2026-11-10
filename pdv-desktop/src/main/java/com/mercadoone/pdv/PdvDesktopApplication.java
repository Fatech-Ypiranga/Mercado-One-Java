package com.mercadoone.pdv;

import com.mercadoone.pdv.offline.OfflineCatalogStore;
import com.mercadoone.pdv.offline.OfflineSaleQueue;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSaleSummary;
import com.mercadoone.pdv.offline.OfflineStorageConfig;
import com.mercadoone.pdv.sales.CheckoutService;
import com.mercadoone.pdv.sales.CheckoutService.CheckoutResult;
import com.mercadoone.pdv.sales.CheckoutService.Draft;
import com.mercadoone.pdv.sales.CheckoutService.DraftLine;
import com.mercadoone.pdv.sales.CheckoutService.Session;
import com.mercadoone.pdv.sales.OnlineSaleClient;
import com.mercadoone.pdv.sales.OnlineSaleClient.CatalogProduct;
import com.mercadoone.pdv.sales.OnlineSaleClient.CustomerOption;
import com.mercadoone.pdv.sales.OnlineSaleClient.DiscountLimit;
import com.mercadoone.pdv.sales.PaymentMethod;
import com.mercadoone.pdv.sales.ReceiptFormatter;
import com.mercadoone.pdv.sync.SyncStatus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PdvDesktopApplication extends Application {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final OfflineStorageConfig storageConfig = OfflineStorageConfig.defaultConfig();
    private final OfflineSaleQueue offlineSaleQueue = new OfflineSaleQueue(storageConfig);
    private final OfflineCatalogStore offlineCatalogStore = new OfflineCatalogStore(storageConfig);
    private final OnlineSaleClient onlineSaleClient = new OnlineSaleClient();
    private final CheckoutService checkoutService = new CheckoutService(offlineSaleQueue, onlineSaleClient);
    private final List<DraftLine> cart = new ArrayList<>();
    private final List<LocalSalePayment> payments = new ArrayList<>();
    private volatile String bearerToken = "";

    private final TextField apiUrl = new TextField(defaultApiUrl());
    private final Label statusBadge = new Label();
    private final Label operatorLabel = new Label("Sem operador");
    private final Label feedback = new Label("Entre com um operador. Sem API, a venda segue para a fila offline.");
    private final ListView<String> cartView = new ListView<>();
    private final ListView<String> paymentsView = new ListView<>();
    private final ListView<String> queueView = new ListView<>();
    private final TextField discount = new TextField("0.00");
    private final Label discountHint = new Label();
    private final Label totals = new Label();
    private final TextField paymentAmount = new TextField("0.00");
    private final ComboBox<PaymentMethod> method = new ComboBox<>();
    private final ComboBox<CustomerOption> customers = new ComboBox<>();
    private final TextArea receipt = new TextArea();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        offlineSaleQueue.initialize();
        offlineCatalogStore.initialize();
        stage.setTitle("Mercado One PDV");
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(createHeader());
        root.setCenter(createBody());
        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(getClass().getResource("/styles/pdv.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.show();
        refreshCart();
        refreshStatus();
        startBackgroundSync();
    }

    private HBox createHeader() {
        Label brand = new Label("Mercado One PDV");
        brand.getStyleClass().add("brand");
        statusBadge.getStyleClass().add("status");
        operatorLabel.getStyleClass().add("subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label storage = new Label("Base local: " + storageConfig.databasePath());
        storage.getStyleClass().add("hint");
        HBox header = new HBox(16, brand, statusBadge, operatorLabel, spacer, storage);
        header.setPadding(new Insets(14, 22, 14, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private Node createBody() {
        VBox left = column(sessionPanel(), productPanel(), customerPanel());
        VBox center = column(cartPanel(), paymentPanel());
        VBox right = column(receiptPanel(), queuePanel());
        HBox body = new HBox(18, scroll(left), scroll(center), scroll(right));
        body.setPadding(new Insets(18));
        HBox.setHgrow(body.getChildren().get(1), Priority.ALWAYS);
        BorderPane wrapper = new BorderPane(body);
        feedback.getStyleClass().add("feedback");
        feedback.setWrapText(true);
        feedback.setPadding(new Insets(10, 22, 14, 22));
        wrapper.setBottom(feedback);
        return wrapper;
    }

    private VBox sessionPanel() {
        TextField login = new TextField("operador");
        PasswordField password = new PasswordField();
        Button loginButton = primary("Entrar");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> runAsync(loginButton, "Autenticando operador...",
                () -> onlineSaleClient.login(apiUrl.getText(), login.getText(), password.getText()),
                token -> {
                    bearerToken = token;
                    password.clear();
                    operatorLabel.setText("Operador: " + login.getText().trim());
                    checkoutService.markApiReachable(true);
                    feedback.setText("Operador autenticado. Atualizando catalogo e fila offline...");
                    refreshLocalCatalog();
                    loadDiscountPolicy();
                    synchronize();
                }));
        Button logout = new Button("Sair");
        logout.setOnAction(event -> {
            bearerToken = "";
            operatorLabel.setText("Sem operador");
            feedback.setText("Sessao encerrada. Vendas seguem para a fila offline ate o proximo login.");
            refreshStatus();
        });
        return panel("Sessao", labeled("API", apiUrl), labeled("Login", login), labeled("Senha", password),
                new HBox(8, loginButton, logout));
    }

    private VBox productPanel() {
        TextField search = new TextField();
        search.setPromptText("Codigo de barras, SKU ou nome");
        ComboBox<CatalogProduct> products = new ComboBox<>();
        products.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField("1");
        Button searchButton = new Button("Buscar");
        Button add = primary("Adicionar item");

        Runnable doSearch = () -> runAsync(searchButton, "Buscando produtos...",
                () -> searchProducts(search.getText()),
                result -> {
                    products.getItems().setAll(result);
                    if (result.size() == 1) {
                        products.setValue(result.getFirst());
                        add.requestFocus();
                    } else if (!result.isEmpty()) {
                        products.show();
                    }
                    feedback.setText(result.isEmpty() ? "Nenhum produto ativo encontrado." : result.size() + " produto(s) encontrado(s).");
                });
        searchButton.setOnAction(event -> doSearch.run());
        search.setOnAction(event -> doSearch.run());

        add.setOnAction(event -> {
            CatalogProduct product = products.getValue();
            if (product == null) {
                feedback.setText("Selecione um produto.");
                return;
            }
            BigDecimal parsed = parseDecimal(quantity.getText());
            if (parsed == null || parsed.signum() <= 0) {
                feedback.setText("Informe uma quantidade maior que zero.");
                return;
            }
            cart.add(new DraftLine(product.id(), product.name(), parsed.setScale(3, RoundingMode.HALF_UP), product.salePrice()));
            quantity.setText("1");
            search.clear();
            search.requestFocus();
            refreshCart();
            feedback.setText(product.name() + " adicionado.");
        });

        Button syncCatalog = new Button("Atualizar catalogo local");
        syncCatalog.setOnAction(event -> refreshLocalCatalog());
        return panel("Produto", labeled("Buscar", new HBox(8, search, searchButton)), labeled("Resultado", products),
                labeled("Quantidade", quantity), add, syncCatalog);
    }

    private VBox customerPanel() {
        TextField search = new TextField();
        search.setPromptText("Nome, telefone ou documento");
        customers.setMaxWidth(Double.MAX_VALUE);
        Button searchButton = new Button("Buscar");
        searchButton.setOnAction(event -> {
            if (!isAuthenticated()) {
                feedback.setText("Busca de clientes exige conexao com a API.");
                return;
            }
            runAsync(searchButton, "Buscando clientes...",
                    () -> onlineSaleClient.searchCustomers(apiUrl.getText(), bearerToken, search.getText()),
                    result -> {
                        customers.getItems().setAll(result);
                        if (!result.isEmpty()) {
                            customers.show();
                        }
                        feedback.setText(result.isEmpty() ? "Nenhum cliente ativo encontrado." : "Selecione o cliente.");
                    });
        });
        Button clear = new Button("Consumidor nao identificado");
        clear.setOnAction(event -> customers.setValue(null));

        TextField name = new TextField();
        name.setPromptText("Nome");
        TextField phone = new TextField();
        phone.setPromptText("Telefone");
        CheckBox consent = new CheckBox("Aceita contato");
        Button create = new Button("Cadastrar cliente rapido");
        create.setOnAction(event -> {
            if (name.getText().isBlank()) {
                feedback.setText("Informe o nome do cliente.");
                return;
            }
            if (!isAuthenticated()) {
                feedback.setText("Cadastro de cliente exige conexao com a API.");
                return;
            }
            runAsync(create, "Cadastrando cliente...",
                    () -> onlineSaleClient.quickCreateCustomer(apiUrl.getText(), bearerToken, name.getText().trim(),
                            blankToNull(phone.getText()), null, consent.isSelected()),
                    customer -> {
                        customers.getItems().setAll(customer);
                        customers.setValue(customer);
                        name.clear();
                        phone.clear();
                        consent.setSelected(false);
                        feedback.setText("Cliente " + customer.name() + " cadastrado e vinculado a venda.");
                    });
        });
        return panel("Cliente (opcional)", labeled("Buscar", new HBox(8, search, searchButton)), labeled("Cliente", customers),
                clear, new Label("Cadastro rapido"), name, phone, consent, create);
    }

    private VBox cartPanel() {
        cartView.setPrefHeight(260);
        Button removeSelected = new Button("Remover item selecionado");
        removeSelected.setOnAction(event -> {
            int index = cartView.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < cart.size()) {
                cart.remove(index);
                refreshCart();
            } else {
                feedback.setText("Selecione um item do carrinho.");
            }
        });
        Button changeQuantity = new Button("Alterar quantidade");
        TextField newQuantity = new TextField();
        newQuantity.setPromptText("Nova qtd.");
        newQuantity.setPrefColumnCount(6);
        changeQuantity.setOnAction(event -> {
            int index = cartView.getSelectionModel().getSelectedIndex();
            BigDecimal parsed = parseDecimal(newQuantity.getText());
            if (index < 0 || index >= cart.size()) {
                feedback.setText("Selecione um item do carrinho.");
                return;
            }
            if (parsed == null || parsed.signum() <= 0) {
                feedback.setText("Informe uma quantidade maior que zero.");
                return;
            }
            DraftLine line = cart.get(index);
            cart.set(index, new DraftLine(line.productId(), line.productName(), parsed.setScale(3, RoundingMode.HALF_UP), line.unitPrice()));
            newQuantity.clear();
            refreshCart();
        });
        Button cancelSale = danger("Cancelar venda em andamento");
        cancelSale.setOnAction(event -> {
            resetSale();
            feedback.setText("Venda em andamento cancelada. Nada foi registrado.");
        });
        discount.setOnAction(event -> refreshCart());
        discount.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused) {
                refreshCart();
            }
        });
        discountHint.getStyleClass().add("hint");
        totals.getStyleClass().add("total");
        return panel("Carrinho", cartView, new HBox(8, removeSelected, newQuantity, changeQuantity),
                labeled("Desconto (R$)", discount), discountHint, totals, cancelSale);
    }

    private VBox paymentPanel() {
        method.getItems().setAll(PaymentMethod.values());
        method.setValue(PaymentMethod.CASH);
        paymentsView.setPrefHeight(110);
        Button addPayment = new Button("Adicionar pagamento");
        addPayment.setOnAction(event -> {
            BigDecimal amount = parseDecimal(paymentAmount.getText());
            if (amount == null || amount.signum() <= 0) {
                feedback.setText("Informe um valor de pagamento maior que zero.");
                return;
            }
            payments.add(new LocalSalePayment(method.getValue(), amount.setScale(2, RoundingMode.HALF_UP)));
            refreshCart();
        });
        Button removePayment = new Button("Remover pagamento");
        removePayment.setOnAction(event -> {
            int index = paymentsView.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < payments.size()) {
                payments.remove(index);
            } else if (!payments.isEmpty()) {
                payments.removeLast();
            }
            refreshCart();
        });
        Button finalizeSale = primary("Finalizar venda");
        finalizeSale.setOnAction(event -> finalizeSale(finalizeSale));
        return panel("Pagamento", new HBox(8, method, paymentAmount, addPayment), paymentsView, removePayment, finalizeSale);
    }

    private VBox receiptPanel() {
        receipt.setEditable(false);
        receipt.setPrefRowCount(16);
        receipt.setPromptText("Comprovante simples nao fiscal");
        receipt.getStyleClass().add("receipt");
        return panel("Comprovante", receipt);
    }

    private VBox queuePanel() {
        queueView.setPrefHeight(220);
        Button sync = new Button("Sincronizar agora");
        sync.setOnAction(event -> synchronize());
        return panel("Fila local", queueView, sync);
    }

    private void finalizeSale(Button button) {
        if (cart.isEmpty()) {
            feedback.setText("Adicione ao menos um item.");
            return;
        }
        BigDecimal discountValue = parseDecimal(discount.getText());
        if (discountValue == null) {
            feedback.setText("Desconto invalido.");
            return;
        }
        List<LocalSalePayment> salePayments = new ArrayList<>(payments);
        Draft preview = new Draft(customerId(), cart, discountValue, salePayments);
        if (salePayments.isEmpty()) {
            // Atalho de caixa: sem pagamentos lancados, usa a forma selecionada pelo valor total.
            BigDecimal amount = parseDecimal(paymentAmount.getText());
            salePayments.add(new LocalSalePayment(method.getValue(), amount == null ? preview.netAmount() : amount));
        }
        Draft draft = new Draft(customerId(), cart, discountValue, salePayments);
        String validation = checkoutService.validate(draft);
        if (validation != null) {
            feedback.setText(validation);
            return;
        }
        Session session = new Session(apiUrl.getText(), bearerToken);
        runAsync(button, "Registrando venda...", () -> checkoutService.checkout(session, draft), result -> {
            showResult(draft, result);
            refreshQueue();
            refreshStatus();
        });
    }

    private void showResult(Draft draft, CheckoutResult result) {
        feedback.setText(result.message());
        if (result.sale() != null && result.completed()) {
            receipt.setText(ReceiptFormatter.format(draft, result, ZoneId.systemDefault()));
            resetSale();
        }
    }

    private void resetSale() {
        cart.clear();
        payments.clear();
        discount.setText("0.00");
        customers.setValue(null);
        refreshCart();
    }

    private void refreshCart() {
        cartView.getItems().setAll(cart.stream()
                .map(line -> "%s x %s  @ %s = %s".formatted(
                        line.quantity().stripTrailingZeros().toPlainString(),
                        line.productName(),
                        ReceiptFormatter.money(line.unitPrice()),
                        ReceiptFormatter.money(line.totalAmount())))
                .toList());
        BigDecimal discountValue = parseDecimal(discount.getText());
        Draft draft = new Draft(customerId(), cart, discountValue == null ? BigDecimal.ZERO : discountValue, payments);
        paymentsView.getItems().setAll(payments.stream()
                .map(payment -> payment.method().displayName() + ": " + ReceiptFormatter.money(payment.amount()))
                .toList());
        BigDecimal remaining = draft.remainingAmount();
        paymentAmount.setText(remaining.signum() > 0 ? remaining.toPlainString() : "0.00");
        totals.setText("Subtotal %s   Desconto %s   Total %s   Falta %s".formatted(
                ReceiptFormatter.money(draft.grossAmount()),
                ReceiptFormatter.money(draft.discountAmount()),
                ReceiptFormatter.money(draft.netAmount()),
                ReceiptFormatter.money(remaining.max(BigDecimal.ZERO))));
        DiscountLimit limit = checkoutService.discountLimit();
        discountHint.setText(limit.unlimited()
                ? "Perfil sem limite de desconto."
                : limit.maxPercent().signum() == 0
                ? "Perfil sem permissao de desconto."
                : "Desconto maximo do perfil: " + limit.maxPercent().stripTrailingZeros().toPlainString() + "%.");
    }

    private void refreshStatus() {
        CompletableFuture.supplyAsync(checkoutService::currentStatus)
                .thenAccept(status -> Platform.runLater(() -> applyStatus(status)));
        refreshQueue();
    }

    private void applyStatus(SyncStatus status) {
        statusBadge.setText(status.displayName());
        statusBadge.getStyleClass().removeAll("status-online", "status-offline", "status-pending", "status-failed");
        statusBadge.getStyleClass().add(switch (status) {
            case ONLINE -> "status-online";
            case OFFLINE_READY -> "status-offline";
            case SYNC_PENDING -> "status-pending";
            case SYNC_FAILED -> "status-failed";
        });
    }

    private void refreshQueue() {
        CompletableFuture.supplyAsync(() -> offlineSaleQueue.recentSales(30))
                .thenAccept(sales -> Platform.runLater(() -> queueView.getItems().setAll(sales.stream()
                        .map(this::describe)
                        .toList())));
    }

    private String describe(LocalSaleSummary sale) {
        String label = switch (sale.status()) {
            case PENDING -> "Pendente";
            case SENT -> "Sincronizada" + (sale.remoteSaleId() == null ? "" : " #" + sale.remoteSaleId());
            case CONFLICT -> "Conflito (aguarda admin)";
            case ERROR -> "Falha, sera reenviada";
            case REJECTED -> "Recusada";
        };
        String detail = sale.lastMessage() == null || sale.status() == com.mercadoone.pdv.offline.LocalSaleStatus.SENT
                ? ""
                : " - " + sale.lastMessage();
        return "%s  %s  %s%s".formatted(TIME.format(sale.createdAt().atZone(ZoneId.systemDefault())),
                ReceiptFormatter.money(sale.netAmount()), label, detail);
    }

    private void synchronize() {
        Session session = new Session(apiUrl.getText(), bearerToken);
        if (!session.authenticated()) {
            feedback.setText("Entre com um operador para sincronizar a fila offline.");
            refreshStatus();
            return;
        }
        CompletableFuture.supplyAsync(() -> checkoutService.synchronize(session))
                .whenComplete((summary, error) -> Platform.runLater(() -> {
                    feedback.setText(error == null ? summary.describe() : "Sincronizacao falhou: " + errorMessage(error));
                    refreshStatus();
                }));
    }

    private void startBackgroundSync() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.minutes(2), event -> {
            if (isAuthenticated()) {
                Session session = new Session(apiUrl.getText(), bearerToken);
                CompletableFuture.runAsync(() -> checkoutService.synchronize(session))
                        .whenComplete((ignored, error) -> Platform.runLater(this::refreshStatus));
            } else {
                refreshStatus();
            }
        }));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void loadDiscountPolicy() {
        Session session = new Session(apiUrl.getText(), bearerToken);
        CompletableFuture.supplyAsync(() -> checkoutService.refreshDiscountLimit(session))
                .whenComplete((limit, error) -> Platform.runLater(this::refreshCart));
    }

    private List<CatalogProduct> searchProducts(String term) {
        if (!isAuthenticated()) {
            return offlineCatalogStore.search(term);
        }
        try {
            List<CatalogProduct> onlineProducts = onlineSaleClient.searchProducts(apiUrl.getText(), bearerToken, term);
            checkoutService.markApiReachable(true);
            if (term == null || term.isBlank()) {
                offlineCatalogStore.replaceAll(onlineProducts);
            }
            return onlineProducts;
        } catch (OnlineSaleClient.ApiUnavailableException exception) {
            checkoutService.markApiReachable(false);
            Platform.runLater(this::refreshStatus);
            return offlineCatalogStore.search(term);
        }
    }

    private void refreshLocalCatalog() {
        if (!isAuthenticated()) {
            feedback.setText("Entre com um operador para atualizar o catalogo local.");
            return;
        }
        CompletableFuture.supplyAsync(() -> onlineSaleClient.searchProducts(apiUrl.getText(), bearerToken, ""))
                .whenComplete((products, error) -> Platform.runLater(() -> {
                    if (error == null) {
                        offlineCatalogStore.replaceAll(products);
                        feedback.setText("Catalogo local atualizado com " + products.size() + " produto(s).");
                    } else {
                        feedback.setText("Catalogo local mantido. " + errorMessage(error));
                    }
                }));
    }

    private <T> void runAsync(Button trigger, String progress, Supplier<T> work, java.util.function.Consumer<T> onSuccess) {
        trigger.setDisable(true);
        feedback.setText(progress);
        CompletableFuture.supplyAsync(work).whenComplete((result, error) -> Platform.runLater(() -> {
            trigger.setDisable(false);
            if (error == null) {
                onSuccess.accept(result);
            } else {
                feedback.setText(errorMessage(error));
            }
        }));
    }

    private boolean isAuthenticated() {
        return bearerToken != null && !bearerToken.isBlank();
    }

    private Long customerId() {
        return customers.getValue() == null ? null : customers.getValue().id();
    }

    private static String defaultApiUrl() {
        String configured = System.getenv("MERCADO_ONE_API_URL");
        return configured == null || configured.isBlank() ? "http://localhost:8080" : configured.trim();
    }

    private static BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static VBox column(Node... children) {
        VBox column = new VBox(14, children);
        column.setPrefWidth(380);
        return column;
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("column");
        return pane;
    }

    private static VBox panel(String title, Node... children) {
        Label heading = new Label(title);
        heading.getStyleClass().add("panel-title");
        VBox box = new VBox(8);
        box.getChildren().add(heading);
        box.getChildren().addAll(children);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(14));
        return box;
    }

    private static VBox labeled(String label, Node field) {
        Label text = new Label(label);
        text.getStyleClass().add("subtitle");
        return new VBox(4, text, field);
    }

    private static Button primary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary");
        return button;
    }

    private static Button danger(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger");
        return button;
    }
}
