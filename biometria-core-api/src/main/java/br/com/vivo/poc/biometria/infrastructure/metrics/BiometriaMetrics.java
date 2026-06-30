package br.com.vivo.poc.biometria.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class BiometriaMetrics {

    private final Counter consultasTotal;
    private final Counter consultasSucesso;
    private final Counter consultasNaoEncontrada;
    private final Counter legadoFalhas;
    private final Timer legadoLatencia;
    private final Timer apiLatencia;

    public BiometriaMetrics(MeterRegistry registry) {
        String service = "biometria-core-api";

        this.consultasTotal = Counter.builder("biometria.consultas.total")
                .description("Total de consultas de biometria")
                .tag("service", service)
                .register(registry);

        this.consultasSucesso = Counter.builder("biometria.consultas.sucesso")
                .description("Consultas com retorno 200")
                .tag("service", service)
                .register(registry);

        this.consultasNaoEncontrada = Counter.builder("biometria.consultas.nao_encontrada")
                .description("CPF nao encontrado no legado")
                .tag("service", service)
                .register(registry);

        this.legadoFalhas = Counter.builder("biometria.legado.falhas")
                .description("Falhas tecnicas ao consultar o SOAP")
                .tag("service", service)
                .register(registry);

        this.legadoLatencia = Timer.builder("biometria.legado.latencia")
                .description("Latencia da chamada SOAP")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .tag("service", service)
                .register(registry);

        this.apiLatencia = Timer.builder("biometria.api.latencia")
                .description("Latencia total do endpoint REST")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .tag("service", service)
                .register(registry);
    }

    public void incrementarTotal() {
        consultasTotal.increment();
    }

    public void incrementarSucesso() {
        consultasSucesso.increment();
    }

    public void incrementarNaoEncontrada() {
        consultasNaoEncontrada.increment();
    }

    public void incrementarFalhaLegado() {
        legadoFalhas.increment();
    }

    public <T> T registrarLegadoLatencia(Supplier<T> fn) {
        return legadoLatencia.record(fn);
    }

    public <T> T registrarApiLatencia(Supplier<T> fn) {
        return apiLatencia.record(fn);
    }
}
