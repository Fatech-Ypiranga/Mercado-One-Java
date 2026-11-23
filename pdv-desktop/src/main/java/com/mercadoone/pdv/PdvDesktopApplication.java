package com.mercadoone.pdv;

import com.mercadoone.pdv.offline.OfflineCatalogStore;
import com.mercadoone.pdv.offline.OfflineSaleQueue;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSalePayment;
import com.mercadoone.pdv.offline.OfflineSaleQueue.LocalSaleSummary;
import com.mercadoone.pdv.offline.OfflineStorageConfig;
import com.mercadoone.pdv.offline.OperatorCredentialStore;
import com.mercadoone.pdv.sales.BarcodeScan;
import com.mercadoone.pdv.sales.CartLines;
import com.mercadoone.pdv.sales.CheckoutService;
import com.mercadoone.pdv.sales.CheckoutService.CheckoutResult;
import com.mercadoone.pdv.sales.CheckoutService.Draft;
import com.mercadoone.pdv.sales.CheckoutService.DraftLine;
import com.mercadoone.pdv.sales.CheckoutService.Session;
import com.mercadoone.pdv.sales.OnlineSaleClient;
import com.mercadoone.pdv.sales.OperatorAccess;
import com.mercadoone.pdv.sales.OperatorAccess.AccessDeniedException;
import com.mercadoone.pdv.sales.OperatorAccess.Entry;
import com.mercadoone.pdv.sales.ProductSearchService;
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
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

public class PdvDesktopApplication extends Application {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String DARK_THEME_KEY = "temaEscuro";
    private static final String LAST_OPERATOR_KEY = "ultimoOperador";

    private final OfflineStorageConfig storageConfig = OfflineStorageConfig.defaultConfig();
    private final OfflineSaleQueue offlineSaleQueue = new OfflineSaleQueue(storageConfig);
    private final OfflineCatalogStore offlineCatalogStore = new OfflineCatalogStore(storageConfig);
    private final OnlineSaleClient onlineSaleClient = new OnlineSaleClient();
    private final OperatorCredentialStore credentialStore = new OperatorCredentialStore(storageConfig);
    private final OperatorAccess operatorAccess = new OperatorAccess(onlineSaleClient, credentialStore);
    private final CheckoutService checkoutService = new CheckoutService(offlineSaleQueue, onlineSaleClient);
    private final ProductSearchService productSearch = new ProductSearchService(offlineCatalogStore, onlineSaleClient, checkoutService);
    private final List<DraftLine> cart = new ArrayList<>();
    private final List<LocalSalePayment> payments = new ArrayList<>();
    private volatile String bearerToken = "";
    /** Quem está no caixa. Vazio na tela de entrada. A venda offline é gravada com este login. */
    private volatile String operatorLogin = "";
    private volatile String operatorName = "";
    /** Senha digitada na entrada, só em memória, para pegar o token quando a API voltar após entrada offline. */
    private char[] sessionPassword;
    /** Falso na tela de entrada: o leitor de código de barras não pode desviar o que se digita no login. */
    private boolean inSession;
    private BorderPane rootPane;
    private HBox header;
    private Node saleBody;
    private LoginView loginView;

    private final Image logoColor = new Image(getClass().getResource("/brand/mercadoone-simbolo-192.png").toExternalForm());
    private final Image logoNegative = new Image(getClass().getResource("/brand/mercadoone-simbolo-negativa-192.png").toExternalForm());
    private final ImageView headerLogo = new ImageView();
    private final TextField apiUrl = new TextField(defaultApiUrl());
    private final Label statusBadge = new Label();
    private final Label operatorLabel = new Label("Sem operador");
    private final Label feedback = new Label("Leia o código de barras ou busque o produto.");
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
    /** Campo "Buscar" do painel de produto: é para onde vai tudo o que o leitor de código de barras digita. */
    private TextField productSearchField;

    public static void main(String[] args) {
        launch(args);
    }

    /** Modo escuro escolhido pelo operador neste computador (padrão: claro). */
    private static boolean darkTheme() {
        return Preferences.userNodeForPackage(PdvDesktopApplication.class).getBoolean(DARK_THEME_KEY, false);
    }

