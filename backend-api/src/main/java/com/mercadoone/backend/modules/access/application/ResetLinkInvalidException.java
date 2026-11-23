package com.mercadoone.backend.modules.access.application;

/** Link de redefinicao inexistente, ja usado ou vencido. A mensagem nao diz qual dos tres. */
public class ResetLinkInvalidException extends RuntimeException {

    public ResetLinkInvalidException() {
        super("Link invalido ou expirado.");
    }
}
