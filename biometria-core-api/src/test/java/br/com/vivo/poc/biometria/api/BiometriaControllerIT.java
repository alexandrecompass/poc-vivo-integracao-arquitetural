package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.api.dto.BiometriaResponse;
import br.com.vivo.poc.biometria.api.mapper.BiometriaResponseMapper;
import br.com.vivo.poc.biometria.application.ConsultarBiometriaUseCase;
import br.com.vivo.poc.biometria.application.exception.BiometriaNaoEncontradaException;
import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.domain.OrigemBiometria;
import br.com.vivo.poc.biometria.infrastructure.config.SecurityConfig;
import br.com.vivo.poc.biometria.infrastructure.error.GlobalExceptionHandler;
import br.com.vivo.poc.biometria.infrastructure.logging.CorrelationIdFilter;
import br.com.vivo.poc.biometria.infrastructure.logging.RequestLoggingFilter;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import br.com.vivo.poc.biometria.infrastructure.security.GatewaySecretFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BiometriaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, GatewaySecretFilter.class,
        CorrelationIdFilter.class, RequestLoggingFilter.class})
@TestPropertySource(properties = "internal.security.gateway-secret=gateway-internal-secret-poc")
class BiometriaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarBiometriaUseCase consultarBiometriaUseCase;

    @MockBean
    private BiometriaResponseMapper mapper;

    @MockBean
    private BiometriaMetrics metrics;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void deveRetornar200QuandoTokenESecretForemValidos() throws Exception {
        Biometria biometria = new Biometria(Cpf.of("52998224725"), true, "img", OrigemBiometria.LEGADO_SOAP,
                LocalDateTime.parse("2026-06-30T12:00:00"));
        when(consultarBiometriaUseCase.executar(any())).thenReturn(biometria);
        when(mapper.toResponse(biometria)).thenReturn(
                new BiometriaResponse("*********25", true, "img", "LEGADO_SOAP", "2026-06-30T12:00:00Z",
                        "correlation-id"));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometriaDisponivel").value(true));
    }

    @Test
    void deveRetornar401SemBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    @Test
    void deveRetornar403SemGatewaySecret() throws Exception {
        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    void deveRetornar400ParaCpfInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/biometria/123")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF_INVALIDO"));
    }

    @Test
    void deveRetornar404QuandoBiometriaNaoEncontrada() throws Exception {
        when(consultarBiometriaUseCase.executar(any()))
                .thenThrow(new BiometriaNaoEncontradaException("Biometria nao encontrada para o CPF informado."));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BIOMETRIA_NAO_ENCONTRADA"));
    }

    @Test
    void deveRetornar502QuandoLegadoEstiverIndisponivel() throws Exception {
        when(consultarBiometriaUseCase.executar(any()))
                .thenThrow(new LegadoIndisponivelException("indisponivel"));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("LEGADO_INDISPONIVEL"));
    }

    @Test
    void deveRetornar504QuandoLegadoTimeout() throws Exception {
        when(consultarBiometriaUseCase.executar(any()))
                .thenThrow(new LegadoTimeoutException("timeout"));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("TIMEOUT_LEGADO"));
    }

    @Test
    void deveGerarCorrelationIdQuandoAusente() throws Exception {
        Biometria biometria = new Biometria(Cpf.of("52998224725"), true, "img", OrigemBiometria.LEGADO_SOAP,
                LocalDateTime.parse("2026-06-30T12:00:00"));
        when(consultarBiometriaUseCase.executar(any())).thenReturn(biometria);
        when(mapper.toResponse(biometria)).thenReturn(
                new BiometriaResponse("*********25", true, "img", "LEGADO_SOAP", "2026-06-30T12:00:00Z",
                        "correlation-id"));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void devePropagarCorrelationIdQuandoInformado() throws Exception {
        Biometria biometria = new Biometria(Cpf.of("52998224725"), true, "img", OrigemBiometria.LEGADO_SOAP,
                LocalDateTime.parse("2026-06-30T12:00:00"));
        when(consultarBiometriaUseCase.executar(any())).thenReturn(biometria);
        when(mapper.toResponse(biometria)).thenReturn(
                new BiometriaResponse("*********25", true, "img", "LEGADO_SOAP", "2026-06-30T12:00:00Z",
                        "correlation-id"));

        mockMvc.perform(get("/api/v1/biometria/52998224725")
                        .with(jwt().authorities(() -> "SCOPE_biometria:read"))
                        .header("X-Gateway-Secret", "gateway-internal-secret-poc")
                        .header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"));
    }
}