    @Override
    public void start(Stage stage) {
        offlineSaleQueue.initialize();
        offlineCatalogStore.initialize();
        credentialStore.initialize();
        loadBrandFonts();
        stage.setTitle("mercadoOne PDV");
        stage.getIcons().add(new Image(getClass().getResource("/brand/icone-512.png").toExternalForm()));
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        Scene scene = new Scene(root, 1280, 780);
        rootPane = root;
        header = createHeader(root);
        saleBody = createBody();
        loginView = new LoginView(darkTheme() ? logoNegative : logoColor, apiUrl, lastOperator(), this::attemptLogin);
        // Tokens da marca (gerados de docs/marca/tokens.json), depois o visual do PDV.
        scene.getStylesheets().add(stylesheet("brand-tokens.css"));
        scene.getStylesheets().add(stylesheet("pdv.css"));
        applyTheme(root, darkTheme());
        scene.addEventFilter(KeyEvent.KEY_TYPED, event -> redirectScannerInput(scene, event));
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.show();
        showLogin();
        refreshCart();
        refreshStatus();
        startBackgroundSync();
    }

    /** Outfit, a fonte da marca (licença OFL em resources/fonts). Sem ela, o JavaFX cai na fonte do sistema. */
    private void loadBrandFonts() {
        for (String weight : List.of("Regular", "SemiBold", "Bold")) {
            try (var in = getClass().getResourceAsStream("/fonts/Outfit-" + weight + ".ttf")) {
                if (in != null) Font.loadFont(in, 14);
            } catch (java.io.IOException exception) {
                // Fonte ausente não impede o caixa de vender.
            }
        }
    }

    private String stylesheet(String name) {
        return getClass().getResource("/styles/" + name).toExternalForm();
    }

    /**
     * Modo escuro: as variáveis -mo-* de styles/brand-tokens-dark.css (gerado de docs/marca/tokens.json)
     * vão como estilo direto no nó raiz. Folha de estilo extra ou classe ".dark" não redefinem
     * variáveis de forma confiável no JavaFX; o estilo direto tem prioridade sobre qualquer folha.
     */
    private void applyTheme(Node root, boolean dark) {
        root.setStyle(dark ? darkTokens() : "");
        // Logo sem fundo: versão negativa (branca) no modo escuro.
        headerLogo.setImage(dark ? logoNegative : logoColor);
        if (loginView != null) {
            loginView.setLogo(dark ? logoNegative : logoColor);
        }
    }

    private String darkTokens() {
        try (var in = getClass().getResourceAsStream("/styles/brand-tokens-dark.css")) {
            String css = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            css = css.replaceAll("(?s)/\\*.*?\\*/", "");
            return css.substring(css.indexOf('{') + 1, css.lastIndexOf('}')).replaceAll("\\s+", " ").trim();
        } catch (java.io.IOException | RuntimeException exception) {
            return "";
        }
    }

