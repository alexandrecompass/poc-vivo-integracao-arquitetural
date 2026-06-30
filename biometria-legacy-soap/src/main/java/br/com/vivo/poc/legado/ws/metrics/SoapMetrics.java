package br.com.vivo.poc.legado.ws.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class SoapMetrics {

    private final Counter requestsTotal;
    private final Counter requestsBlocked;
    private final Counter requestsSuccess;
    private final Counter requestsNotFound;
    private final Timer dbLatencia;

    public SoapMetrics(MeterRegistry registry) {
        this.requestsTotal = Counter.builder("soap.requests.total")
                .description("Total de requisições SOAP recebidas")
                .tag("service", "biometria-legacy-soap")
                .register(registry);

        this.requestsBlocked = Counter.builder("soap.requests.blocked")
                .description("Requisições bloqueadas pelo InternalSecretInterceptor")
                .tag("service", "biometria-legacy-soap")
                .register(registry);

        this.requestsSuccess = Counter.builder("soap.requests.success")
                .description("Requisições SOAP respondidas com sucesso")
                .tag("service", "biometria-legacy-soap")
                .register(registry);

        this.requestsNotFound = Counter.builder("soap.requests.not_found")
                .description("CPF não encontrado no banco")
                .tag("service", "biometria-legacy-soap")
                .register(registry);

        this.dbLatencia = Timer.builder("soap.db.latencia")
                .description("Latência da consulta ao H2")
                .tag("service", "biometria-legacy-soap")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);
    }

    public void incrementarTotal() {
        requestsTotal.increment();
    }

    public void incrementarBloqueada() {
        requestsBlocked.increment();
    }

    public void incrementarSucesso() {
        requestsSuccess.increment();
    }

    public void incrementarNaoEncontrada() {
        requestsNotFound.increment();
    }

    public <T> T registrarDbLatencia(Supplier<T> fn) {
        return dbLatencia.record(fn);
    }
}
