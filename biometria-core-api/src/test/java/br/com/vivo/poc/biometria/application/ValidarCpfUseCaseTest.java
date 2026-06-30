package br.com.vivo.poc.biometria.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidarCpfUseCaseTest {

    private final ValidarCpfUseCase useCase = new ValidarCpfUseCase();

    @Test
    void deveRetornarTrueParaCpfValido() {
        assertThat(useCase.executar("52998224725")).isTrue();
    }

    @Test
    void deveRetornarFalseParaCpfInvalido() {
        assertThat(useCase.executar("00000000000")).isFalse();
        assertThat(useCase.executar("123")).isFalse();
        assertThat(useCase.executar(null)).isFalse();
    }

    @Test
    void deveAceitarCpfComPontuacao() {
        assertThat(useCase.executar("529.982.247-25")).isTrue();
    }
}
