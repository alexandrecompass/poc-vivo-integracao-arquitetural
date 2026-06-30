package br.com.vivo.poc.biometria.application.exception;

public class CpfInvalidoException extends RuntimeException {

    public CpfInvalidoException(String rawCpf) {
        super("CPF invalido. Informe um CPF com 11 digitos numericos.");
    }
}
