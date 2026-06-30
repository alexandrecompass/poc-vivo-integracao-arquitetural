package br.com.vivo.poc.legado.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfMaskerTest {

    private final CpfMasker cpfMasker = new CpfMasker();

    @Test
    void deveMascararMantendoApenasOsDoisUltimosDigitos() {
        assertThat(cpfMasker.mask("52998224725")).isEqualTo("*********25");
    }

    @Test
    void deveTratarCpfNuloOuCurtoComMascaraPadrao() {
        assertThat(cpfMasker.mask(null)).isEqualTo("*********" + "**");
        assertThat(cpfMasker.mask("1")).isEqualTo("*********" + "**");
    }
}
