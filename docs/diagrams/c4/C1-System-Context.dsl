// C4 Model — C1 System Context
// Projeto: POC Vivo – Integração Arquitetural
// Artefato: C1-System-Context.dsl
// Versão: 1.0

workspace "POC Vivo - Integração Arquitetural" "C1 System Context da Plataforma de Integração de Biometria" {

    model {
        parceiro = person "Parceiro Externo" "Sistema ou parceiro autorizado que consome a API de biometria facial."

        vivo = softwareSystem "Ecossistema Vivo" "Ecossistema corporativo onde reside a solução de integração." {
            plataforma = softwareSystem "Plataforma de Integração de Biometria" "Expõe API REST versionada e segura para consulta de biometria facial, isolando o legado SOAP."
            keycloak = softwareSystem "Keycloak" "Provedor OAuth2/OpenID Connect responsável por emitir tokens JWT e controlar escopos de acesso."
            legado = softwareSystem "Sistema Legado SOAP de Biometria" "Sistema interno que mantém a consulta oficial de biometria facial via SOAP."
        }

        parceiro -> keycloak "Solicita token OAuth2" "HTTPS / Client Credentials"
        keycloak -> parceiro "Retorna JWT com escopo biometria:read" "JWT"
        parceiro -> plataforma "Consulta biometria facial por CPF" "HTTPS REST /api/v1/biometria/{cpf}"
        plataforma -> keycloak "Valida token e metadados de segurança" "JWT / JWKS"
        plataforma -> legado "Consulta biometria no legado" "SOAP"
    }

    views {
        systemContext plataforma "C1-System-Context" {
            include *
            autolayout lr
            title "C1 - System Context - POC Vivo Integração Arquitetural"
            description "Mostra o contexto da Plataforma de Integração de Biometria, seus consumidores externos e sistemas de apoio."
        }

        styles {
            element "Person" {
                background "#08427b"
                color "#ffffff"
                shape person
            }

            element "Software System" {
                background "#1168bd"
                color "#ffffff"
            }

            element "Ecossistema Vivo" {
                background "#f5f5f5"
                color "#333333"
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
