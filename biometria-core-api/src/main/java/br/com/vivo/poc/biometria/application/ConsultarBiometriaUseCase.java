package br.com.vivo.poc.biometria.application;

import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import br.com.vivo.poc.biometria.port.BiometriaLegadoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class ConsultarBiometriaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarBiometriaUseCase.class);

    private final BiometriaLegadoPort legadoPort;
    private final BiometriaMetrics metrics;

    public ConsultarBiometriaUseCase(BiometriaLegadoPort legadoPort, BiometriaMetrics metrics) {
        this.legadoPort = legadoPort;
        this.metrics = metrics;
    }

    public Biometria executar(Cpf cpf) {
        log.info("usecase_started",
                kv("event", "usecase_started"),
                kv("cpf", cpf.toString()),
                kv("correlationId", MDC.get("correlationId")));
        try {
            Biometria biometria = legadoPort.consultarPorCpf(cpf);
            metrics.incrementarSucesso();
            log.info("usecase_finished",
                    kv("event", "usecase_finished"),
                    kv("cpf", cpf.toString()),
                    kv("correlationId", MDC.get("correlationId")));
            return biometria;
        } catch (BiometriaNaoEncontradaException ex) {
            metrics.incrementarNaoEncontrada();
            log.warn("biometria_not_found",
                    kv("event", "biometria_not_found"),
                    kv("cpf", cpf.toString()),
                    kv("correlationId", MDC.get("correlationId")));
            throw ex;
        } catch (LegadoIndisponivelException | LegadoTimeoutException ex) {
            metrics.incrementarFalhaLegado();
            log.error("legado_falha",
                    kv("event", "legado_falha"),
                    kv("cpf", cpf.toString()),
                    kv("correlationId", MDC.get("correlationId")),
                    kv("message", ex.getMessage()));
            throw ex;
        } finally {
            metrics.incrementarTotal();
        }
    }
}
