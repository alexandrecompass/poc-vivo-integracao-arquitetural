package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.api.dto.CpfValidacaoResponse;
import br.com.vivo.poc.biometria.application.ValidarCpfUseCase;
import br.com.vivo.poc.biometria.infrastructure.logging.CpfMasker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cpf")
public class CpfValidadorController {

    private final ValidarCpfUseCase validarCpfUseCase;
    private final CpfMasker cpfMasker;

    public CpfValidadorController(ValidarCpfUseCase validarCpfUseCase, CpfMasker cpfMasker) {
        this.validarCpfUseCase = validarCpfUseCase;
        this.cpfMasker = cpfMasker;
    }

    @GetMapping("/{cpf}/validar")
    public ResponseEntity<CpfValidacaoResponse> validar(@PathVariable String cpf) {
        boolean valido = validarCpfUseCase.executar(cpf);
        String mensagem = valido ? "CPF valido." : "CPF invalido.";
        return ResponseEntity.ok(new CpfValidacaoResponse(cpfMasker.mask(cpf), valido, mensagem));
    }
}
