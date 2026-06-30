package br.com.vivo.poc.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String correlationId = exchange.getAttribute(CorrelationIdGatewayFilter.CORRELATION_ATTRIBUTE);
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? "unknown" : route.getId();

        log.info("gateway_request_routed",
                kv("event", "gateway_request_routed"),
                kv("method", exchange.getRequest().getMethod() == null ? "UNKNOWN"
                        : exchange.getRequest().getMethod().name()),
                kv("path", exchange.getRequest().getPath().value()),
                kv("route", routeId),
                kv("correlationId", correlationId));

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Integer status = exchange.getResponse().getRawStatusCode();
                    long durationMs = System.currentTimeMillis() - start;
                    if (status != null && status >= 500) {
                        log.error("gateway_downstream_error",
                                kv("event", "gateway_downstream_error"),
                                kv("status", status),
                                kv("route", routeId),
                                kv("durationMs", durationMs),
                                kv("correlationId", correlationId));
                    }
                    log.info("gateway_response_sent",
                            kv("event", "gateway_response_sent"),
                            kv("status", status == null ? 0 : status),
                            kv("route", routeId),
                            kv("durationMs", durationMs),
                            kv("correlationId", correlationId));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
