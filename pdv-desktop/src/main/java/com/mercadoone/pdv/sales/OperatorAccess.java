package com.mercadoone.pdv.sales;

import com.mercadoone.pdv.offline.OperatorCredentialStore;
import com.mercadoone.pdv.sales.OnlineSaleClient.ApiRejectedException;
import com.mercadoone.pdv.sales.OnlineSaleClient.ApiUnavailableException;
import com.mercadoone.pdv.sales.OnlineSaleClient.OperatorLogin;

import java.util.Optional;

/**
 * Entrada do operador no caixa. Com a API no ar, a senha é conferida pelo servidor. Com a API fora do ar,
 * só entra quem já entrou online neste computador, conferido pela credencial local (hash, nunca a senha).
 * Se o servidor respondeu e recusou, não há plano B: senha errada é senha errada, com ou sem internet.
 */
public class OperatorAccess {

    public enum Mode {
        /** Autenticado na API: tem token, vende e sincroniza na hora. */
        ONLINE,
        /** Aceito pela credencial local: vende para a fila e sincroniza quando a API voltar. */
        OFFLINE
    }

    /** Resultado de uma entrada aceita. {@code token} é vazio no modo offline. */
    public record Entry(Mode mode, String login, String name, String token) {
        public boolean online() {
            return mode == Mode.ONLINE;
        }
    }

    /** Entrada recusada. A mensagem já é a que o operador lê na tela. */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    static final String WRONG_CREDENTIALS = "Login ou senha incorretos. Confira os dados e tente de novo.";
    static final String NO_CONNECTION_NO_ACCESS =
            "Sem conexão com o sistema. Sem internet só entra quem já entrou neste caixa com internet.";

    private final OnlineSaleClient client;
    private final OperatorCredentialStore credentials;

    public OperatorAccess(OnlineSaleClient client, OperatorCredentialStore credentials) {
        this.client = client;
        this.credentials = credentials;
    }

    public Entry enter(String apiBaseUrl, String login, char[] password) {
        String typedLogin = login == null ? "" : login.trim();
        if (typedLogin.isEmpty() || password == null || password.length == 0) {
            throw new AccessDeniedException(WRONG_CREDENTIALS);
        }
        try {
            OperatorLogin result = client.authenticate(apiBaseUrl, typedLogin, new String(password));
            String name = result.name().isBlank() ? typedLogin : result.name();
            // Renova a credencial local: a senha trocada no admin passa a valer offline daqui em diante.
            credentials.remember(typedLogin, name, password);
            return new Entry(Mode.ONLINE, typedLogin, name, result.token());
        } catch (ApiRejectedException rejected) {
            throw new AccessDeniedException(WRONG_CREDENTIALS);
        } catch (ApiUnavailableException unavailable) {
            Optional<String> name = credentials.verify(typedLogin, password);
            if (name.isPresent()) {
                return new Entry(Mode.OFFLINE, typedLogin, name.get(), "");
            }
            throw new AccessDeniedException(credentials.knows(typedLogin) ? WRONG_CREDENTIALS : NO_CONNECTION_NO_ACCESS);
        }
    }

    /**
     * Com o operador dentro em modo offline, tenta obter o token na API (a senha digitada fica só na memória
     * da sessão). Devolve vazio se a API ainda estiver fora do ar ou recusar a senha.
     */
    public Optional<String> reconnect(String apiBaseUrl, String login, char[] password) {
        try {
            OperatorLogin result = client.authenticate(apiBaseUrl, login, new String(password));
            credentials.remember(login, result.name().isBlank() ? login : result.name(), password);
            return Optional.of(result.token());
        } catch (ApiRejectedException | ApiUnavailableException exception) {
            return Optional.empty();
        }
    }
}
