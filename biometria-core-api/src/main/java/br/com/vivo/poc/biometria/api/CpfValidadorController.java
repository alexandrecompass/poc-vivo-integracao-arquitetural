package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.api.dto.CpfValidacaoResponse;
import br.com.vivo.poc.biometria.application.ValidarCpfUseCase;
import br.com.vivo.poc.biometria.infrastructure.logging.CpfMasker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Validação de CPF", description = "Validação de CPF pelo dígito verificador — endpoint público, sem autenticação")
@RestController
@RequestMapping("/api/v1/cpf")
public class CpfValidadorController {

    private final ValidarCpfUseCase validarCpfUseCase;
    private final CpfMasker cpfMasker;

    public CpfValidadorController(ValidarCpfUseCase validarCpfUseCase, CpfMasker cpfMasker) {
        this.validarCpfUseCase = validarCpfUseCase;
        this.cpfMasker = cpfMasker;
    }

    @Operation(summary = "Validar CPF", description = "Valida o CPF pelo algoritmo do dígito verificador. Não requer autenticação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado da validação (válido ou inválido)"),
            @ApiResponse(responseCode = "400", description = "Formato de CPF irreconhecível")
    })
    @GetMapping("/{cpf}/validar")
    public ResponseEntity<CpfValidacaoResponse> validar(
            @Parameter(description = "CPF a validar (somente dígitos ou formatado)", example = "123.456.789-09")
            @PathVariable String cpf) {
        boolean valido = validarCpfUseCase.executar(cpf);
        String mensagem = valido ? "CPF valido." : "CPF invalido.";
        return ResponseEntity.ok(new CpfValidacaoResponse(cpfMasker.mask(cpf), valido, mensagem));
    }
}
