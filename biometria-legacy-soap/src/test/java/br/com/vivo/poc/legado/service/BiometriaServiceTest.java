package br.com.vivo.poc.legado.service;

import br.com.vivo.poc.legado.domain.Biometria;
import br.com.vivo.poc.legado.logging.CpfMasker;
import br.com.vivo.poc.legado.repository.BiometriaRepository;
import br.com.vivo.poc.legado.ws.metrics.SoapMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiometriaServiceTest {

    @Mock
    private BiometriaRepository repository;

    private final CpfMasker cpfMasker = new CpfMasker();
    private final SoapMetrics metrics = new SoapMetrics(new SimpleMeterRegistry());

    @Test
    void deveRetornarBiometriaQuandoCpfEncontrado() {
        Biometria biometria = new Biometria("52998224725", true, "aW1hZ2VtLWZpY3RpY2lhLTUyOTk4", LocalDateTime.now());
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(biometria));

        BiometriaService service = new BiometriaService(repository, cpfMasker, metrics);

        Biometria resultado = service.consultarPorCpf("52998224725");

        assertThat(resultado).isEqualTo(biometria);
        assertThat(resultado.getCpf()).isEqualTo("52998224725");
        assertThat(resultado.isBiometriaDisponivel()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoCpfNaoEncontrado() {
        when(repository.findByCpf("00000000000")).thenReturn(Optional.empty());

        BiometriaService service = new BiometriaService(repository, cpfMasker, metrics);

        assertThatThrownBy(() -> service.consultarPorCpf("00000000000"))
                .isInstanceOf(BiometriaNaoEncontradaException.class);
    }
}
