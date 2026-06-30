package br.com.vivo.poc.gateway.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component("coreApi")
public class CoreApiHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    private final String coreApiUrl;

    public CoreApiHealthIndicator(WebClient.Builder webClientBuilder,
            @Value("${downstream.core-api.url}") String coreApiUrl) {
        this.webClient = webClientBuilder.build();
        this.coreApiUrl = coreApiUrl;
    }

    @Override
    public Mono<Health> health() {
        return webClient.get()
                .uri(coreApiUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> Health.up()
                        .withDetail("coreApi", "disponivel")
                        .withDetail("url", coreApiUrl)
                        .withDetail("payload", body)
                        .build())
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withDetail("coreApi", "indisponivel")
                        .withDetail("url", coreApiUrl)
                        .withDetail("erro", ex.getMessage())
                        .build()));
    }
}
