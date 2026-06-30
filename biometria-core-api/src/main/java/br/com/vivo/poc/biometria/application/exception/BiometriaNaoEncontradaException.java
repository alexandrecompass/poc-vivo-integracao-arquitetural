package br.com.vivo.poc.biometria.application.exception;

public class BiometriaNaoEncontradaException extends RuntimeException {

    public BiometriaNaoEncontradaException(String message) {
        super(message);
    }
}
