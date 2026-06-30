package br.com.vivo.poc.biometria.port;

import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;

public interface BiometriaLegadoPort {

    Biometria consultarPorCpf(Cpf cpf);
}
