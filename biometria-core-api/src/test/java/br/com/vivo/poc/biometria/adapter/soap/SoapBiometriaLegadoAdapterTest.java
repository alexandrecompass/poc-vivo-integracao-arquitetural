package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.domain.OrigemBiometria;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoapBiometriaLegadoAdapterTest {

    @Mock
    private SoapBiometriaClient client;

    @Mock
    private SoapBiometriaMapper mapper;

    @Mock
    private SoapFaultTranslator faultTranslator;

    @Mock
    private BiometriaMetrics metrics;

    @InjectMocks
    private SoapBiometriaLegadoAdapter adapter;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(metrics).registrarLegadoLatencia(any());
    }

    @Test
    void deveRetornarBiometriaQuandoClienteResponder() {
        Cpf cpf = Cpf.of("52998224725");
        Object result = new Object();
        Biometria biometria = new Biometria(cpf, true, "img", OrigemBiometria.LEGADO_SOAP, LocalDateTime.now());
        when(client.consultar(cpf.getValor())).thenReturn(result);
        when(mapper.toDomain(result, cpf)).thenReturn(biometria);

        Biometria retorno = adapter.consultarPorCpf(cpf);

        assertThat(retorno).isSameAs(biometria);
    }

    @Test
    void deveTraduzirSoapFault() {
        Cpf cpf = Cpf.of("52998224725");
        SoapFaultClientException fault = org.mockito.Mockito.mock(SoapFaultClientException.class);
        RuntimeException translated = new LegadoIndisponivelException("fault");
        when(client.consultar(cpf.getValor())).thenThrow(fault);
        when(faultTranslator.traduzir(fault)).thenReturn(translated);

        assertThatThrownBy(() -> adapter.consultarPorCpf(cpf))
                .isSameAs(translated);
    }

    @Test
    void deveConverterRuntimeExceptionGenericaParaIndisponibilidade() {
        Cpf cpf = Cpf.of("52998224725");
        when(client.consultar(cpf.getValor())).thenThrow(new RuntimeException("erro"));

        assertThatThrownBy(() -> adapter.consultarPorCpf(cpf))
                .isInstanceOf(LegadoIndisponivelException.class)
                .hasMessage("erro");
    }
}