    private HBox createHeader(Node root) {
        headerLogo.setFitHeight(32);
        headerLogo.setPreserveRatio(true);
        headerLogo.setSmooth(true);
        headerLogo.setImage(darkTheme() ? logoNegative : logoColor);
        Label brandMercado = new Label("mercado");
        brandMercado.getStyleClass().add("brand");
        Label brandOne = new Label("One");
        brandOne.getStyleClass().add("brand-one");
        Label brandSuffix = new Label("PDV");
        brandSuffix.getStyleClass().add("brand-suffix");
        HBox brand = new HBox(0, brandMercado, brandOne, brandSuffix);
        brand.setAlignment(Pos.BASELINE_LEFT);
        brand.setAccessibleText("Mercado One PDV");
        HBox brandRow = new HBox(10, headerLogo, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        statusBadge.getStyleClass().add("status");
        operatorLabel.getStyleClass().add("subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label storage = new Label("Base local: " + storageConfig.databasePath());
        storage.getStyleClass().add("hint");
        Button themeButton = new Button(darkTheme() ? "Modo claro" : "Modo escuro");
        themeButton.setOnAction(event -> {
            boolean dark = root.getStyle().isEmpty();
            applyTheme(root, dark);
            themeButton.setText(dark ? "Modo claro" : "Modo escuro");
            Preferences.userNodeForPackage(PdvDesktopApplication.class).putBoolean(DARK_THEME_KEY, dark);
        });
        Button logoutButton = new Button("Sair");
        logoutButton.setOnAction(event -> logout());
        HBox header = new HBox(16, brandRow, statusBadge, operatorLabel, spacer, storage, themeButton, logoutButton);
        header.setPadding(new Insets(14, 22, 14, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private Node createBody() {
        VBox left = column(productPanel(), customerPanel());
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

    private static String lastOperator() {
        return Preferences.userNodeForPackage(PdvDesktopApplication.class).get(LAST_OPERATOR_KEY, "");
    }

    /** Tela de entrada: sem cabeçalho nem venda. Só sai daqui com um operador aceito, online ou offline. */
    private void showLogin() {
        inSession = false;
        rootPane.setTop(null);
        rootPane.setCenter(loginView);
        loginView.focusFirstField();
    }

    private void showSale() {
        inSession = true;
        rootPane.setTop(header);
        rootPane.setCenter(saleBody);
        Platform.runLater(this::focusProductSearch);
    }

    private void attemptLogin(String login, char[] password) {
        loginView.setBusy(true);
        CompletableFuture.supplyAsync(() -> operatorAccess.enter(apiUrl.getText(), login, password))
                .whenComplete((entry, error) -> Platform.runLater(() -> {
                    loginView.setBusy(false);
                    if (error == null) {
                        enter(entry, password);
                    } else {
                        Arrays.fill(password, '\0');
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        loginView.showError(cause instanceof AccessDeniedException
                                ? cause.getMessage()
                                : "Não foi possível entrar agora. Tente de novo.");
                    }
                }));
    }

    private void enter(Entry entry, char[] password) {
        bearerToken = entry.token();
        operatorLogin = entry.login();
        operatorName = entry.name();
        sessionPassword = password;
        Preferences.userNodeForPackage(PdvDesktopApplication.class).put(LAST_OPERATOR_KEY, entry.login());
        operatorLabel.setText("Operador: " + entry.name() + (entry.online() ? "" : " (sem conexão)"));
        loginView.reset();
        showSale();
        if (entry.online()) {
            checkoutService.markApiReachable(true);
            feedback.setText("Operador autenticado. Atualizando catalogo e fila offline...");
            refreshLocalCatalog();
            loadDiscountPolicy();
            synchronize();
        } else {
            checkoutService.markApiReachable(false);
            feedback.setText("Entrou sem conexão. As vendas ficam na fila e seguem sozinhas quando o sistema voltar.");
            refreshStatus();
        }
        refreshCart();
    }

    private void logout() {
        if (!cart.isEmpty()) {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Há itens no carrinho. Sair descarta a venda em andamento.",
                    javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            confirm.setHeaderText("Sair do caixa?");
            if (confirm.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).isEmpty()) {
                return;
            }
        }
        bearerToken = "";
        operatorLogin = "";
        operatorName = "";
        if (sessionPassword != null) {
            Arrays.fill(sessionPassword, '\0');
            sessionPassword = null;
        }
        operatorLabel.setText("Sem operador");
        cart.clear();
        payments.clear();
        discount.setText("0.00");
        customers.setValue(null);
        receipt.clear();
        refreshCart();
        refreshStatus();
        showLogin();
        loginView.setNotice("Sessão encerrada. As vendas feitas ficam na fila e seguem no próximo login.");
    }

    private Session session() {
        return new Session(apiUrl.getText(), bearerToken, operatorLogin);
    }

    /**
     * Garante o token antes de falar com a API. Se o operador entrou sem conexão, tenta obter o token agora
     * com a senha da entrada (só em memória). Sem token, avisa e deixa as vendas na fila.
     */
    private void withConnection(boolean quiet, Runnable whenReady) {
        if (isAuthenticated()) {
            whenReady.run();
            return;
        }
        char[] secret = sessionPassword;
        if (!inSession || secret == null || operatorLogin.isBlank()) {
            if (!quiet) {
                feedback.setText("Entre com um operador para sincronizar a fila offline.");
            }
            refreshStatus();
            return;
        }
        String login = operatorLogin;
        CompletableFuture.supplyAsync(() -> operatorAccess.reconnect(apiUrl.getText(), login, secret))
                .thenAccept(token -> Platform.runLater(() -> {
                    if (token.isPresent() && inSession && login.equals(operatorLogin)) {
                        bearerToken = token.get();
                        operatorLabel.setText("Operador: " + operatorName);
                        checkoutService.markApiReachable(true);
                        loadDiscountPolicy();
                        refreshLocalCatalog();
                        whenReady.run();
                    } else {
                        if (!quiet) {
                            feedback.setText("Sem conexão com o sistema. As vendas seguem na fila.");
                        }
                        refreshStatus();
                    }
                }));
    }

    private VBox productPanel() {
        TextField search = new TextField();
        search.setPromptText("Codigo de barras, SKU ou nome");
        ComboBox<CatalogProduct> products = new ComboBox<>();
        products.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField("1");
        Button searchButton = new Button("Buscar");
        Button add = primary("Adicionar item");

        productSearchField = search;

        // Leitor de código de barras: ele "digita" o código e aperta Enter. Se o código bate com um único
        // produto, o item entra sozinho no carrinho e o campo fica livre para a próxima leitura.
        Runnable doSearch = () -> {
            String term = search.getText() == null ? "" : search.getText().trim();
            boolean scanned = BarcodeScan.looksLikeBarcode(term);
            if (scanned) {
                // Limpa já: se a próxima leitura chegar antes da resposta, os códigos não se misturam.
                search.clear();
            }
            BigDecimal wantedQuantity = parseDecimal(quantity.getText());
            runAsync(searchButton, "Buscando produtos...",
                    () -> searchProducts(term),
                    result -> {
                        Optional<CatalogProduct> exact = BarcodeScan.exactMatch(term, result);
                        if (exact.isPresent()) {
                            BigDecimal amount = wantedQuantity == null || wantedQuantity.signum() <= 0 ? BigDecimal.ONE : wantedQuantity;
                            if (!scanned) {
                                search.clear(); // SKU digitado à mão; leitura de código já limpou o campo no Enter
                            }
                            addToCart(exact.get(), amount);
                            quantity.setText("1");
                            products.getItems().clear();
                            return;
                        }
                        if (result.isEmpty() && scanned) {
                            feedback.setText("Codigo " + term + " nao encontrado. Confira o cadastro ou leia de novo.");
                            search.setText(term);
                            search.selectAll();
                            search.requestFocus();
                            return;
                        }
                        products.getItems().setAll(result);
                        if (result.size() == 1) {
                            products.setValue(result.getFirst());
                            add.requestFocus();
                        } else if (!result.isEmpty()) {
                            products.show();
                        }
                        feedback.setText(result.isEmpty() ? "Nenhum produto ativo encontrado." : result.size() + " produto(s) encontrado(s).");
                    });
        };
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
            search.clear();
            addToCart(product, parsed);
            quantity.setText("1");
        });

        Button syncCatalog = new Button("Atualizar catalogo local");
        syncCatalog.setOnAction(event -> refreshLocalCatalog());
        return panel("Produto", labeled("Buscar", new HBox(8, search, searchButton)), labeled("Resultado", products),
                labeled("Quantidade", quantity), add, syncCatalog);
    }

    /**
     * Põe o produto no carrinho (somando se ele já estiver lá) e devolve o foco à busca.
     *
     * <p>Não limpa o campo de busca de propósito: quando a resposta de uma leitura chega, o leitor já pode
     * estar digitando o código da próxima, e limpar aqui apagaria os primeiros dígitos dela. A limpeza é
     * feita por quem chamou, no momento certo.
     */
    private void addToCart(CatalogProduct product, BigDecimal amount) {
        CartLines.add(cart, new DraftLine(product.id(), product.name(), amount.setScale(3, RoundingMode.HALF_UP), product.salePrice()));
        refreshCart();
        feedback.setText(product.name() + " adicionado.");
        focusProductSearch();
    }

    private void focusProductSearch() {
        if (productSearchField != null) {
            productSearchField.requestFocus();
        }
    }

    /**
     * O leitor digita no campo que estiver com o foco. Se o foco estiver em outro lugar que não aceita
     * texto (botão, lista, carrinho), os caracteres lidos iriam se perder; aqui eles são desviados para
     * o campo de busca de produto. Campos de texto (login, cliente, desconto...) não são afetados.
     */
    private void redirectScannerInput(Scene scene, KeyEvent event) {
        if (!inSession || productSearchField == null) {
            return;
        }
        Node owner = scene.getFocusOwner();
        if (owner == productSearchField || owner instanceof TextInputControl
                || (owner instanceof ComboBox<?> combo && combo.isEditable())) {
            return;
        }
        String typed = event.getCharacter();
        if (typed == null || typed.length() != 1 || !isScannableCharacter(typed.charAt(0))) {
            return;
        }
        productSearchField.requestFocus();
        productSearchField.appendText(typed);
        event.consume();
    }

    private static boolean isScannableCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '.' || c == '/' || c == '_';
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
        totals.setWrapText(true);
        return panel("Carrinho", cartView, wrapRow(removeSelected, newQuantity, changeQuantity),
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
        method.setMinWidth(150);
        return panel("Pagamento", wrapRow(method, paymentAmount, addPayment), paymentsView, removePayment, finalizeSale);
    }

    /** Linha de controles que quebra para a próxima linha em vez de cortar o texto dos botões. */
    private FlowPane wrapRow(Node... nodes) {
        for (Node node : nodes) {
            if (node instanceof Button button) button.setMinWidth(Region.USE_PREF_SIZE);
        }
        FlowPane row = new FlowPane(8, 8, nodes);
        row.setPrefWrapLength(0);
        return row;
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
        Session session = session();
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
        focusProductSearch();
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
        discountHint.setText(!isAuthenticated()
                ? "Sem conexão: o desconto só vale depois que o sistema voltar."
                : limit.unlimited()
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
        withConnection(false, this::synchronizeNow);
    }

    private void synchronizeNow() {
        Session session = session();
        CompletableFuture.supplyAsync(() -> checkoutService.synchronize(session))
                .whenComplete((summary, error) -> Platform.runLater(() -> {
                    feedback.setText(error == null ? summary.describe() : "Sincronizacao falhou: " + errorMessage(error));
                    refreshStatus();
                }));
    }

    private void startBackgroundSync() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.minutes(2), event -> {
            if (!inSession) {
                return;
            }
            withConnection(true, () -> {
                Session session = session();
                CompletableFuture.runAsync(() -> checkoutService.synchronize(session))
                        .whenComplete((ignored, error) -> Platform.runLater(this::refreshStatus));
            });
        }));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void loadDiscountPolicy() {
        Session session = session();
        CompletableFuture.supplyAsync(() -> checkoutService.refreshDiscountLimit(session))
                .whenComplete((limit, error) -> Platform.runLater(this::refreshCart));
    }

    private List<CatalogProduct> searchProducts(String term) {
        List<CatalogProduct> products = productSearch.search(session(), term);
        Platform.runLater(this::refreshStatus);
        return products;
    }

    private void refreshLocalCatalog() {
        if (!isAuthenticated()) {
            feedback.setText("Entre com um operador para atualizar o catalogo local.");
            return;
        }
        Session session = session();
        CompletableFuture.supplyAsync(() -> productSearch.refreshLocalCatalog(session))
                .whenComplete((count, error) -> Platform.runLater(() -> feedback.setText(error == null
                        ? "Catalogo local atualizado com " + count + " produto(s)."
                        : "Catalogo local mantido. " + errorMessage(error))));
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
