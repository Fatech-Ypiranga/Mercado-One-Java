package com.mercadoone.pdv;

/**
 * Ponto de entrada do pacote distribuivel. Com JavaFX no classpath (sem module-path), a classe main
 * nao pode estender {@link javafx.application.Application}; por isso o jpackage usa este launcher.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        PdvDesktopApplication.main(args);
    }
}
