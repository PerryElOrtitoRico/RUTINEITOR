package com.rutina.service.calculo;

/** Los parámetros del mesociclo no permiten calcularlo; el mensaje está pensado para mostrarse al usuario. */
public class ParametrosInvalidosException extends Exception {

    public ParametrosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
