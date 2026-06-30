package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Component
public class SoapFaultTranslator {

    public RuntimeException traduzir(SoapFaultClientException ex) {
        String faultString = ex.getFaultStringOrReason();
        if (faultString != null && faultString.contains("BIOMETRIA_NAO_ENCONTRADA")) {
            return new BiometriaNaoEncontradaException("Biometria nao encontrada para o CPF informado.");
        }
        return new LegadoIndisponivelException("SOAP Fault: " + faultString, ex);
    }
}
