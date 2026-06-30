// C4 Model — C3 Component Diagram
// Projeto: POC Vivo – Integração Arquitetural
// Artefato: C3-Component-Core-API.dsl
// Versão: 1.0

workspace "POC Vivo - Integração Arquitetural" "C3 - Component Diagram - Biometria Core API" {

    model {
        parceiro = person "Parceiro Externo" "Consumidor autorizado da API."

        keycloak = softwareSystem "Keycloak" "Identity Provider OAuth2/OpenID Connect."

        plataforma = softwareSystem "Plataforma de Integração de Biometria" "Sistema responsável por expor biometria facial via API REST segura e integrada ao legado SOAP." {

            gateway = container "API Gateway" "Ponto único de entrada, valida JWT, aplica políticas e encaminha requisições." "Spring Boot + Spring Cloud Gateway"

            core = container "Biometria Core API" "Microserviço REST responsável pelo caso de uso de consulta de biometria e integração com legado SOAP." "Spring Boot REST" {

                correlationFilter = component "CorrelationIdFilter" "Garante X-Correlation-Id em todas as requisições." "Servlet Filter"

                loggingFilter = component "RequestLoggingFilter" "Registra logs estruturados de entrada, saída, status e duração." "Servlet Filter"

                controller = component "BiometriaController" "Expõe GET /api/v1/biometria/{cpf}." "REST Controller"

                validator = component "BiometriaRequestValidator / Cpf" "Valida e normaliza o CPF recebido na requisição." "Validator / Value Object"

                responseMapper = component "BiometriaResponseMapper" "Converte o modelo interno para DTO REST v1." "Mapper"

                useCase = component "ConsultarBiometriaUseCase" "Orquestra o caso de uso de consulta de biometria." "Application Use Case"

                domain = component "Biometria Domain Model" "Representa CPF, biometria, status e origem no modelo interno." "Domain Model"

                port = component "BiometriaLegadoPort" "Define o contrato interno para consulta à origem de biometria." "Output Port"

                adapter = component "SoapBiometriaLegadoAdapter" "Implementa a porta usando o serviço SOAP legado." "SOAP Adapter"

                soapClient = component "SoapBiometriaClient" "Executa a chamada técnica ao Legacy SOAP Service." "SOAP Client"

                soapMapper = component "SoapBiometriaMapper" "Converte a resposta SOAP para o modelo interno." "Mapper"

                faultTranslator = component "SoapFaultTranslator" "Traduz SOAP Faults e falhas técnicas para exceções internas." "Error Translator"

                exceptionHandler = component "GlobalExceptionHandler" "Converte exceções internas em respostas REST padronizadas." "Exception Handler"

                metrics = component "BiometriaMetrics" "Registra métricas de consultas, erros e latência." "Micrometer / Actuator"
            }

            legado = container "Legacy SOAP Service" "Simula o serviço legado responsável pela consulta de biometria." "Spring Boot SOAP"

            banco = container "H2 Database" "Simula o Oracle legado contendo dados fictícios de biometria." "H2 Database"
        }

        parceiro -> keycloak "Obtém token OAuth2" "HTTPS / Client Credentials"
        keycloak -> parceiro "Retorna JWT"
        parceiro -> gateway "Chama GET /api/v1/biometria/{cpf}" "HTTPS REST"
        gateway -> keycloak "Valida JWT" "JWKS / OIDC"
        gateway -> correlationFilter "Encaminha requisição autorizada" "HTTP REST"

        correlationFilter -> loggingFilter "Propaga correlation ID"
        loggingFilter -> controller "Encaminha requisição"

        controller -> validator "Valida CPF"
        controller -> useCase "Executa caso de uso"
        controller -> responseMapper "Monta resposta REST"
        controller -> exceptionHandler "Erros são tratados por"

        useCase -> domain "Usa modelo interno"
        useCase -> port "Consulta biometria"

        adapter -> port "Implementa"
        adapter -> soapClient "Chama serviço SOAP"
        adapter -> soapMapper "Mapeia resposta SOAP"
        adapter -> faultTranslator "Traduz falhas SOAP"

        soapClient -> legado "Consulta biometria" "SOAP / HTTP"
        legado -> banco "Consulta dados fictícios" "JDBC"

        exceptionHandler -> controller "Padroniza respostas de erro"
        metrics -> useCase "Registra métricas do caso de uso"
        metrics -> adapter "Registra latência e erros SOAP"
    }

    views {
        component core "C3-Component-Core-API" {
            include *
            autolayout lr
            title "C3 - Component Diagram - Biometria Core API"
            description "Detalha os componentes internos da Biometria Core API, evidenciando Clean Architecture, Ports and Adapters e isolamento do legado SOAP."
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

            element "Component" {
                background "#85bbf0"
                color "#000000"
            }

            element "REST Controller" {
                background "#1168bd"
                color "#ffffff"
            }

            element "Application Use Case" {
                background "#0b6e4f"
                color "#ffffff"
            }

            element "Output Port" {
                background "#6c5ce7"
                color "#ffffff"
            }

            element "SOAP Adapter" {
                background "#e17055"
                color "#ffffff"
            }

            element "SOAP Client" {
                background "#d35400"
                color "#ffffff"
            }

            element "Domain Model" {
                background "#00b894"
                color "#ffffff"
            }

            element "Exception Handler" {
                background "#c0392b"
                color "#ffffff"
            }

            element "Servlet Filter" {
                background "#0984e3"
                color "#ffffff"
            }

            element "Micrometer / Actuator" {
                background "#2d3436"
                color "#ffffff"
            }

            element "H2 Database" {
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
