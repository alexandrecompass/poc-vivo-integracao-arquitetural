package br.com.vivo.poc.legado.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.transport.http.HttpUrlConnection;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class BiometriaEndpointE2ETest {

    private static final String NAMESPACE = BiometriaEndpoint.NAMESPACE;
    private static final String CORE_SECRET_VALIDO = "core-api-internal-secret-poc";

    @LocalServerPort
    private int port;

    private WebServiceTemplate templateComSecretValido;

    @BeforeEach
    void setUp() {
        templateComSecretValido = criarTemplate(CORE_SECRET_VALIDO);
    }

    @Test
    void cpfValidoExistenteRetornaBiometriaDisponivel() {
        String resposta = consultar(templateComSecretValido, "52998224725");

        assertThat(resposta).contains("<biometriaDisponivel>true</biometriaDisponivel>");
        assertThat(resposta).contains("LEGADO_SOAP");
    }

    @Test
    void cpfNaoEncontradoRetornaSoapFaultComBiometriaNaoEncontrada() {
        assertThatThrownBy(() -> consultar(templateComSecretValido, "00000000000"))
                .isInstanceOf(SoapFaultClientException.class)
                .hasMessageContaining("BIOMETRIA_NAO_ENCONTRADA");
    }

    @Test
    void semHeaderCoreSecretRetornaHttp403() throws Exception {
        WebServiceTemplate templateSemSecret = criarTemplateSemInterceptor();

        assertThatThrownBy(() -> consultar(templateSemSecret, "52998224725"))
                .isInstanceOf(WebServiceTransportException.class)
                .hasMessageContaining("403");

        RespostaCrua respostaCrua = enviarRequisicaoCrua("52998224725", null);
        assertThat(respostaCrua.statusCode()).isEqualTo(403);
        assertThat(respostaCrua.body()).contains("UNAUTHORIZED");
    }

    @Test
    void secretIncorretoRetornaHttp403() throws Exception {
        WebServiceTemplate templateSecretErrado = criarTemplate("valor-errado");

        assertThatThrownBy(() -> consultar(templateSecretErrado, "52998224725"))
                .isInstanceOf(WebServiceTransportException.class)
                .hasMessageContaining("403");

        RespostaCrua respostaCrua = enviarRequisicaoCrua("52998224725", "valor-errado");
        assertThat(respostaCrua.statusCode()).isEqualTo(403);
        assertThat(respostaCrua.body()).contains("UNAUTHORIZED");
    }

    @Test
    void segundoCpfValidoDoSeedRetornaBiometriaDisponivel() {
        String resposta = consultar(templateComSecretValido, "11144477735");

        assertThat(resposta).contains("<biometriaDisponivel>true</biometriaDisponivel>");
    }

    @Test
    void deveExporMetricasPrometheus() {
        consultar(templateComSecretValido, "52998224725");

        RestTemplate rest = new RestTemplate();
        String body = rest.getForObject(
                "http://localhost:" + port + "/actuator/prometheus",
                String.class);

        assertThat(body).contains("soap_requests_total");
        assertThat(body).contains("soap_db_latencia");
    }

    private record RespostaCrua(int statusCode, String body) {
    }

    /**
     * O WebServiceTemplate só tenta ler o corpo como SOAP Fault quando o status HTTP é 400 ou 500
     * (limitação do Spring-WS). Como o bloqueio do InternalSecretInterceptor responde com 403, a
     * verificação do conteúdo do fault (faultcode UNAUTHORIZED) precisa ser feita via HTTP cru,
     * usando HttpURLConnection (java.net.http.HttpClient conflita com a instrumentação do JaCoCo).
     */
    private RespostaCrua enviarRequisicaoCrua(String cpf, String coreSecretOuNulo) throws IOException {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:tns=\"" + NAMESPACE + "\">"
                + "<soapenv:Body>"
                + "<tns:ConsultarBiometriaRequest>"
                + "<tns:cpf>" + cpf + "</tns:cpf>"
                + "</tns:ConsultarBiometriaRequest>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:" + port + "/ws")
                .toURL()
                .openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        if (coreSecretOuNulo != null) {
            connection.setRequestProperty("X-Core-Secret", coreSecretOuNulo);
        }

        connection.getOutputStream().write(envelope.getBytes(StandardCharsets.UTF_8));

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        return new RespostaCrua(statusCode, body);
    }

    private String consultar(WebServiceTemplate template, String cpf) {
        StringSource request = new StringSource(
                "<tns:ConsultarBiometriaRequest xmlns:tns=\"" + NAMESPACE + "\">"
                        + "<tns:cpf>" + cpf + "</tns:cpf>"
                        + "</tns:ConsultarBiometriaRequest>");
        StringResult result = new StringResult();
        template.sendSourceAndReceiveToResult(request, result);
        return result.toString();
    }

    private WebServiceTemplate criarTemplate(String coreSecret) {
        WebServiceTemplate template = criarTemplateSemInterceptor();
        template.setInterceptors(new ClientInterceptor[] {new CoreSecretHeaderInterceptor(coreSecret)});
        return template;
    }

    private WebServiceTemplate criarTemplateSemInterceptor() {
        SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory();
        messageFactory.afterPropertiesSet();

        WebServiceTemplate template = new WebServiceTemplate(messageFactory);
        template.setDefaultUri("http://localhost:" + port + "/ws");
        return template;
    }

    private static class CoreSecretHeaderInterceptor implements ClientInterceptor {

        private final String coreSecret;

        private CoreSecretHeaderInterceptor(String coreSecret) {
            this.coreSecret = coreSecret;
        }

        @Override
        public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
            WebServiceConnection connection = TransportContextHolder.getTransportContext().getConnection();
            if (connection instanceof HttpUrlConnection httpUrlConnection) {
                try {
                    httpUrlConnection.addRequestHeader("X-Core-Secret", coreSecret);
                } catch (IOException ex) {
                    throw new WebServiceIOException("Erro ao adicionar header X-Core-Secret", ex);
                }
            }
            return true;
        }

        @Override
        public boolean handleResponse(MessageContext messageContext) {
            return true;
        }

        @Override
        public boolean handleFault(MessageContext messageContext) {
            return true;
        }

        @Override
        public void afterCompletion(MessageContext messageContext, Exception ex) {
            // nada a fazer
        }
    }
}
