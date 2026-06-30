package br.com.vivo.poc.biometria.application.exception;

public class LegadoTimeoutException extends RuntimeException {

    public LegadoTimeoutException(String message) {
        super(message);
    }

    public LegadoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
