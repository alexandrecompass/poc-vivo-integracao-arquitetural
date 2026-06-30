package br.com.vivo.poc.biometria.infrastructure.security;

import br.com.vivo.poc.biometria.api.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class GatewaySecretFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecretFilter.class);
    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";

    private final String gatewaySecret;
    private final ObjectMapper objectMapper;

    public GatewaySecretFilter(@Value("${internal.security.gateway-secret}") String gatewaySecret,
            ObjectMapper objectMapper) {
        this.gatewaySecret = gatewaySecret;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/biometria/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_GATEWAY_SECRET);
        if (header == null || header.isBlank()) {
            log.warn("gateway_secret_missing", kv("event", "gateway_secret_missing"));
            writeForbidden(response, request);
            return;
        }
        if (!gatewaySecret.equals(header)) {
            log.warn("gateway_secret_invalid", kv("event", "gateway_secret_invalid"));
            writeForbidden(response, request);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(
                Instant.now().toString(),
                HttpServletResponse.SC_FORBIDDEN,
                "ACESSO_NEGADO",
                "Acesso negado. Header interno invalido ou ausente.",
                request.getRequestURI(),
                MDC.get("correlationId")
        )));
    }
}
