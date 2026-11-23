package com.mercadoone.backend.modules.access.application;

/** A redefinicao por e-mail nao esta configurada neste ambiente. */
public class ResetUnavailableException extends RuntimeException {

    public ResetUnavailableException() {
        super("A redefinicao de senha por e-mail nao esta configurada. Fale com o administrador.");
    }
}
