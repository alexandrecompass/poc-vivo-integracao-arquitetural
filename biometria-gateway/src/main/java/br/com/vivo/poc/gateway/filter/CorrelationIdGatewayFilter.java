package br.com.vivo.poc.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ATTRIBUTE = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("gateway_correlation_generated",
                    kv("event", "gateway_correlation_generated"),
                    kv("correlationId", correlationId));
        }
        final String finalCorrelationId = correlationId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(CORRELATION_HEADER, finalCorrelationId))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getAttributes().put(CORRELATION_ATTRIBUTE, finalCorrelationId);
        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders().set(CORRELATION_HEADER, finalCorrelationId);
            return Mono.empty();
        });

        log.info("gateway_request_received",
                kv("event", "gateway_request_received"),
                kv("method", request.getMethod() == null ? "UNKNOWN" : request.getMethod().name()),
                kv("path", request.getURI().getPath()),
                kv("correlationId", finalCorrelationId));

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
