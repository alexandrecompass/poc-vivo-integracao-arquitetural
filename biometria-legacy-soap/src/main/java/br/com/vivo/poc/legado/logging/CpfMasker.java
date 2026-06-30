package br.com.vivo.poc.legado.logging;

import org.springframework.stereotype.Component;

@Component
public class CpfMasker {

    private static final int DIGITOS_VISIVEIS = 2;

    public String mask(String cpf) {
        if (cpf == null || cpf.length() <= DIGITOS_VISIVEIS) {
            return "*".repeat(9) + "**";
        }
        int inicioVisivel = cpf.length() - DIGITOS_VISIVEIS;
        return "*".repeat(inicioVisivel) + cpf.substring(inicioVisivel);
    }
}
