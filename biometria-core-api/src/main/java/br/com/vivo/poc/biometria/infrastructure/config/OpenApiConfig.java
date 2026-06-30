package br.com.vivo.poc.biometria.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI 3.0 (Swagger) para o biometria-core-api.
 *
 * <p>Expõe dois esquemas de autenticação:
 * <ul>
 *   <li><b>bearerAuth</b> — cole diretamente um JWT obtido via Keycloak</li>
 *   <li><b>oauth2</b> — fluxo Client Credentials direto pelo Swagger UI</li>
 * </ul>
 *
 * <p>Acesse via Gateway: {@code http://localhost:8080/swagger-ui/index.html}
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Biometria Facial — Core API",
                version = "1.0.0",
                description = """
                        API REST para consulta de biometria facial de clientes Vivo.

                        **Autenticação:**
                        1. Clique em **Authorize** (cadeado)
                        2. Escolha **oauth2** → Client Credentials, use `parceiro-externo` / `parceiro-externo-secret`
                        3. Marque o escopo `biometria:read` e clique **Authorize**

                        Alternativamente, obtenha o token via curl e cole em **bearerAuth**.

                        ```bash
                        TOKEN=$(curl -s -X POST \\
                          http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token \\
                          -d "grant_type=client_credentials&client_id=parceiro-externo&client_secret=parceiro-externo-secret" \\
                          | jq -r .access_token)
                        ```
                        """,
                contact = @Contact(
                        name = "Time de Integração Arquitetural — Vivo",
                        email = "arquitetura-integracao@vivo.com.br"
                ),
                license = @License(name = "Interno — Confidencial", url = "")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Gateway (JWT obrigatório — escopo biometria:read)")
        },
        security = {
                @SecurityRequirement(name = "bearerAuth"),
                @SecurityRequirement(name = "oauth2", scopes = {"biometria:read"})
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Cole o JWT obtido do Keycloak. Escopo necessário: `biometria:read`"
)
@SecurityScheme(
        name = "oauth2",
        type = SecuritySchemeType.OAUTH2,
        description = "Fluxo Client Credentials via Keycloak (realm vivo-poc)",
        flows = @OAuthFlows(
                clientCredentials = @OAuthFlow(
                        // Proxy via Gateway (mesma origem do Swagger UI) — elimina CORS
                        // Gateway repassa para: http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token
                        tokenUrl = "http://localhost:8080/auth/token",
                        scopes = {
                                @OAuthScope(name = "biometria:read", description = "Leitura de dados de biometria facial")
                        }
                )
        )
)
public class OpenApiConfig {
    // Configuração via anotações — sem beans adicionais necessários
}
