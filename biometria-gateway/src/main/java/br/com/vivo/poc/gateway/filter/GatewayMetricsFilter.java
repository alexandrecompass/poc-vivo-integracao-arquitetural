package br.com.vivo.poc.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class GatewayMetricsFilter implements GlobalFilter, Ordered {

    private final Counter requestsTotal;
    private final Counter requestsAuthorized;
    private final Counter deniedNoToken;
    private final Counter deniedScope;
    private final Timer routingLatency;

    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.requestsTotal = Counter.builder("gateway.requests.total")
                .tag("service", "biometria-gateway")
                .register(meterRegistry);
        this.requestsAuthorized = Counter.builder("gateway.requests.authorized")
                .tag("service", "biometria-gateway")
                .register(meterRegistry);
        this.deniedNoToken = Counter.builder("gateway.requests.denied")
                .tag("service", "biometria-gateway")
                .tag("reason", "no_token")
                .register(meterRegistry);
        this.deniedScope = Counter.builder("gateway.requests.denied")
                .tag("service", "biometria-gateway")
                .tag("reason", "scope")
                .register(meterRegistry);
        this.routingLatency = Timer.builder("gateway.routing.latencia")
                .tag("service", "biometria-gateway")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        requestsTotal.increment();
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    routingLatency.record(Duration.ofNanos(System.nanoTime() - start));
                    String path = exchange.getRequest().getPath().value();
                    Integer status = exchange.getResponse().getRawStatusCode();
                    if (path.startsWith("/api/v1/biometria/") && status != null) {
                        if (status >= 200 && status < 400) {
                            requestsAuthorized.increment();
                        } else if (status == 401) {
                            deniedNoToken.increment();
                        } else if (status == 403) {
                            deniedScope.increment();
                        }
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
