package br.com.vivo.poc.biometria.infrastructure.error;

import br.com.vivo.poc.biometria.api.dto.ErrorResponse;
import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.CpfInvalidoException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CpfInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleCpfInvalido(CpfInvalidoException ex, HttpServletRequest request) {
        return buildError(400, "CPF_INVALIDO", ex.getMessage(), request);
    }

    @ExceptionHandler(BiometriaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrada(BiometriaNaoEncontradaException ex,
            HttpServletRequest request) {
        return buildError(404, "BIOMETRIA_NAO_ENCONTRADA", ex.getMessage(), request);
    }

    @ExceptionHandler(LegadoTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(LegadoTimeoutException ex, HttpServletRequest request) {
        log.error("legado_timeout", kv("event", "legado_timeout"), kv("message", ex.getMessage()));
        return buildError(504, "TIMEOUT_LEGADO",
                "O servico legado demorou mais do que o esperado para responder.", request);
    }

    @ExceptionHandler(LegadoIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleIndisponivel(LegadoIndisponivelException ex,
            HttpServletRequest request) {
        log.error("legado_indisponivel", kv("event", "legado_indisponivel"), kv("message", ex.getMessage()));
        return buildError(502, "LEGADO_INDISPONIVEL",
                "Nao foi possivel consultar a biometria neste momento.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("erro_interno", kv("event", "erro_interno"), kv("message", ex.getMessage()));
        return buildError(500, "ERRO_INTERNO", "Erro interno inesperado.", request);
    }

    private ResponseEntity<ErrorResponse> buildError(int status, String error, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                request.getRequestURI(),
                MDC.get("correlationId")
        ));
    }
}
