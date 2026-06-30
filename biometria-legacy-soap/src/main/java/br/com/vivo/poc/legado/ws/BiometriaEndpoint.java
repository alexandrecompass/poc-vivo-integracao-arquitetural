package br.com.vivo.poc.legado.ws;

import br.com.vivo.poc.legado.domain.Biometria;
import br.com.vivo.poc.legado.service.BiometriaNaoEncontradaException;
import br.com.vivo.poc.legado.service.BiometriaService;
import br.com.vivo.poc.legado.ws.metrics.SoapMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapFaultException;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Endpoint
public class BiometriaEndpoint {

    public static final String NAMESPACE = "http://vivo.com.br/poc/biometria";

    private static final Logger log = LoggerFactory.getLogger(BiometriaEndpoint.class);

    private final BiometriaService biometriaService;
    private final SoapMetrics metrics;
    private final DocumentBuilderFactory documentBuilderFactory;

    public BiometriaEndpoint(BiometriaService biometriaService, SoapMetrics metrics) {
        this.biometriaService = biometriaService;
        this.metrics = metrics;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "ConsultarBiometriaRequest")
    @ResponsePayload
    public Element consultarBiometria(@RequestPayload Element request, MessageContext messageContext)
            throws ParserConfigurationException {
        String correlationId = extrairCorrelationId();
        MDC.put("correlationId", correlationId);
        try {
            log.info("soap_request_received", kv("event", "soap_request_received"),
                    kv("correlationId", correlationId));

            String cpf = extrairCpf(request);

            try {
                Biometria biometria = biometriaService.consultarPorCpf(cpf);
                Element resposta = montarResposta(biometria);
                metrics.incrementarSucesso();
                log.info("soap_response_sent", kv("event", "soap_response_sent"));
                return resposta;
            } catch (BiometriaNaoEncontradaException ex) {
                throw new SoapFaultException("BIOMETRIA_NAO_ENCONTRADA");
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extrairCpf(Element request) {
        NodeList nodes = request.getElementsByTagNameNS(NAMESPACE, "cpf");
        if (nodes.getLength() == 0) {
            throw new SoapFaultException("CPF_AUSENTE");
        }
        return nodes.item(0).getTextContent();
    }

    private Element montarResposta(Biometria biometria) throws ParserConfigurationException {
        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        Element response = document.createElementNS(NAMESPACE, "ConsultarBiometriaResponse");

        appendElement(document, response, "cpf", biometria.getCpf());
        appendElement(document, response, "biometriaDisponivel", String.valueOf(biometria.isBiometriaDisponivel()));
        appendElement(document, response, "imagemBase64", biometria.getImagemBase64());
        appendElement(document, response, "origem", "LEGADO_SOAP");
        appendElement(document, response, "dataConsulta", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return response;
    }

    private void appendElement(Document document, Element parent, String localName, String value) {
        Element element = document.createElementNS(NAMESPACE, localName);
        if (value != null) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
    }

    private String extrairCorrelationId() {
        TransportContext transportContext = TransportContextHolder.getTransportContext();
        if (transportContext != null) {
            WebServiceConnection connection = transportContext.getConnection();
            if (connection instanceof HttpServletConnection httpServletConnection) {
                String correlationId = httpServletConnection.getHttpServletRequest().getHeader("X-Correlation-Id");
                if (correlationId != null && !correlationId.isBlank()) {
                    return correlationId;
                }
            }
        }
        return "N/A";
    }
}
