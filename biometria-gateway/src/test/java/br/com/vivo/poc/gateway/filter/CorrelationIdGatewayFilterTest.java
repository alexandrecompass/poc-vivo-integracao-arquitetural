package br.com.vivo.poc.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGatewayFilterTest {

    private final CorrelationIdGatewayFilter filter = new CorrelationIdGatewayFilter();

    @Test
    void devePropagarCorrelationIdExistente() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/biometria/52998224725")
                .header(CorrelationIdGatewayFilter.CORRELATION_HEADER, "corr-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst(
                    CorrelationIdGatewayFilter.CORRELATION_HEADER)).isEqualTo("corr-123");
            return mutatedExchange.getResponse().setComplete();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(
                CorrelationIdGatewayFilter.CORRELATION_HEADER)).isEqualTo("corr-123");
    }

    @Test
    void deveGerarCorrelationIdQuandoAusente() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/cpf/52998224725/validar").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String header = mutatedExchange.getRequest().getHeaders().getFirst(
                    CorrelationIdGatewayFilter.CORRELATION_HEADER);
            assertThat(header).isNotBlank();
            return Mono.defer(mutatedExchange.getResponse()::setComplete);
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(CorrelationIdGatewayFilter.CORRELATION_HEADER)).isNotBlank();
    }
}
