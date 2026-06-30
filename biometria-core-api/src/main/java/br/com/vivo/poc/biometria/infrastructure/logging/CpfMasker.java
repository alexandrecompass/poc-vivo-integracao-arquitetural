package br.com.vivo.poc.biometria.infrastructure.logging;

import org.springframework.stereotype.Component;

@Component
public class CpfMasker {

    public String mask(String cpf) {
        String digits = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() == 1) {
            return "***";
        }
        if (digits.length() == 2) {
            return digits;
        }
        return "*".repeat(digits.length() - 2) + digits.substring(digits.length() - 2);
    }
}
