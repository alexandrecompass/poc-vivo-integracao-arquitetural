package br.com.vivo.poc.legado.service;

public class BiometriaNaoEncontradaException extends RuntimeException {

    public BiometriaNaoEncontradaException(String message) {
        super(message);
    }
}
