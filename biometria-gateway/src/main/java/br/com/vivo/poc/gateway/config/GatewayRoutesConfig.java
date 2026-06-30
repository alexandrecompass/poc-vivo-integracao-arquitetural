package br.com.vivo.poc.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    private final String gatewaySecret;
    private final String coreApiUrl;
    private final String legacySoapUrl;
    private final String prometheusUrl;
    private final String grafanaUrl;
    private final String zipkinUrl;
    private final String keycloakUrl;

    public GatewayRoutesConfig(@Value("${gateway.secret}") String gatewaySecret,
            @Value("${downstream.core-api.url}") String coreApiUrl,
            @Value("${downstream.legacy-soap.url}") String legacySoapUrl,
            @Value("${downstream.prometheus.url}") String prometheusUrl,
            @Value("${downstream.grafana.url}") String grafanaUrl,
            @Value("${downstream.zipkin.url}") String zipkinUrl,
            @Value("${downstream.keycloak.url}") String keycloakUrl) {
        this.gatewaySecret = gatewaySecret;
        this.coreApiUrl = coreApiUrl;
        this.legacySoapUrl = legacySoapUrl;
        this.prometheusUrl = prometheusUrl;
        this.grafanaUrl = grafanaUrl;
        this.zipkinUrl = zipkinUrl;
        this.keycloakUrl = keycloakUrl;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // ── Swagger UI e OpenAPI spec (sem JWT — acesso público via Gateway) ──
                .route("swagger-ui", r -> r
                        .path("/swagger-ui.html", "/swagger-ui/**")
                        .uri(coreApiUrl))
                .route("openapi-docs", r -> r
                        .path("/api-docs/**")
                        .uri(coreApiUrl))
                // ── Proxy do token Keycloak — elimina CORS no Swagger UI OAuth2 ──
                // Swagger UI (localhost:8080) chama /auth/token (mesma origem)
                // Gateway repassa para Keycloak internamente → sem CORS
                .route("keycloak-token", r -> r
                        .path("/auth/token")
                        .filters(f -> f.rewritePath("/auth/token",
                                "/realms/vivo-poc/protocol/openid-connect/token"))
                        .uri(keycloakUrl))
                // ──────────────────────────────────────────────────────────────────
                .route("cpf-validador", r -> r
                        .path("/api/v1/cpf/**")
                        .uri(coreApiUrl))
                .route("biometria-api", r -> r
                        .path("/api/v1/biometria/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Secret", gatewaySecret)
                                .addResponseHeader("X-Gateway-Version", "1.0"))
                        .uri(coreApiUrl))
                .route("management-core-api", r -> r
                        .path("/management/core-api/**")
                        .filters(f -> f
                                .rewritePath("/management/core-api/?(?<segment>.*)", "/actuator/${segment}")
                                .addRequestHeader("X-Management-Source", "gateway"))
                        .uri(coreApiUrl))
                .route("management-legacy-soap", r -> r
                        .path("/management/legacy-soap/**")
                        .filters(f -> f
                                .rewritePath("/management/legacy-soap/?(?<segment>.*)", "/actuator/${segment}")
                                .addRequestHeader("X-Management-Source", "gateway"))
                        .uri(legacySoapUrl))
                .route("monitoring-prometheus", r -> r
                        .path("/monitoring/prometheus/**")
                        .filters(f -> f.rewritePath("/monitoring/prometheus/?(?<segment>.*)", "/${segment}"))
                        .uri(prometheusUrl))
                .route("monitoring-grafana", r -> r
                        .path("/monitoring/grafana/**")
                        .filters(f -> f.rewritePath("/monitoring/grafana/?(?<segment>.*)", "/${segment}"))
                        .uri(grafanaUrl))
                .route("monitoring-zipkin", r -> r
                        .path("/monitoring/zipkin/**")
                        .filters(f -> f.rewritePath("/monitoring/zipkin/?(?<segment>.*)", "/${segment}"))
                        .uri(zipkinUrl))
                .build();
    }
}
