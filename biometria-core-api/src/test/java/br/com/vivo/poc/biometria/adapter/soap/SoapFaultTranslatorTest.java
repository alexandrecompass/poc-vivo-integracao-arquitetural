package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.ws.soap.client.SoapFaultClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoapFaultTranslatorTest {

    private final SoapFaultTranslator translator = new SoapFaultTranslator();

    @Test
    void deveTraduzirFaultDeNaoEncontrada() {
        SoapFaultClientException exception = mock(SoapFaultClientException.class);
        when(exception.getFaultStringOrReason()).thenReturn("BIOMETRIA_NAO_ENCONTRADA");

        RuntimeException traduzida = translator.traduzir(exception);

        assertThat(traduzida).isInstanceOf(BiometriaNaoEncontradaException.class);
    }

    @Test
    void deveTraduzirFaultGenericoComoIndisponibilidade() {
        SoapFaultClientException exception = mock(SoapFaultClientException.class);
        when(exception.getFaultStringOrReason()).thenReturn("FAULT_GENERICO");

        RuntimeException traduzida = translator.traduzir(exception);

        assertThat(traduzida).isInstanceOf(LegadoIndisponivelException.class);
    }
}
