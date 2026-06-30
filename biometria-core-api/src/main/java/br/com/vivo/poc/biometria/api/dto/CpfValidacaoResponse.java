package br.com.vivo.poc.biometria.api.dto;

public record CpfValidacaoResponse(
        String cpf,
        boolean valido,
        String mensagem
) {
}
