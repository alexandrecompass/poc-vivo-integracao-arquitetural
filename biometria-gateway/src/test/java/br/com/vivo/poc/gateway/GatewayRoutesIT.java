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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BiometriaGatewayApplication.class)
@AutoConfigureObservability
@AutoConfigureWebTestClient
class GatewayRoutesIT {

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

        CORE_API.stubFor(get(urlEqualTo("/api/v1/cpf/52998224725/validar"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cpf\":\"*********25\",\"valido\":true,\"mensagem\":\"CPF valido.\"}")));

        CORE_API.stubFor(get(urlEqualTo("/api/v1/biometria/52998224725"))
                .withHeader("X-Gateway-Secret", equalTo("gateway-secret-poc"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cpf\":\"*********25\",\"biometriaDisponivel\":true}")));

        CORE_API.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\"}")));

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
    void devePermitirProxyPublicoParaValidadorCpf() {
        webTestClient.get()
                .uri("/api/v1/cpf/52998224725/validar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valido").isEqualTo(true);
    }

    @Test
    void deveRoteiarBiometriaComJwtESecretInterno() throws Exception {
        String token = jwt("biometria:read");
        webTestClient.get()
                .uri("/api/v1/biometria/52998224725")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0")
                .expectBody()
                .jsonPath("$.biometriaDisponivel").isEqualTo(true);
    }

    @Test
    void deveProxyarHealthDoCoreApiSemJwt() {
        webTestClient.get()
                .uri("/management/core-api/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
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
