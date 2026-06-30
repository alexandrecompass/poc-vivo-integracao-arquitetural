package br.com.vivo.poc.biometria.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfMaskerTest {

    private final CpfMasker masker = new CpfMasker();

    @Test
    void deveMascararCpfValido() {
        assertThat(masker.mask("52998224725")).isEqualTo("*********25");
    }

    @Test
    void deveRetornarMascaraPadraoParaEntradasCurtas() {
        assertThat(masker.mask(null)).isEqualTo("***");
        assertThat(masker.mask("")).isEqualTo("***");
        assertThat(masker.mask("1")).isEqualTo("***");
    }

    @Test
    void deveRetornarDoisUltimosDigitosSemMascaraQuandoTamanhoForDois() {
        assertThat(masker.mask("12")).isEqualTo("12");
    }
}
