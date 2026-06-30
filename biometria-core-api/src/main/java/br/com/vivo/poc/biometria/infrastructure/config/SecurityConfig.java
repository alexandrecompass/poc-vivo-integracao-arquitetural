package br.com.vivo.poc.biometria.infrastructure.config;

import br.com.vivo.poc.biometria.api.dto.ErrorResponse;
import br.com.vivo.poc.biometria.infrastructure.security.GatewaySecretFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class SecurityConfig {

    private final GatewaySecretFilter gatewaySecretFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(GatewaySecretFilter gatewaySecretFilter, ObjectMapper objectMapper) {
        this.gatewaySecretFilter = gatewaySecretFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * Permite que o Springdoc (OpenAPI spec) use o Host real do Gateway (:8080)
     * ao invés do host interno do Core API (:8082) ao gerar a URL do servidor.
     * Sem este filtro, o Swagger UI tentaria chamar localhost:8082 diretamente → CORS.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/cpf/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/biometria/**").hasAuthority("SCOPE_biometria:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "NAO_AUTENTICADO",
                                        "Token de acesso ausente, invalido ou expirado."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, request, HttpServletResponse.SC_FORBIDDEN, "ACESSO_NEGADO",
                                        "O token informado nao possui o escopo necessario: biometria:read.")));
        http.addFilterBefore(gatewaySecretFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, int status, String error,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                request.getRequestURI(),
                MDC.get("correlationId")
        )));
    }
}
