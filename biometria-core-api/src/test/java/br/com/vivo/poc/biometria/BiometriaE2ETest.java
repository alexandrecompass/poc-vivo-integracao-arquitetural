package br.com.vivo.poc.biometria;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CoreApiApplication.class)
@Import(BiometriaE2ETest.TestSecurityConfig.class)
@AutoConfigureObservability
class BiometriaE2ETest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    static {
        WIRE_MOCK.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("soap.legacy.url", () -> "http://localhost:" + WIRE_MOCK.port() + "/ws");
        registry.add("internal.security.gateway-secret", () -> "gateway-internal-secret-poc");
        registry.add("soap.legacy.core-secret", () -> "core-api-internal-secret-poc");
    }

    @BeforeEach
    void setUp() {
        WIRE_MOCK.resetAll();
        WIRE_MOCK.stubFor(post(urlEqualTo("/ws"))
                .withHeader("X-Core-Secret", equalTo("core-api-internal-secret-poc"))
                .withRequestBody(containing("52998224725"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(soapSucessoXml("52998224725"))));

        WIRE_MOCK.stubFor(post(urlEqualTo("/ws"))
                .withHeader("X-Core-Secret", equalTo("core-api-internal-secret-poc"))
                .withRequestBody(containing("00000000000"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(soapFaultXml("BIOMETRIA_NAO_ENCONTRADA"))));

        WIRE_MOCK.stubFor(post(urlEqualTo("/ws"))
                .withHeader("X-Core-Secret", equalTo("core-api-internal-secret-poc"))
                .withRequestBody(containing("99988877766"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(soapSucessoXml("99988877766"))));
    }

    @AfterAll
    static void tearDown() {
        WIRE_MOCK.stop();
    }

    @Test
    void fluxoFelizCompleto() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("cpf", "*********25");
    }

    @Test
    void deveRetornar401SemBearerToken() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth(null, "gateway-internal-secret-poc", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void deveRetornar403SemGatewaySecret() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", null, null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void deveRetornar403ComGatewaySecretErrado() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "errado", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void deveRetornar400ParaCpfInvalido() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/123"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void deveRetornar404QuandoSoapNaoEncontrarCpf() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/00000000000"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deveRetornar504QuandoSoapDerTimeout() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/99988877766"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", null)), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(504);
    }

    @Test
    void deveValidarCpfValidoSemTokenESemSecret() {
        ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/v1/cpf/52998224725/validar"), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("valido", true);
    }

    @Test
    void deveValidarCpfInvalidoSemTokenESemSecret() {
        ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/v1/cpf/123/validar"), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("valido", false);
    }

    @Test
    void devePropagarCorrelationId() {
        ResponseEntity<Map> response = restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", "corr-123")), Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-123");
    }

    @Test
    void deveExporMetricasCustomizadasNoPrometheus() {
        restTemplate.exchange(url("/api/v1/biometria/52998224725"), HttpMethod.GET,
                new HttpEntity<>(headersComAuth("valid-token", "gateway-internal-secret-poc", null)), String.class);

        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/prometheus"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("biometria_consultas_total");
        assertThat(response.getBody()).contains("biometria_legado_latencia_seconds");
    }

    private HttpHeaders headersComAuth(String token, String gatewaySecret, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (gatewaySecret != null) {
            headers.add("X-Gateway-Secret", gatewaySecret);
        }
        if (correlationId != null) {
            headers.add("X-Correlation-Id", correlationId);
        }
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static String soapSucessoXml(String cpf) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:ConsultarBiometriaResponse xmlns:ns2="http://vivo.com.br/poc/biometria">
                      <ns2:cpf>%s</ns2:cpf>
                      <ns2:biometriaDisponivel>true</ns2:biometriaDisponivel>
                      <ns2:imagemBase64>aW1hZ2VtLWZpY3RpY2lhLWJhc2U2NA==</ns2:imagemBase64>
                      <ns2:origem>LEGADO_SOAP</ns2:origem>
                      <ns2:dataConsulta>2026-06-30T12:00:00</ns2:dataConsulta>
                    </ns2:ConsultarBiometriaResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(cpf);
    }

    private static String soapFaultXml(String faultCode) {
        return """
                <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                  <SOAP-ENV:Body>
                    <SOAP-ENV:Fault>
                      <faultcode>SOAP-ENV:Server</faultcode>
                      <faultstring>%s</faultstring>
                    </SOAP-ENV:Fault>
                  </SOAP-ENV:Body>
                </SOAP-ENV:Envelope>
                """.formatted(faultCode);
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("parceiro-externo")
                    .claim("scope", "biometria:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}
