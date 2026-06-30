package br.com.vivo.poc.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityConfig.class);

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/cpf/**").permitAll()
                        .pathMatchers("/management/**", "/monitoring/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .pathMatchers("/auth/token").permitAll()
                        .pathMatchers("/api/v1/biometria/**").hasAuthority("SCOPE_biometria:read")
                        .anyExchange().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("gateway_token_invalid",
                                    kv("event", "gateway_token_invalid"),
                                    kv("path", exchange.getRequest().getPath().value()),
                                    kv("correlationId", exchange.getAttribute("correlationId")));
                            return writeJsonError(exchange, HttpStatus.UNAUTHORIZED,
                                    "Token ausente, invalido ou expirado.");
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            log.warn("gateway_scope_denied",
                                    kv("event", "gateway_scope_denied"),
                                    kv("path", exchange.getRequest().getPath().value()),
                                    kv("correlationId", exchange.getAttribute("correlationId")));
                            return writeJsonError(exchange, HttpStatus.FORBIDDEN,
                                    "Escopo biometria:read obrigatorio.");
                        }))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwkSetUri(jwkSetUri)))
                .build();
    }

    private Mono<Void> writeJsonError(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] body = ("{\"status\":" + status.value() + ",\"error\":\"" + status.name()
                + "\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
