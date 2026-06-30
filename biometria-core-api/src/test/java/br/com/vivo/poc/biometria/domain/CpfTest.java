package br.com.vivo.poc.biometria.domain;

import br.com.vivo.poc.biometria.application.exception.CpfInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void deveValidarCpfsDoSeed() {
        assertThat(Cpf.isValido("52998224725")).isTrue();
        assertThat(Cpf.isValido("11144477735")).isTrue();
        assertThat(Cpf.isValido("87126398870")).isTrue();
        assertThat(Cpf.isValido("56872389510")).isTrue();
        assertThat(Cpf.isValido("04896019630")).isTrue();
        assertThat(Cpf.isValido("72687789120")).isTrue();
        assertThat(Cpf.isValido("98473126540")).isTrue();
        assertThat(Cpf.isValido("01546532755")).isTrue();
        assertThat(Cpf.isValido("31279940582")).isTrue();
        assertThat(Cpf.isValido("45261897366")).isTrue();
    }

    @Test
    void deveRejeitarCpfsInvalidos() {
        assertThat(Cpf.isValido("00000000000")).isFalse();
        assertThat(Cpf.isValido("11111111111")).isFalse();
        assertThat(Cpf.isValido("12345678900")).isFalse();
        assertThat(Cpf.isValido("99999999999")).isFalse();
        assertThat(Cpf.isValido("123")).isFalse();
        assertThat(Cpf.isValido("")).isFalse();
        assertThat(Cpf.isValido(null)).isFalse();
        assertThat(Cpf.isValido("abc12345678")).isFalse();
        assertThat(Cpf.isValido("1234567890")).isFalse();
    }

    @Test
    void deveCriarObjetoComCpfValido() {
        Cpf cpf = Cpf.of("529.982.247-25");

        assertThat(cpf.getValor()).isEqualTo("52998224725");
    }

    @Test
    void deveLancarExcecaoParaCpfInvalido() {
        assertThatThrownBy(() -> Cpf.of("123"))
                .isInstanceOf(CpfInvalidoException.class)
                .hasMessage("CPF invalido. Informe um CPF com 11 digitos numericos.");
    }

    @Test
    void deveMascararCpfNoToString() {
        assertThat(Cpf.of("52998224725").toString()).isEqualTo("*********25");
    }
}
