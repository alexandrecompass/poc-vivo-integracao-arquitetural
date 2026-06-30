package br.com.vivo.poc.biometria.application;

import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.domain.OrigemBiometria;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import br.com.vivo.poc.biometria.port.BiometriaLegadoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarBiometriaUseCaseTest {

    @Mock
    private BiometriaLegadoPort legadoPort;

    @Mock
    private BiometriaMetrics metrics;

    @InjectMocks
    private ConsultarBiometriaUseCase useCase;

    @Test
    void deveRetornarBiometriaQuandoLegadoResponder() {
        Cpf cpf = Cpf.of("52998224725");
        Biometria biometria = new Biometria(cpf, true, "img", OrigemBiometria.LEGADO_SOAP, LocalDateTime.now());
        when(legadoPort.consultarPorCpf(cpf)).thenReturn(biometria);

        Biometria resultado = useCase.executar(cpf);

        assertThat(resultado).isSameAs(biometria);
        verify(metrics).incrementarSucesso();
        verify(metrics).incrementarTotal();
    }

    @Test
    void deveRelancarQuandoBiometriaNaoEncontrada() {
        Cpf cpf = Cpf.of("52998224725");
        when(legadoPort.consultarPorCpf(cpf)).thenThrow(new BiometriaNaoEncontradaException("nao encontrada"));

        assertThatThrownBy(() -> useCase.executar(cpf))
                .isInstanceOf(BiometriaNaoEncontradaException.class);

        verify(metrics).incrementarNaoEncontrada();
        verify(metrics).incrementarTotal();
    }

    @Test
    void deveRelancarQuandoLegadoIndisponivel() {
        Cpf cpf = Cpf.of("52998224725");
        when(legadoPort.consultarPorCpf(cpf)).thenThrow(new LegadoIndisponivelException("indisponivel"));

        assertThatThrownBy(() -> useCase.executar(cpf))
                .isInstanceOf(LegadoIndisponivelException.class);

        verify(metrics).incrementarFalhaLegado();
        verify(metrics).incrementarTotal();
    }

    @Test
    void deveRelancarQuandoLegadoTimeout() {
        Cpf cpf = Cpf.of("52998224725");
        when(legadoPort.consultarPorCpf(cpf)).thenThrow(new LegadoTimeoutException("timeout"));

        assertThatThrownBy(() -> useCase.executar(cpf))
                .isInstanceOf(LegadoTimeoutException.class);

        verify(metrics).incrementarFalhaLegado();
        verify(metrics).incrementarTotal();
    }
}
