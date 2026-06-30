package br.com.vivo.poc.biometria.api.dto;

public record BiometriaResponse(
        String cpf,
        boolean biometriaDisponivel,
        String imagemBase64,
        String origem,
        String dataConsulta,
        String correlationId
) {
}
