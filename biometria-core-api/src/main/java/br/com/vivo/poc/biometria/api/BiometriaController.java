package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.api.dto.BiometriaResponse;
import br.com.vivo.poc.biometria.api.mapper.BiometriaResponseMapper;
import br.com.vivo.poc.biometria.application.ConsultarBiometriaUseCase;
import br.com.vivo.poc.biometria.application.exception.CpfInvalidoException;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Tag(name = "Biometria Facial", description = "Consulta de dados biométricos faciais de clientes Vivo (dado sensível — LGPD Art. 11)")
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

    @Operation(
            summary = "Consultar biometria facial por CPF",
            description = """
                    Retorna os dados de biometria facial do cliente identificado pelo CPF.

                    **Segurança:** Requer escopo `biometria:read`. CPF é mascarado nos logs (LGPD Art. 11).

                    CPFs de teste disponíveis no seed H2: `12345678909`, `98765432100`
                    """,
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "oauth2", scopes = {"biometria:read"})
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Biometria encontrada",
                    content = @Content(schema = @Schema(implementation = BiometriaResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "cpf": "*********09",
                                      "status": "ATIVO",
                                      "tipoVerificacao": "FACIAL",
                                      "dataUltimaVerificacao": "2024-01-15T10:30:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "CPF inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Escopo biometria:read ausente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Biometria não encontrada para o CPF", content = @Content),
            @ApiResponse(responseCode = "503", description = "Legado SOAP indisponível", content = @Content)
    })
    @GetMapping("/{cpf}")
    public ResponseEntity<BiometriaResponse> consultar(
            @Parameter(description = "CPF do cliente (somente dígitos ou formatado com pontos e traço)",
                    example = "123.456.789-09")
            @PathVariable String cpf) {
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
