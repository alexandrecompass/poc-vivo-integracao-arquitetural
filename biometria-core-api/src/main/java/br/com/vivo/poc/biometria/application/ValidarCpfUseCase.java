package br.com.vivo.poc.biometria.application;

import br.com.vivo.poc.biometria.domain.Cpf;
import org.springframework.stereotype.Service;

@Service
public class ValidarCpfUseCase {

    public boolean executar(String rawCpf) {
        String digits = Cpf.sanitize(rawCpf);
        if (digits.length() == 11 && digits.chars().distinct().count() == 1) {
            return false;
        }
        return Cpf.isValido(digits);
    }
}
