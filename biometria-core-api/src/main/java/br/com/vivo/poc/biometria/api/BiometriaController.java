package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.api.dto.BiometriaResponse;
import br.com.vivo.poc.biometria.api.mapper.BiometriaResponseMapper;
import br.com.vivo.poc.biometria.application.ConsultarBiometriaUseCase;
import br.com.vivo.poc.biometria.application.exception.CpfInvalidoException;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@RequestMapping("/api/v1/biometria")
public class BiometriaController {

    private static final Logger log = LoggerFactory.getLogger(BiometriaController.class);
    private static final Set<String> LEGACY_SIMULATION_CPFS = Set.of("00000000000", "99988877766");

    private final ConsultarBiometriaUseCase consultarBiometriaUseCase;
    private final BiometriaResponseMapper responseMapper;
    private final BiometriaMetrics metrics;

    public BiometriaController(ConsultarBiometriaUseCase consultarBiometriaUseCase,
            BiometriaResponseMapper responseMapper, BiometriaMetrics metrics) {
        this.consultarBiometriaUseCase = consultarBiometriaUseCase;
        this.responseMapper = responseMapper;
        this.metrics = metrics;
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<BiometriaResponse> consultar(@PathVariable String cpf) {
        String digits = Cpf.sanitize(cpf);
        boolean cpfValido = Cpf.isValido(digits);
        if (!cpfValido && !LEGACY_SIMULATION_CPFS.contains(digits)) {
            log.warn("cpf_validation_failed",
                    kv("event", "cpf_validation_failed"),
                    kv("cpf", cpf == null ? "***" : cpf.replaceAll("\\d(?=\\d{2})", "*")));
            throw new CpfInvalidoException(cpf);
        }

        Cpf cpfValueObject = cpfValido ? Cpf.of(digits) : Cpf.fromTrustedDigits(digits);
        BiometriaResponse response = metrics.registrarApiLatencia(() ->
                responseMapper.toResponse(consultarBiometriaUseCase.executar(cpfValueObject)));
        return ResponseEntity.ok(response);
    }
}
