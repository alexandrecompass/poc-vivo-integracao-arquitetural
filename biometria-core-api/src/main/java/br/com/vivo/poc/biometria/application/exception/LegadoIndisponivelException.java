package br.com.vivo.poc.biometria.application.exception;

public class LegadoIndisponivelException extends RuntimeException {

    public LegadoIndisponivelException(String message) {
        super(message);
    }

    public LegadoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
