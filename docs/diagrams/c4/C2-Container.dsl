workspace "POC Vivo - Integração Arquitetural" "C2 - Container Diagram" {

    model {

        parceiro = person "Parceiro Externo" "Consumidor autorizado da API."

        keycloak = softwareSystem "Keycloak" "Identity Provider (OAuth2/OpenID Connect)."

        plataforma = softwareSystem "Plataforma de Integração de Biometria" "Sistema em desenvolvimento." {

            gateway = container "API Gateway" "Ponto único de entrada, valida JWT, aplica políticas e roteia chamadas." "Spring Boot + Spring Cloud Gateway"

            core = container "Biometria Core API" "Implementa os casos de uso e integra com o legado SOAP." "Spring Boot REST"

            legado = container "Legacy SOAP Service" "Simula o serviço SOAP legado." "Spring Boot SOAP"

            banco = container "H2 Database" "Simula o Oracle legado." "H2 Database"
        }

        parceiro -> keycloak "Obtém token OAuth2" "HTTPS / Client Credentials"
        keycloak -> parceiro "Retorna JWT"

        parceiro -> gateway "GET /api/v1/biometria/{cpf}" "HTTPS REST"
        gateway -> keycloak "Valida JWT" "JWKS / OIDC"
        gateway -> core "Encaminha requisição" "HTTP REST"
        core -> legado "Consulta biometria" "SOAP"
        legado -> banco "Consulta dados" "JDBC"
    }

    views {

        container plataforma "C2-Container" {
            include *
            autolayout lr
            title "C2 - Container Diagram - POC Vivo Integração Arquitetural"
            description "Visão dos principais containers que compõem a Plataforma de Integração de Biometria."
        }

        styles {

            element "Person" {
                shape person
                background "#08427b"
                color "#ffffff"
            }

            element "Software System" {
                background "#1168bd"
                color "#ffffff"
            }

            element "Container" {
                background "#0b6e4f"
                color "#ffffff"
            }

            element "Database" {
                shape cylinder
                background "#f39c12"
                color "#ffffff"
            }

            relationship "Relationship" {
                color "#707070"
            }
        }

        theme default
    }

    configuration {
        scope softwaresystem
    }
}
