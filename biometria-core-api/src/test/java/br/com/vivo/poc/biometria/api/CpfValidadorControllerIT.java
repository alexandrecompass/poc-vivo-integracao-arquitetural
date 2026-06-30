package br.com.vivo.poc.biometria.api;

import br.com.vivo.poc.biometria.application.ValidarCpfUseCase;
import br.com.vivo.poc.biometria.infrastructure.config.SecurityConfig;
import br.com.vivo.poc.biometria.infrastructure.error.GlobalExceptionHandler;
import br.com.vivo.poc.biometria.infrastructure.logging.CorrelationIdFilter;
import br.com.vivo.poc.biometria.infrastructure.logging.CpfMasker;
import br.com.vivo.poc.biometria.infrastructure.logging.RequestLoggingFilter;
import br.com.vivo.poc.biometria.infrastructure.security.GatewaySecretFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CpfValidadorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, GatewaySecretFilter.class,
        CorrelationIdFilter.class, RequestLoggingFilter.class, CpfMasker.class})
@TestPropertySource(properties = "internal.security.gateway-secret=gateway-internal-secret-poc")
class CpfValidadorControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ValidarCpfUseCase validarCpfUseCase;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void deveValidarCpfValidoSemToken() throws Exception {
        when(validarCpfUseCase.executar("52998224725")).thenReturn(true);

        mockMvc.perform(get("/api/v1/cpf/52998224725/validar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true));
    }

    @Test
    void deveValidarCpfInvalidoSemToken() throws Exception {
        when(validarCpfUseCase.executar("00000000000")).thenReturn(false);

        mockMvc.perform(get("/api/v1/cpf/00000000000/validar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(false));
    }

    @Test
    void deveRetornarFalseParaCpfCurto() throws Exception {
        when(validarCpfUseCase.executar("123")).thenReturn(false);

        mockMvc.perform(get("/api/v1/cpf/123/validar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.cpf").exists());
    }
}
