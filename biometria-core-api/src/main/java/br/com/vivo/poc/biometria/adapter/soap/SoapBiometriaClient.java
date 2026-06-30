package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import br.com.vivo.poc.biometria.infrastructure.logging.CpfMasker;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.net.SocketTimeoutException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class SoapBiometriaClient {

    private static final Logger log = LoggerFactory.getLogger(SoapBiometriaClient.class);

    private final WebServiceTemplate webServiceTemplate;
    private final CpfMasker cpfMasker;

    @Value("${soap.legacy.url}")
    private String soapUrl;

    @Value("${soap.legacy.core-secret}")
    private String coreSecret;

    public SoapBiometriaClient(WebServiceTemplate webServiceTemplate, CpfMasker cpfMasker) {
        this.webServiceTemplate = webServiceTemplate;
        this.cpfMasker = cpfMasker;
    }

    public Object consultar(String cpfDigits) {
        long start = System.currentTimeMillis();
        StringSource source = new StringSource("""
                <tns:ConsultarBiometriaRequest xmlns:tns="http://vivo.com.br/poc/biometria">
                  <tns:cpf>%s</tns:cpf>
                </tns:ConsultarBiometriaRequest>
                """.formatted(cpfDigits));
        DOMResult result = new DOMResult();
        try {
            log.info("soap_call_started",
                    kv("event", "soap_call_started"),
                    kv("cpf", cpfMasker.mask(cpfDigits)),
                    kv("url", soapUrl),
                    kv("correlationId", MDC.get("correlationId")));
            webServiceTemplate.sendSourceAndReceiveToResult(soapUrl, source, buildCallback(), result);
            log.info("soap_call_finished",
                    kv("event", "soap_call_finished"),
                    kv("cpf", cpfMasker.mask(cpfDigits)),
                    kv("durationMs", System.currentTimeMillis() - start),
                    kv("correlationId", MDC.get("correlationId")));
            return result;
        } catch (SoapFaultClientException ex) {
            log.warn("soap_fault_received",
                    kv("event", "soap_fault_received"),
                    kv("cpf", cpfMasker.mask(cpfDigits)),
                    kv("correlationId", MDC.get("correlationId")));
            throw ex;
        } catch (ResourceAccessException | WebServiceIOException ex) {
            if (isTimeout(ex)) {
                log.error("soap_timeout",
                        kv("event", "soap_timeout"),
                        kv("cpf", cpfMasker.mask(cpfDigits)),
                        kv("correlationId", MDC.get("correlationId")));
                throw new LegadoTimeoutException("O servico legado demorou mais do que o esperado.", ex);
            }
            log.error("soap_unavailable",
                    kv("event", "soap_unavailable"),
                    kv("cpf", cpfMasker.mask(cpfDigits)),
                    kv("correlationId", MDC.get("correlationId")));
            throw new LegadoIndisponivelException("Nao foi possivel acessar o servico legado.", ex);
        } catch (Exception ex) {
            log.error("soap_unavailable",
                    kv("event", "soap_unavailable"),
                    kv("cpf", cpfMasker.mask(cpfDigits)),
                    kv("correlationId", MDC.get("correlationId")));
            throw new LegadoIndisponivelException("Falha inesperada ao consultar o legado.", ex);
        }
    }

    private WebServiceMessageCallback buildCallback() {
        return new WebServiceMessageCallback() {
            @Override
            public void doWithMessage(WebServiceMessage message) throws IOException {
                if (message instanceof SaajSoapMessage saajSoapMessage) {
                    try {
                        MimeHeaders mimeHeaders = saajSoapMessage.getSaajMessage().getMimeHeaders();
                        mimeHeaders.setHeader("X-Core-Secret", coreSecret);
                        String correlationId = MDC.get("correlationId");
                        if (correlationId != null && !correlationId.isBlank()) {
                            mimeHeaders.setHeader("X-Correlation-Id", correlationId);
                        }
                        saajSoapMessage.getSaajMessage().saveChanges();
                    } catch (SOAPException ex) {
                        throw new IOException("Nao foi possivel preparar a mensagem SOAP.", ex);
                    }
                }
            }
        };
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof SocketTimeoutException
                    || current.getClass().getSimpleName().contains("Timeout")
                    || (message != null && message.toLowerCase().contains("timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
