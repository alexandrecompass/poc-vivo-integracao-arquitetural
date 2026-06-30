package br.com.vivo.poc.legado.ws.security;

import br.com.vivo.poc.legado.ws.metrics.SoapMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

import javax.xml.namespace.QName;
import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class InternalSecretInterceptor implements EndpointInterceptor {

    private static final Logger log = LoggerFactory.getLogger(InternalSecretInterceptor.class);

    private static final String HEADER_CORE_SECRET = "X-Core-Secret";

    private final String coreApiSecret;
    private final SoapMetrics metrics;

    public InternalSecretInterceptor(@Value("${internal.security.core-api-secret}") String coreApiSecret,
            SoapMetrics metrics) {
        this.coreApiSecret = coreApiSecret;
        this.metrics = metrics;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        metrics.incrementarTotal();

        HttpServletConnection connection = currentHttpServletConnection();
        if (connection == null) {
            return true;
        }

        HttpServletRequest httpServletRequest = connection.getHttpServletRequest();
        String secretHeader = httpServletRequest.getHeader(HEADER_CORE_SECRET);

        if (secretHeader == null) {
            log.warn("core_secret_missing", kv("event", "core_secret_missing"));
            bloquear(connection, httpServletRequest, "X-Core-Secret ausente");
            return false;
        }
        if (!secretHeader.equals(coreApiSecret)) {
            log.warn("core_secret_invalid", kv("event", "core_secret_invalid"));
            bloquear(connection, httpServletRequest, "X-Core-Secret inválido");
            return false;
        }
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) {
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
        // nada a fazer
    }

    private void bloquear(HttpServletConnection connection, HttpServletRequest httpServletRequest, String motivo)
            throws IOException {
        metrics.incrementarBloqueada();
        log.warn("acesso_direto_bloqueado",
                kv("event", "acesso_direto_bloqueado"),
                kv("remoteAddr", httpServletRequest.getRemoteAddr()),
                kv("motivo", motivo));
        rejeitarComForbidden(connection);
    }

    private HttpServletConnection currentHttpServletConnection() {
        TransportContext transportContext = TransportContextHolder.getTransportContext();
        if (transportContext == null) {
            return null;
        }
        WebServiceConnection connection = transportContext.getConnection();
        return connection instanceof HttpServletConnection httpServletConnection ? httpServletConnection : null;
    }

    /**
     * Escreve a resposta HTTP diretamente, sem passar pelo {@link MessageContext}.
     * Isso evita que o Spring-WS sobrescreva o status HTTP 403 com 500, que é o
     * comportamento padrão do framework para qualquer SOAP Fault.
     */
    private void rejeitarComForbidden(HttpServletConnection connection) throws IOException {
        connection.setFaultCode(new QName("UNAUTHORIZED"));

        HttpServletResponse response = connection.getHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/xml;charset=UTF-8");
        response.getWriter().write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                  <SOAP-ENV:Body>
                    <SOAP-ENV:Fault>
                      <faultcode>UNAUTHORIZED</faultcode>
                      <faultstring>Acesso direto ao serviço SOAP não permitido.</faultstring>
                    </SOAP-ENV:Fault>
                  </SOAP-ENV:Body>
                </SOAP-ENV:Envelope>
                """);
        response.getWriter().flush();
    }
}
