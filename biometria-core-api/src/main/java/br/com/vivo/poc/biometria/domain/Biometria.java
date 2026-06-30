package br.com.vivo.poc.biometria.domain;

import java.time.LocalDateTime;

public final class Biometria {

    private final Cpf cpf;
    private final boolean biometriaDisponivel;
    private final String imagemBase64;
    private final OrigemBiometria origem;
    private final LocalDateTime dataConsulta;

    public Biometria(Cpf cpf, boolean biometriaDisponivel, String imagemBase64, OrigemBiometria origem,
            LocalDateTime dataConsulta) {
        this.cpf = cpf;
        this.biometriaDisponivel = biometriaDisponivel;
        this.imagemBase64 = imagemBase64;
        this.origem = origem;
        this.dataConsulta = dataConsulta;
    }

    public Cpf getCpf() {
        return cpf;
    }

    public boolean isBiometriaDisponivel() {
        return biometriaDisponivel;
    }

    public String getImagemBase64() {
        return imagemBase64;
    }

    public OrigemBiometria getOrigem() {
        return origem;
    }

    public LocalDateTime getDataConsulta() {
        return dataConsulta;
    }
}
