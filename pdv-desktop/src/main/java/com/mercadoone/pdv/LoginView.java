package com.mercadoone.pdv;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Tela de entrada do operador (visual P3a da identidade: card de 440px sobre o degradê da marca).
 * O PDV só mostra a tela de venda depois que esta tela aceita o operador, online ou offline.
 *
 * <p>A tela não decide nada: entrega login e senha a quem a criou e mostra o resultado.
 */
final class LoginView extends StackPane {

    /** Chamado com login e senha já preenchidos. A senha é entregue como char[] para quem chamou apagar. */
    interface SubmitHandler {
        void submit(String login, char[] password);
    }

    private final ImageView logo = new ImageView();
    private final TextField login = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField passwordVisible = new TextField();
    private final Label loginError = fieldError();
    private final Label passwordError = fieldError();
    private final Label alertText = new Label();
    private final HBox alert = new HBox(10);
    private final Label notice = new Label();
    private final Button toggleSecret = new Button("Mostrar");
    private final Button submit = new Button("Entrar");
    private final VBox forgotNote = new VBox();
    private final VBox connectionBox = new VBox(6);
    private final SubmitHandler handler;
    private boolean busy;

    LoginView(Image logoImage, TextField apiUrl, String defaultLogin, SubmitHandler handler) {
        this.handler = handler;
        getStyleClass().add("login-page");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40, 24, 40, 24));

        logo.setImage(logoImage);
        logo.setFitHeight(56);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        Label title = new Label("Bem-vindo de volta");
        title.getStyleClass().add("login-title");
        title.setFocusTraversable(false);
        Label subtitle = new Label("Entre para abrir o caixa.");
        subtitle.getStyleClass().add("login-subtitle");
        VBox head = new VBox(6, logo, title, subtitle);
        head.setAlignment(Pos.CENTER);
        head.getStyleClass().add("login-head");

        alertText.setWrapText(true);
        alertText.getStyleClass().add("login-alert-text");
        Label alertIcon = new Label("!");
        alertIcon.getStyleClass().add("login-alert-icon");
        alert.getChildren().addAll(alertIcon, alertText);
        alert.getStyleClass().add("login-alert");
        alert.setAlignment(Pos.TOP_LEFT);
        setShown(alert, false);

        notice.setWrapText(true);
        notice.getStyleClass().add("login-notice");
        setShown(notice, false);

        login.setText(defaultLogin == null ? "" : defaultLogin);
        login.setAccessibleText("Login");
        login.getStyleClass().add("login-input");
        login.setOnAction(event -> password.requestFocus());
        login.textProperty().addListener((observable, before, after) -> clearFieldError(login, loginError));

        password.setAccessibleText("Senha");
        password.getStyleClass().addAll("login-input", "login-secret");
        passwordVisible.setAccessibleText("Senha");
        passwordVisible.getStyleClass().addAll("login-input", "login-secret");
        passwordVisible.textProperty().bindBidirectional(password.textProperty());
        setShown(passwordVisible, false);
        password.setOnAction(event -> attempt());
        passwordVisible.setOnAction(event -> attempt());
        password.textProperty().addListener((observable, before, after) -> clearFieldError(password, passwordError));

        toggleSecret.getStyleClass().add("login-eye");
        toggleSecret.setFocusTraversable(true);
        toggleSecret.setAccessibleText("Mostrar senha");
        toggleSecret.setOnAction(event -> toggleSecret());
        StackPane secret = new StackPane(password, passwordVisible, toggleSecret);
        StackPane.setAlignment(toggleSecret, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleSecret, new Insets(0, 6, 0, 0));

        Hyperlink forgot = new Hyperlink("Esqueci a senha");
        forgot.getStyleClass().add("login-link");
        forgot.setOnAction(event -> setShown(forgotNote, !forgotNote.isManaged()));
        Label senhaLabel = label("Senha");
        HBox senhaRow = new HBox(senhaLabel, spacer(), forgot);
        senhaRow.setAlignment(Pos.BASELINE_LEFT);

        Label forgotText = new Label("Se o seu e-mail está cadastrado, abra o painel administrativo, escolha Esqueci a senha e "
                + "siga o link que chegar por e-mail. Se não, peça ao administrador do mercado para redefinir a sua senha "
                + "em Usuários, Redefinir senha. A senha nova vale aqui depois da próxima entrada com internet.");
        forgotText.setWrapText(true);
        forgotText.getStyleClass().add("login-note");
        forgotNote.getChildren().add(forgotText);
        setShown(forgotNote, false);

        VBox loginField = new VBox(6, label("Login ou e-mail"), login, loginError);
        VBox passwordField = new VBox(6, senhaRow, secret, passwordError);

        submit.getStyleClass().addAll("primary", "login-submit");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> attempt());

        Hyperlink connection = new Hyperlink("Endereço do servidor");
        connection.getStyleClass().add("login-link");
        Label apiLabel = label("Endereço da API");
        apiUrl.getStyleClass().add("login-input");
        connectionBox.getChildren().addAll(apiLabel, apiUrl);
        setShown(connectionBox, false);
        connection.setOnAction(event -> setShown(connectionBox, !connectionBox.isManaged()));
        HBox footer = new HBox(connection);
        footer.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, head, alert, notice, loginField, passwordField, forgotNote, submit, footer, connectionBox);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(440);
        // O StackPane estica os filhos; o card tem de ficar do tamanho do conteudo.
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setPadding(new Insets(36, 36, 28, 36));
        getChildren().add(card);
    }

    void setLogo(Image image) {
        logo.setImage(image);
    }

    /** Foco no primeiro campo que falta preencher; é para onde o operador quer ir ao abrir a tela. */
    void focusFirstField() {
        Platform.runLater(() -> (login.getText().isBlank() ? login : password).requestFocus());
    }

    /** Volta ao estado inicial depois de uma saída: senha apagada, erros limpos, foco no login. */
    void reset() {
        password.clear();
        login.clear();
        hideAlert();
        setNotice(null);
        clearFieldError(login, loginError);
        clearFieldError(password, passwordError);
        setBusy(false);
        showSecret(false);
        focusFirstField();
    }

    void showError(String message) {
        alertText.setText(message);
        setShown(alert, true);
        alert.setAccessibleText(message);
        password.clear();
        password.requestFocus();
    }

    /** Aviso neutro (por exemplo, a sessão que acabou de ser encerrada). Some quando o operador tenta de novo. */
    void setNotice(String message) {
        notice.setText(message == null ? "" : message);
        setShown(notice, message != null && !message.isBlank());
    }

    void setBusy(boolean value) {
        busy = value;
        submit.setDisable(value);
        login.setEditable(!value);
        password.setEditable(!value);
        passwordVisible.setEditable(!value);
        submit.setText(value ? "Entrando..." : "Entrar");
    }

    private void attempt() {
        if (busy) {
            return;
        }
        boolean noLogin = login.getText().isBlank();
        boolean noPassword = password.getText().isEmpty();
        showFieldError(login, loginError, noLogin ? "Informe o login." : null);
        showFieldError(password, passwordError, noPassword ? "Informe a senha." : null);
        if (noLogin || noPassword) {
            // Teclado e leitor de tela vão direto ao primeiro campo com problema.
            (noLogin ? login : password).requestFocus();
            return;
        }
        hideAlert();
        setNotice(null);
        handler.submit(login.getText().trim(), password.getText().toCharArray());
    }

    private void toggleSecret() {
        showSecret(!passwordVisible.isManaged());
    }

    private void showSecret(boolean visible) {
        setShown(passwordVisible, visible);
        setShown(password, !visible);
        toggleSecret.setText(visible ? "Ocultar" : "Mostrar");
        toggleSecret.setAccessibleText(visible ? "Ocultar senha" : "Mostrar senha");
        (visible ? passwordVisible : password).requestFocus();
    }

    private void hideAlert() {
        setShown(alert, false);
    }

    private static void showFieldError(TextField field, Label label, String message) {
        label.setText(message == null ? "" : message);
        setShown(label, message != null);
        if (message == null) {
            field.getStyleClass().remove("login-input-invalid");
        } else if (!field.getStyleClass().contains("login-input-invalid")) {
            field.getStyleClass().add("login-input-invalid");
        }
    }

    private static void clearFieldError(TextField field, Label label) {
        showFieldError(field, label, null);
    }

    private static Label fieldError() {
        Label label = new Label();
        label.getStyleClass().add("login-field-error");
        label.setWrapText(true);
        setShown(label, false);
        return label;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("login-label");
        return label;
    }

    private static Node spacer() {
        javafx.scene.layout.Region region = new javafx.scene.layout.Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
