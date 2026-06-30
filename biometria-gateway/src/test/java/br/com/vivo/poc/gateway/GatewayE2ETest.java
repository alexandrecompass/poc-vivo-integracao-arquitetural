package br.com.vivo.poc.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BiometriaGatewayApplication.class)
@AutoConfigureObservability
@AutoConfigureWebTestClient
class GatewayE2ETest {

    private static final WireMockServer CORE_API = new WireMockServer(options().dynamicPort());
    private static final WireMockServer LEGACY_SOAP = new WireMockServer(options().dynamicPort());
    private static final WireMockServer PROMETHEUS = new WireMockServer(options().dynamicPort());
    private static final WireMockServer GRAFANA = new WireMockServer(options().dynamicPort());
    private static final WireMockServer ZIPKIN = new WireMockServer(options().dynamicPort());
    private static final WireMockServer JWKS = new WireMockServer(options().dynamicPort());
    private static final RSAKey RSA_JWK = generateRsaKey();

    static {
        CORE_API.start();
        LEGACY_SOAP.start();
        PROMETHEUS.start();
        GRAFANA.start();
        ZIPKIN.start();
        JWKS.start();
    }

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("gateway.secret", () -> "gateway-secret-poc");
        registry.add("downstream.core-api.url", CORE_API::baseUrl);
        registry.add("downstream.legacy-soap.url", LEGACY_SOAP::baseUrl);
        registry.add("downstream.prometheus.url", PROMETHEUS::baseUrl);
        registry.add("downstream.grafana.url", GRAFANA::baseUrl);
        registry.add("downstream.zipkin.url", ZIPKIN::baseUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> JWKS.baseUrl() + "/realms/vivo-poc/protocol/openid-connect/certs");
    }

    @BeforeEach
    void setUp() {
        CORE_API.resetAll();
        LEGACY_SOAP.resetAll();
        PROMETHEUS.resetAll();
        GRAFANA.resetAll();
        ZIPKIN.resetAll();
        JWKS.resetAll();

        JWKS.stubFor(get(urlEqualTo("/realms/vivo-poc/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(new JWKSet(RSA_JWK.toPublicJWK()).toString())));

        CORE_API.stubFor(get(urlEqualTo("/api/v1/biometria/52998224725"))
                .withHeader("X-Gateway-Secret", equalTo("gateway-secret-poc"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cpf\":\"*********25\",\"biometriaDisponivel\":true}")));
        CORE_API.stubFor(get(urlEqualTo("/api/v1/biometria/00000000000"))
                .withHeader("X-Gateway-Secret", equalTo("gateway-secret-poc"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"BIOMETRIA_NAO_ENCONTRADA\"}")));
        CORE_API.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));
        CORE_API.stubFor(get(urlEqualTo("/actuator/prometheus"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("gateway_requests_total 1\nbiometria_consultas_total 1\n")));
        LEGACY_SOAP.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));
    }

    @AfterAll
    static void tearDown() {
        CORE_API.stop();
        LEGACY_SOAP.stop();
        PROMETHEUS.stop();
        GRAFANA.stop();
        ZIPKIN.stop();
        JWKS.stop();
    }

    @Test
    void tokenValidoComEscopoRetorna200() throws Exception {
        String token = jwt("biometria:read");
        webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void semTokenRetorna401() {
        webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenSemEscopoRetorna403() throws Exception {
        String token = jwt("outro:scope");
        webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void correlationIdAusenteEhGeradoNaResposta() throws Exception {
        String token = jwt("biometria:read");
        String correlationId = webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("X-Correlation-Id");

        assertThat(correlationId).isNotBlank();
    }

    @Test
    void correlationIdPresenteEhPropagado() throws Exception {
        String token = jwt("biometria:read");
        webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .header("X-Correlation-Id", "corr-presente")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "corr-presente");
    }

    @Test
    void downstreamRetornando502EhPropagado() throws Exception {
        String token = jwt("biometria:read");
        CORE_API.stubFor(get(urlEqualTo("/api/v1/biometria/88888888888"))
                .withHeader("X-Gateway-Secret", equalTo("gateway-secret-poc"))
                .willReturn(aResponse().withStatus(502)));

        webTestClient.get()
                .uri("/api/v1/biometria/88888888888")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    void healthDoGatewayRetornaUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void managementProxyDoCoreApiHealthFunciona() {
        webTestClient.get()
                .uri("/management/core-api/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void prometheusDoGatewayEstaDisponivel() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("gateway_requests_total"));
    }

    private static String jwt(String scope) throws JOSEException {
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_JWK.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject("parceiro-externo")
                        .issuer("http://localhost/test")
                        .issueTime(new Date())
                        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                        .claim("scope", scope)
                        .build());
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) RSA_JWK.toPrivateKey()));
        return signedJwt.serialize();
    }

    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID("gateway-test-key")
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar a chave RSA de teste", ex);
        }
    }
}
