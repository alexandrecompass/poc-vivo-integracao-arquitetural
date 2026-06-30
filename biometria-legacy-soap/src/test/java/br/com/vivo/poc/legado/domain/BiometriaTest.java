package br.com.vivo.poc.legado.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BiometriaTest {

    @Test
    void deveExporTodosOsCamposViaGetters() {
        LocalDateTime dataRegistro = LocalDateTime.now();
        Biometria biometria = new Biometria("52998224725", true, "aW1hZ2Vt", dataRegistro);

        assertThat(biometria.getCpf()).isEqualTo("52998224725");
        assertThat(biometria.isBiometriaDisponivel()).isTrue();
        assertThat(biometria.getImagemBase64()).isEqualTo("aW1hZ2Vt");
        assertThat(biometria.getDataRegistro()).isEqualTo(dataRegistro);
        assertThat(biometria.getId()).isNull();
    }

    @Test
    void deveCompararPorIdentidade() {
        Biometria biometria1 = new Biometria("52998224725", true, "aW1hZ2Vt", LocalDateTime.now());
        ReflectionTestUtils.setField(biometria1, "id", 1L);

        Biometria biometria2 = new Biometria("11144477735", true, "b3V0cm8=", LocalDateTime.now());
        ReflectionTestUtils.setField(biometria2, "id", 1L);

        Biometria biometria3 = new Biometria("87126398870", true, "dGVyY2Vpcm8=", LocalDateTime.now());
        ReflectionTestUtils.setField(biometria3, "id", 2L);

        assertThat(biometria1).isEqualTo(biometria1);
        assertThat(biometria1).isEqualTo(biometria2);
        assertThat(biometria1).hasSameHashCodeAs(biometria2);
        assertThat(biometria1).isNotEqualTo(biometria3);
        assertThat(biometria1).isNotEqualTo(null);
        assertThat(biometria1).isNotEqualTo("nao-e-uma-biometria");
    }
}
