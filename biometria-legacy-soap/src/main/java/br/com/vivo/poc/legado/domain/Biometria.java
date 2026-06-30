package br.com.vivo.poc.legado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "BIOMETRIA")
public class Biometria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;

    @Column(name = "biometria_disponivel", nullable = false)
    private boolean biometriaDisponivel = true;

    @Column(name = "imagem_base64")
    private String imagemBase64;

    @Column(name = "data_registro")
    private LocalDateTime dataRegistro;

    protected Biometria() {
    }

    public Biometria(String cpf, boolean biometriaDisponivel, String imagemBase64, LocalDateTime dataRegistro) {
        this.cpf = cpf;
        this.biometriaDisponivel = biometriaDisponivel;
        this.imagemBase64 = imagemBase64;
        this.dataRegistro = dataRegistro;
    }

    public Long getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public boolean isBiometriaDisponivel() {
        return biometriaDisponivel;
    }

    public String getImagemBase64() {
        return imagemBase64;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Biometria that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
