<div align="center">

<img src="https://img.shields.io/badge/Vivo-660099?style=for-the-badge&logoColor=white" alt="Vivo"/>

# POC — Integração Arquitetural de Biometria Facial

**Case Técnico · Arquiteto de Integração · Vivo / Telefônica Brasil**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-2023.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-gateway)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-24+-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[![Keycloak](https://img.shields.io/badge/Keycloak-24-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![Prometheus](https://img.shields.io/badge/Prometheus-2.47-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-10.2-F46800?style=for-the-badge&logo=grafana&logoColor=white)](https://grafana.com/)
[![Zipkin](https://img.shields.io/badge/Zipkin-3-FE7139?style=for-the-badge&logo=apache&logoColor=white)](https://zipkin.io/)
[![H2](https://img.shields.io/badge/H2_Database-in--memory-00618A?style=for-the-badge&logo=h2&logoColor=white)](https://www.h2database.com/)

[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)](.)
[![Coverage](https://img.shields.io/badge/coverage-%E2%89%A580%25-brightgreen?style=flat-square)](.)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)
[![LGPD](https://img.shields.io/badge/LGPD-conforme-660099?style=flat-square)](docs/observability/OBSERVABILIDADE.md)

</div>

---

## Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Arquitetura](#arquitetura)
- [Stack Tecnológica](#stack-tecnológica)
- [Pré-requisitos](#pré-requisitos)
- [Instalação por Sistema Operacional](#instalação-por-sistema-operacional)
  - [Windows](#windows)
  - [Linux](#linux)
  - [macOS](#macos)
- [Rodando em Desenvolvimento (Isolado)](#rodando-em-desenvolvimento-isolado)
- [Rodando via Docker Compose](#rodando-via-docker-compose)
- [Fluxo de Autenticação OAuth2](#fluxo-de-autenticação-oauth2)
- [Endpoints e Mapa de URLs](#endpoints-e-mapa-de-urls)
- [Observabilidade](#observabilidade)
- [Segurança](#segurança)
- [Testes](#testes)
- [Estrutura do Repositório](#estrutura-do-repositório)
- [LGPD e Dados Sensíveis](#lgpd-e-dados-sensíveis)
- [Documentação Arquitetural](#documentação-arquitetural)
- [Contribuindo](#contribuindo)

---

## Sobre o Projeto

Esta POC demonstra uma estratégia de integração arquitetural para expor dados de **biometria facial** mantidos em um sistema **legado Oracle/SOAP** por meio de uma **API REST moderna**, segura, observável e versionada — alinhada às práticas de arquitetura de integração adotadas na Vivo / Telefônica Brasil.

### Contexto e Problema

Sistemas legados de biometria facial, comuns em operadoras de telecomunicações, frequentemente expõem dados via interfaces SOAP/XML sobre bancos Oracle. Parceiros externos precisam consumir esses dados por meio de APIs modernas REST/JSON, com autenticação OAuth2, rastreabilidade por correlação e observabilidade integrada — sem que o legado precise ser reescrito.

### Solução Implementada

A POC implementa um padrão de **Anti-Corruption Layer (ACL)** com **Facade** e **Adapter**, expondo o legado SOAP como REST sem acoplamento direto, protegido por camadas de segurança com defesa em profundidade:

```
Parceiro Externo
      │
      │  HTTPS + Bearer JWT (OAuth2 Client Credentials)
      ▼
┌─────────────────────┐
│  biometria-gateway  │  ← valida JWT, injeta X-Gateway-Secret, gera correlationId
│  Spring Cloud GW    │
│  :8080              │
└──────────┬──────────┘
           │  HTTP interno + X-Gateway-Secret
           ▼
┌─────────────────────┐
│  biometria-core-api │  ← valida secret, executa caso de uso, coleta métricas
│  Spring Boot REST   │
│  :8082              │
└──────────┬──────────┘
           │  SOAP/HTTP + X-Core-Secret
           ▼
┌─────────────────────┐
│ biometria-legacy-   │  ← valida secret, consulta H2 (simula Oracle)
│ soap                │
│ Spring-WS :8083     │
└──────────┬──────────┘
           │  JDBC
           ▼
┌─────────────────────┐
│  H2 In-Memory DB    │  ← simula Oracle com 10 CPFs válidos pré-cadastrados
└─────────────────────┘
```

### Decisões Arquiteturais Principais

| Decisão | Escolha | Justificativa |
|---|---|---|
| Ponto único de entrada | Spring Cloud Gateway | Centraliza autenticação, correlação e observabilidade |
| Autenticação | OAuth2 Client Credentials + JWT | Padrão de mercado para comunicação M2M |
| Identity Provider | Keycloak 24 | Open-source, extensível, compatível com OIDC/OAuth2 |
| Isolamento de camadas | X-Gateway-Secret + X-Core-Secret | Defesa em profundidade — impede acesso direto ao legado |
| Observabilidade | Micrometer + Prometheus + Grafana + Loki + Zipkin | Três pilares: métricas, logs, traces |
| Logs estruturados | logstash-logback-encoder (JSON) | Ingestão nativa pelo Loki sem parsing adicional |
| Tracing distribuído | Micrometer Tracing + Brave + Zipkin | B3 propagation, correlação logs ↔ traces no Grafana |
| Legado como ACL | Spring-WS + Adapter + Translator | Protege o domínio de vazamento de contratos SOAP |

### Padrões de Projeto

A solução aplica padrões GoF, DDD e arquiteturais de forma deliberada. Veja o documento de referência completo com explicações, localização no código e justificativas:

📄 **[`docs/PADROES-DE-PROJETO.md`](docs/PADROES-DE-PROJETO.md)**

Resumo dos padrões principais:

| Categoria | Padrão | Onde |
|---|---|---|
| Arquitetural | **Clean Architecture / Ports & Adapters** | `biometria-core-api` — camadas domain/port/adapter/api/infrastructure |
| Arquitetural | **API Gateway** | `biometria-gateway` — ponto único de entrada com JWT |
| Arquitetural | **Strangler Fig** | Estratégia de migração SOAP → REST em 3 fases |
| GoF Estrutural | **Adapter** | `SoapBiometriaLegadoAdapter` implementa `BiometriaLegadoPort` |
| GoF Estrutural | **Facade** | `SoapBiometriaClient` encapsula WebServiceTemplate |
| GoF Estrutural | **Decorator** | `BiometriaMetrics.registrarApiLatencia(Supplier)` |
| GoF Comportamental | **Chain of Responsibility** | Filtros do Gateway e Core API (security, logging, metrics) |
| GoF Comportamental | **Template Method** | `OncePerRequestFilter` — `GatewaySecretFilter`, `RequestLoggingFilter` |
| DDD | **Value Object** | `Cpf` — imutável, auto-validante, máscara LGPD no `toString()` |
| DDD | **Anti-Corruption Layer** | `SoapBiometriaMapper` + `SoapFaultTranslator` — isola contrato SOAP |
| Segurança | **Defense in Depth** | JWT → X-Gateway-Secret → X-Core-Secret (3 camadas independentes) |
| Segurança | **Interceptor** | `InternalSecretInterceptor` valida X-Core-Secret no SOAP |
| Observabilidade | **Correlation ID** | `X-Correlation-Id` propagado Gateway → Core → SOAP → logs → Zipkin |

---

## Arquitetura

O modelo arquitetural completo segue o **C4 Model** (Context → Container → Component):

| Diagrama | Arquivo |
|---|---|
| C1 — Contexto do Sistema | [`docs/diagrams/c4/C1-System-Context.mmd`](docs/diagrams/c4/C1-System-Context.mmd) |
| C2 — Containers | [`docs/diagrams/c4/C2-Container.mmd`](docs/diagrams/c4/C2-Container.mmd) |
| C3 — Componentes (Core API) | [`docs/diagrams/c4/C3-Component-Core-API.mmd`](docs/diagrams/c4/C3-Component-Core-API.mmd) |
| Fluxo principal | [`docs/diagrams/sequence/consultar-biometria-fluxo-feliz.mmd`](docs/diagrams/sequence/consultar-biometria-fluxo-feliz.mmd) |
| Autenticação OAuth2 | [`docs/diagrams/sequence/autenticacao-oauth2-client-credentials.mmd`](docs/diagrams/sequence/autenticacao-oauth2-client-credentials.mmd) |
| Legado indisponível | [`docs/diagrams/sequence/legado-soap-indisponivel.mmd`](docs/diagrams/sequence/legado-soap-indisponivel.mmd) |

Documentação arquitetural completa: [`docs/architecture/DAS-v1.0-POC-Vivo-Biometria.md`](docs/architecture/DAS-v1.0-POC-Vivo-Biometria.md)

---

## Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 17 LTS |
| Framework | Spring Boot | 3.2.x |
| Gateway | Spring Cloud Gateway | 2023.0 |
| Segurança | Spring Security OAuth2 Resource Server | 6.x |
| SOAP | Spring-WS | 4.x |
| Identity Provider | Keycloak | 24.0.1 |
| Banco simulado | H2 In-Memory | 2.x |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Build | Apache Maven | 3.9+ |
| Testes | JUnit 5 + Mockito + WireMock | 5.x / 3.x / 2.35 |
| Documentação API | SpringDoc OpenAPI (Swagger UI) | 2.x |
| Logs | SLF4J + Logback + logstash-logback-encoder | 7.4 |
| Métricas | Micrometer + Spring Boot Actuator | 1.12.x |
| Tracing | Micrometer Tracing + Brave + Zipkin Reporter | 1.2.x |
| Observabilidade | Prometheus + Grafana + Loki + Promtail + Zipkin | ver compose |
| Containerização | Docker + Docker Compose | 24+ / v2 |
| API Client | Bruno | latest |
| Cobertura | JaCoCo | 0.8.11 |

---

## Pré-requisitos

Os itens abaixo são necessários para rodar o projeto **localmente** (sem Docker).
Para rodar **apenas via Docker**, basta Docker 24+ e Docker Compose v2.

| Ferramenta | Versão mínima | Verificar |
|---|---|---|
| Java (JDK) | 17 LTS | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker | 24+ | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Git | 2.x | `git --version` |
| jq _(opcional)_ | 1.6+ | `jq --version` |
| Bruno CLI _(opcional)_ | latest | `bru --version` |

---

## Instalação por Sistema Operacional

### Windows

#### Java 17

1. Baixe o instalador em [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17)
2. Execute o `.msi` e marque a opção **Set JAVA_HOME**
3. Verifique:
   ```powershell
   java -version
   # openjdk version "17.x.x"
   ```

#### Maven

```powershell
# Via winget (recomendado)
winget install Apache.Maven

# Ou baixe o .zip em https://maven.apache.org/download.cgi
# Extraia em C:\tools\maven e adicione C:\tools\maven\bin ao PATH
```

Verifique:
```powershell
mvn -version
```

#### Docker Desktop

1. Baixe em [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. Instale e habilite **WSL 2 backend**
3. Verifique:
   ```powershell
   docker --version
   docker compose version
   ```

#### Git

```powershell
winget install Git.Git
```

#### jq (opcional)

```powershell
winget install jqlang.jq
```

#### Clonar e buildar

```powershell
git clone https://github.com/seu-usuario/poc-vivo-integracao-arquitetural.git
cd poc-vivo-integracao-arquitetural
mvn clean install -DskipTests
```

---

### Linux

> Testado em Ubuntu 22.04 LTS e Debian 12. Adapte o gerenciador de pacotes se necessário.

#### Java 17

```bash
sudo apt update
sudo apt install -y temurin-17-jdk

# Alternativa via SDKMAN (recomendado para múltiplas versões)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.11-tem
```

Verifique:
```bash
java -version
```

#### Maven

```bash
# Via SDKMAN (recomendado)
sdk install maven 3.9.6

# Ou via apt
sudo apt install -y maven
```

#### Docker e Docker Compose

```bash
# Instalação via script oficial
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# Docker Compose v2 (incluído no Docker Engine moderno)
docker compose version
```

#### jq

```bash
sudo apt install -y jq
```

#### Clonar e buildar

```bash
git clone https://github.com/seu-usuario/poc-vivo-integracao-arquitetural.git
cd poc-vivo-integracao-arquitetural
mvn clean install -DskipTests
```

---

### macOS

#### Java 17

```bash
# Via Homebrew + SDKMAN (recomendado)
brew install sdkman
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.11-tem

# Ou via Homebrew direto
brew install --cask temurin@17
```

Adicione ao `~/.zshrc`:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
```

#### Maven

```bash
brew install maven
mvn -version
```

#### Docker Desktop

```bash
brew install --cask docker
# Abra o Docker Desktop e aguarde inicializar
docker --version
docker compose version
```

Para **Apple Silicon (M1/M2/M3)**, o Docker Desktop suporta imagens `linux/amd64` via emulação Rosetta.
Todas as imagens usadas nesta POC possuem builds para `arm64`.

#### jq

```bash
brew install jq
```

#### Clonar e buildar

```bash
git clone https://github.com/seu-usuario/poc-vivo-integracao-arquitetural.git
cd poc-vivo-integracao-arquitetural
mvn clean install -DskipTests
```

---

## Rodando em Desenvolvimento (Isolado)

Cada módulo pode ser executado de forma independente, sem dependência dos outros containers.

### Ordem recomendada

```
1. biometria-legacy-soap  (porta 8083) — não depende de nenhum outro
2. biometria-core-api     (porta 8082) — depende do SOAP no ar
3. biometria-gateway      (porta 8080) — depende do Core API no ar
```

### 1. biometria-legacy-soap

Módulo autossuficiente (H2 in-memory, sem dependências externas).

```bash
mvn spring-boot:run -pl biometria-legacy-soap
```

Verifique:
```bash
# Health
curl http://localhost:8083/actuator/health

# WSDL do serviço legado
curl http://localhost:8083/ws/biometria.wsdl

# H2 Console — acesse no browser
# http://localhost:8083/h2-console
# JDBC URL: jdbc:h2:mem:biometriadb  |  User: sa  |  Password: (vazio)
```

Teste SOAP direto (requer header de segurança interna):
```bash
curl -s -X POST http://localhost:8083/ws \
  -H "Content-Type: text/xml" \
  -H "X-Core-Secret: core-secret-poc" \
  -d '<?xml version="1.0"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:bio="http://vivo.com.br/poc/biometria">
  <soapenv:Body>
    <bio:ConsultarBiometriaRequest>
      <bio:cpf>52998224725</bio:cpf>
    </bio:ConsultarBiometriaRequest>
  </soapenv:Body>
</soapenv:Envelope>'
```

---

### 2. biometria-core-api

Requer `biometria-legacy-soap` no ar. Para rodar sem Keycloak local, use o perfil de testes
ou configure um Keycloak via Docker (veja abaixo).

```bash
# Subir Keycloak isolado (apenas para desenvolvimento)
docker run -d --name keycloak-dev \
  -p 8081:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v "$(pwd)/docker/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json" \
  quay.io/keycloak/keycloak:24.0.1 start-dev --import-realm

# Aguardar Keycloak inicializar (~30s) e então subir o core-api
mvn spring-boot:run -pl biometria-core-api
```

Verifique:
```bash
# Health com indicator do SOAP legado
curl http://localhost:8082/actuator/health | jq .

# Swagger UI via Gateway (recomendado — botão Authorize com OAuth2)
# http://localhost:8080/swagger-ui/index.html
#
# Swagger UI direto (dev local, sem JWT do Gateway)
# http://localhost:8082/swagger-ui/index.html

# Prometheus metrics
curl http://localhost:8082/actuator/prometheus | grep biometria_

# Validação de CPF (endpoint público)
curl http://localhost:8082/api/v1/cpf/52998224725/validar
```

---

### 3. biometria-gateway

Requer `biometria-core-api` e Keycloak no ar.

```bash
mvn spring-boot:run -pl biometria-gateway
```

Verifique:
```bash
# Health agregado
curl http://localhost:8080/actuator/health | jq .

# Proxy para o core-api
curl http://localhost:8080/management/core-api/health | jq .
```

---

## Rodando via Docker Compose

### Stack principal (aplicações + Keycloak)

```bash
cd docker

# Build e inicialização
docker compose up -d --build

# Acompanhar logs de todos os serviços
docker compose logs -f

# Verificar status e healthchecks
docker compose ps
```

Aguarde todos os serviços ficarem com status `healthy` (≈2 min na primeira execução).

```bash
# Verificação rápida de todos os healthchecks
docker compose ps --format "table {{.Name}}\t{{.Status}}"
```

---

### Stack com observabilidade completa

```bash
cd docker

docker compose \
  -f docker-compose.yml \
  -f docker-compose.observability.yml \
  up -d --build
```

Isso adiciona Prometheus, Grafana, Loki, Promtail e Zipkin à stack.

---

### Gerenciamento dos containers

```bash
# Parar sem remover volumes
docker compose stop

# Parar e remover containers (preserva imagens)
docker compose down

# Parar, remover containers e volumes (reset completo)
docker compose down -v

# Rebuild forçado de um módulo específico
docker compose build --no-cache biometria-core-api
docker compose up -d biometria-core-api
```

### Variáveis de ambiente

Crie o arquivo `docker/.env` (não versionado) baseado em `docker/.env.example`:

```bash
cp docker/.env.example docker/.env
```

| Variável | Padrão | Descrição |
|---|---|---|
| `GATEWAY_SECRET` | `gateway-secret-poc` | Secret compartilhado Gateway → Core API |
| `CORE_SECRET` | `core-secret-poc` | Secret compartilhado Core API → Legacy SOAP |

> ⚠️ **Nunca commite o arquivo `.env` com credenciais reais.**

---

## Fluxo de Autenticação OAuth2

### Passo 1 — Obter token (Client Credentials)

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=parceiro-externo" \
  -d "client_secret=parceiro-externo-secret" \
  -d "scope=biometria:read" \
  | jq -r '.access_token')

echo "Token: ${TOKEN:0:50}..."
```

### Passo 2 — Consultar biometria

```bash
curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: meu-id-de-rastreio-001" \
  "http://localhost:8080/api/v1/biometria/52998224725" \
  | jq .
```

Resposta esperada:
```json
{
  "cpf": "*********25",
  "biometriaDisponivel": true,
  "qualidadeImagem": 95,
  "imagemBase64": "...",
  "dataColeta": "2024-01-15",
  "origem": "LEGADO_SOAP",
  "correlationId": "meu-id-de-rastreio-001"
}
```

### Passo 3 — Validar CPF sem autenticação (endpoint público)

```bash
curl -s "http://localhost:8080/api/v1/cpf/52998224725/validar" | jq .
```

```json
{ "cpf": "52998224725", "valido": true }
```

### CPFs pré-cadastrados no seed

| CPF | Status |
|---|---|
| `52998224725` | ✅ Válido |
| `11144477735` | ✅ Válido |
| `87126398870` | ✅ Válido |
| `56872389510` | ✅ Válido |
| `04896019630` | ✅ Válido |
| `72687789120` | ✅ Válido |
| `98473126540` | ✅ Válido |
| `01546532755` | ✅ Válido |
| `31279940582` | ✅ Válido |
| `45261897366` | ✅ Válido |

Qualquer outro CPF retorna `404 BIOMETRIA_NAO_ENCONTRADA`.

---

## Endpoints e Mapa de URLs

### APIs de negócio

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/biometria/{cpf}` | Bearer JWT + scope `biometria:read` | Consulta biometria facial |
| `GET` | `/api/v1/cpf/{cpf}/validar` | Público | Valida dígito verificador do CPF |

### Códigos de resposta

| HTTP | Código de erro | Situação |
|---|---|---|
| `200` | — | Sucesso |
| `400` | `CPF_INVALIDO` | CPF com formato ou dígito verificador inválido |
| `401` | — | Token JWT ausente ou expirado |
| `403` | — | Token sem escopo `biometria:read` |
| `404` | `BIOMETRIA_NAO_ENCONTRADA` | CPF não cadastrado no legado |
| `502` | `LEGADO_INDISPONIVEL` | Legado SOAP inacessível |
| `504` | `TIMEOUT_LEGADO` | Timeout na chamada SOAP (> 5000ms) |

### Documentação interativa

| Interface | URL | Obs |
|---|---|---|
| **Swagger UI via Gateway** | [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html) | ✅ Recomendado — botão **Authorize** com OAuth2/JWT |
| Swagger UI (direto Core API) | [`http://localhost:8082/swagger-ui/index.html`](http://localhost:8082/swagger-ui/index.html) | Dev local — sem validação JWT do Gateway |
| OpenAPI JSON via Gateway | [`http://localhost:8080/api-docs`](http://localhost:8080/api-docs) | Importe no Postman / Insomnia |
| OpenAPI JSON (direto) | [`http://localhost:8082/api-docs`](http://localhost:8082/api-docs) | — |
| WSDL (Legacy SOAP) | [`http://localhost:8083/ws/biometria.wsdl`](http://localhost:8083/ws/biometria.wsdl) | — |

> **Como usar o Swagger UI:** clique em **Authorize** → seção **oauth2** → Client Credentials
> (`client_id: parceiro-externo`, `client_secret: parceiro-externo-secret`, scope: `biometria:read`) → **Authorize** → feche o modal → **Try it out**.

### Health e management (via Gateway)

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Health do próprio gateway |
| `GET /management/core-api/health` | Health do Core API (proxy) |
| `GET /management/core-api/metrics` | Métricas JSON do Core API |
| `GET /management/legacy-soap/health` | Health do Legacy SOAP (proxy) |
| `GET /management/legacy-soap/metrics` | Métricas JSON do SOAP |

### Observabilidade (via Gateway)

| Endpoint | Descrição |
|---|---|
| `GET /actuator/prometheus` | Métricas Prometheus do Gateway |
| `GET /management/core-api/prometheus` | Métricas Prometheus do Core API |
| `GET /management/legacy-soap/prometheus` | Métricas Prometheus do Legacy SOAP |
| `GET /monitoring/grafana` | Grafana UI (dashboards completos) |
| `GET /monitoring/prometheus` | Prometheus UI |
| `GET /monitoring/zipkin` | Zipkin UI (traces distribuídos) |

### Serviços de suporte (acesso direto)

| Serviço | URL | Credenciais |
|---|---|---|
| Keycloak Admin | [`http://localhost:8081`](http://localhost:8081) | `admin` / `admin` |
| H2 Console (SOAP) | [`http://localhost:8083/h2-console`](http://localhost:8083/h2-console) | JDBC: `jdbc:h2:mem:biometriadb` |
| Grafana | [`http://localhost:3000`](http://localhost:3000) | `admin` / `admin` |
| Prometheus | [`http://localhost:9090`](http://localhost:9090) | — |
| Zipkin | [`http://localhost:9411`](http://localhost:9411) | — |

---

## Observabilidade

Esta POC implementa os **três pilares de observabilidade** integrados ao Grafana como interface única.

### Métricas customizadas de negócio

```bash
# Verificar métricas do Core API via Gateway
curl -s http://localhost:8080/management/core-api/prometheus | grep "biometria_"

# Saída esperada
# biometria_consultas_total{application="biometria-core-api"} 42.0
# biometria_legado_latencia_seconds{quantile="0.95"} 0.087
# biometria_legado_falhas_total{application="biometria-core-api"} 0.0
```

### Logs estruturados (JSON)

Cada linha de log contém:

```json
{
  "timestamp": "2026-06-30T12:00:00.123Z",
  "level": "INFO",
  "service": "biometria-core-api",
  "correlationId": "7f1d5c90-6e09-4d8f-91a9-9b6cf7a01f4a",
  "traceId": "65b5a8e1f3c29d4a",
  "spanId": "1a2b3c4d",
  "event": "soap_call_finished",
  "durationMs": 43,
  "cpf": "*********25"
}
```

```bash
# Visualizar logs em tempo real com formatação
docker logs biometria-core-api -f | jq '.'
```

### Tracing distribuído

O `traceId` é propagado de ponta a ponta (Gateway → Core API → Legacy SOAP).
No Grafana, ao visualizar um log no Loki, um link automático leva ao trace completo no Zipkin.

```bash
# Disparar uma requisição e capturar o traceId
curl -si -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/biometria/52998224725 \
  | grep -i "x-correlation-id"

# Buscar o trace no Zipkin por traceId
# http://localhost:9411/zipkin/traces/<traceId>
```

### Acesso ao Grafana

1. Acesse [`http://localhost:3000`](http://localhost:3000) (admin / admin)
2. Menu **Explore** → selecione datasource **Loki** → filtre por `{service="biometria-core-api"}`
3. Menu **Explore** → selecione datasource **Prometheus** → query: `biometria_consultas_total`
4. Menu **Explore** → selecione datasource **Zipkin** → busque por Service Name

> Documentação completa de observabilidade: [`docs/observability/OBSERVABILIDADE.md`](docs/observability/OBSERVABILIDADE.md)

---

## Segurança

### Modelo de defesa em profundidade

```
Parceiro → [JWT OAuth2] → Gateway → [X-Gateway-Secret] → Core API → [X-Core-Secret] → SOAP
```

| Camada | Mecanismo | O que protege |
|---|---|---|
| 1 — Borda | JWT Bearer Token (OAuth2 Client Credentials) | Acesso externo ao Gateway |
| 2 — Escopo | `SCOPE_biometria:read` no JWT | Autorização por recurso |
| 3 — Interna GW→API | `X-Gateway-Secret` (header HTTP interno) | Impede acesso direto ao Core API |
| 4 — Interna API→SOAP | `X-Core-Secret` (header SOAP) | Impede acesso direto ao Legacy SOAP |
| 5 — Rede | Docker network bridge isolada | Serviços internos não acessíveis externamente |

### Keycloak — configuração da POC

| Parâmetro | Valor |
|---|---|
| Realm | `vivo-poc` |
| Client ID | `parceiro-externo` |
| Client Secret | `parceiro-externo-secret` |
| Grant Type | `client_credentials` |
| Scope | `biometria:read` |
| JWKS URI | `http://localhost:8081/realms/vivo-poc/protocol/openid-connect/certs` |

> ⚠️ Credenciais acima são exclusivas para a POC local. Em produção, rotacionar secrets e
> utilizar Vault ou equivalente para gestão de segredos.

### Boas práticas implementadas

- CPF nunca é logado em claro — mascarado como `*********XX`
- `imagemBase64` nunca aparece em logs (dado sensível LGPD)
- JWT, secrets e envelopes SOAP excluídos de todos os logs
- Usuário não-root nos containers Docker
- Headers de segurança interna não são expostos ao parceiro externo

---

## Testes

### Executar todos os testes

```bash
# Todos os módulos — compile, test, verify, JaCoCo check
mvn clean verify
```

### Por módulo

```bash
# biometria-legacy-soap (cobertura mínima: 70%)
mvn verify -pl biometria-legacy-soap

# biometria-core-api (cobertura mínima: 80%)
mvn verify -pl biometria-core-api

# biometria-gateway (cobertura mínima: 70%)
mvn verify -pl biometria-gateway
```

### Relatório de cobertura JaCoCo

```bash
# Após mvn verify, abrir relatório HTML
# Linux / macOS
open biometria-core-api/target/site/jacoco/index.html

# Windows (PowerShell)
start biometria-core-api\target\site\jacoco\index.html
```

### Smoke test da stack completa

```bash
# Requer stack Docker no ar e jq instalado
bash scripts/test-api.sh
```

### Coleções Bruno (API Client)

```bash
# Gateway — E2E completo
bru run bruno/biometria-gateway --env local

# Core API — testes isolados
bru run bruno/biometria-core-api --env local

# Legacy SOAP — testes SOAP diretos
bru run bruno/biometria-legacy-soap --env local
```

Se o Bruno CLI não estiver disponível, importe as coleções no [Bruno Desktop](https://www.usebruno.com/).

### Cobertura por módulo

| Módulo | Mínimo JaCoCo | Meta real |
|---|---|---|
| `biometria-core-api` | 80% linhas | 100% dos cenários identificados |
| `biometria-legacy-soap` | 70% linhas | Melhor esforço |
| `biometria-gateway` | 70% linhas | Melhor esforço |

---

## Estrutura do Repositório

```
poc-vivo-integracao-arquitetural/
│
├── biometria-gateway/                  ← API Gateway (Spring Cloud Gateway, :8080)
│   ├── src/main/java/br/com/vivo/poc/gateway/
│   │   ├── config/                     ← GatewayRoutesConfig, GatewaySecurityConfig
│   │   ├── filter/                     ← CorrelationId, Logging, Metrics filters
│   │   └── health/                     ← CoreApiHealthIndicator
│   └── Dockerfile
│
├── biometria-core-api/                 ← REST API + ACL SOAP (:8082)
│   ├── src/main/java/br/com/vivo/poc/biometria/
│   │   ├── domain/                     ← Biometria, Cpf (Value Object)
│   │   ├── application/usecase/        ← ConsultarBiometriaUseCase, ValidarCpfUseCase
│   │   ├── application/port/           ← BiometriaLegadoPort (interface)
│   │   └── infrastructure/
│   │       ├── adapter/soap/           ← SoapBiometriaLegadoAdapter, SoapBiometriaClient
│   │       ├── controller/             ← BiometriaController, CpfValidadorController
│   │       ├── filter/                 ← GatewaySecretFilter, CorrelationIdFilter
│   │       ├── metrics/                ← BiometriaMetrics
│   │       └── health/                 ← LegadoSoapHealthIndicator
│   └── Dockerfile
│
├── biometria-legacy-soap/              ← Serviço SOAP legado simulado (:8083)
│   ├── src/main/java/br/com/vivo/poc/legado/
│   │   ├── ws/endpoint/                ← BiometriaEndpoint (Spring-WS)
│   │   ├── ws/interceptor/             ← InternalSecretInterceptor
│   │   ├── domain/                     ← BiometriaEntity, BiometriaService
│   │   └── infrastructure/             ← SoapMetrics, seed de dados H2
│   └── Dockerfile
│
├── bruno/                              ← Coleções de API (Bruno)
│   ├── biometria-legacy-soap/
│   ├── biometria-core-api/
│   └── biometria-gateway/
│
├── docker/                             ← Infraestrutura Docker
│   ├── docker-compose.yml              ← Apps + Keycloak
│   ├── docker-compose.observability.yml← Prometheus + Grafana + Loki + Zipkin
│   ├── .env.example
│   ├── keycloak/realm-export.json
│   ├── prometheus/prometheus.yml
│   ├── promtail/promtail-config.yml
│   └── grafana/provisioning/
│
├── docs/
│   ├── architecture/
│   │   └── DAS-v1.0-POC-Vivo-Biometria.md  ← Documento Arquitetural do Sistema
│   ├── diagrams/
│   │   ├── c4/                         ← C1, C2, C3 (Mermaid)
│   │   └── sequence/                   ← Diagramas de sequência
│   └── observability/
│       └── OBSERVABILIDADE.md          ← Estratégia completa de observabilidade
│
├── scripts/
│   └── test-api.sh                     ← Smoke test end-to-end
│
├── pom.xml                             ← POM raiz multi-módulo
├── AGENTS.md                           ← Guia normativo para agentes de implementação
├── GITFLOW.md                          ← Estratégia de branches e commits
└── README.md                           ← Este arquivo
```

---

## LGPD e Dados Sensíveis

> **⚠️ Atenção — Dado Sensível (Art. 11, Lei 13.709/2018)**

A **biometria facial** é classificada como **dado sensível** pela Lei Geral de Proteção de
Dados Pessoais (LGPD). O CPF é classificado como **dado pessoal** (Art. 5º, I).

### Controles implementados nesta POC

| Controle | Implementação |
|---|---|
| Mascaramento de CPF em logs | `CpfMasker` — `*********XX` |
| Exclusão de `imagemBase64` dos logs | Regra em `RequestLoggingFilter` |
| Exclusão de JWT e secrets dos logs | Regra em todos os filtros |
| Acesso restrito por escopo | `SCOPE_biometria:read` no JWT |
| Isolamento de rede | Docker bridge network — serviços internos não expostos |
| Rastreabilidade | `X-Correlation-Id` em todas as respostas e logs |

### Controles obrigatórios em produção (fora do escopo da POC)

- TLS obrigatório em todas as comunicações (HTTPS end-to-end)
- Criptografia em repouso para `imagemBase64` no banco
- Audit log imutável de todos os acessos
- DPIA (Avaliação de Impacto à Proteção de Dados)
- Revisão do DPO antes do go-live
- Substituição do `imagemBase64` na resposta por URL temporária assinada (S3 presigned URL)
- Política de retenção de logs compatível com LGPD

> Análise completa: seção 12 do [DAS-v1.0](docs/architecture/DAS-v1.0-POC-Vivo-Biometria.md)

---

## Documentação Arquitetural

| Documento | Descrição |
|---|---|
| [`DAS-v1.0-POC-Vivo-Biometria.md`](docs/architecture/DAS-v1.0-POC-Vivo-Biometria.md) | Documento Arquitetural do Sistema — 19 seções incluindo decisões, LGPD e estratégia de migração |
| [`OBSERVABILIDADE.md`](docs/observability/OBSERVABILIDADE.md) | Estratégia completa de métricas, logs e tracing |
| [`AGENTS.md`](AGENTS.md) | Guia normativo para agentes de implementação (Claude / Codex) |
| [`GITFLOW.md`](GITFLOW.md) | Estratégia de branches, commits convencionais e tags |

---

## Contribuindo

Este projeto segue o padrão [Conventional Commits](https://www.conventionalcommits.org/) com
escopos definidos em [`GITFLOW.md`](GITFLOW.md).

### Fluxo de contribuição

```bash
# 1. Criar branch a partir de develop
git checkout develop
git checkout -b feat/core-api/minha-feature

# 2. Implementar com testes
mvn verify -pl <modulo>

# 3. Verificar cobertura JaCoCo
open <modulo>/target/site/jacoco/index.html

# 4. Executar coleção Bruno
bru run bruno/<modulo> --env local

# 5. Commitar com Conventional Commits
git commit -m "feat(core-api): descrição objetiva da mudança"

# 6. Push e Pull Request para develop
git push origin feat/core-api/minha-feature
```

### Padrão de commit

```
<tipo>(<escopo>): <descrição em imperativo, lowercase>

tipos: feat | fix | test | refactor | docs | chore | ci
escopos: legacy-soap | core-api | gateway | docker | docs | root
```

### Proibições

- ❌ Commitar com testes falhando
- ❌ Commitar com cobertura abaixo do mínimo JaCoCo
- ❌ Logar CPF, JWT, secrets, `imagemBase64` ou envelopes SOAP
- ❌ Usar `@Autowired` em campo (injeção por construtor obrigatória)
- ❌ Usar Lombok
- ❌ Permitir que `SoapFaultClientException` vaze além do `SoapFaultTranslator`

---

<div align="center">

**POC — Vivo / Telefônica Brasil**

Desenvolvido como case técnico para a posição de Arquiteto de Integração.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/)
[![Docker](https://img.shields.io/badge/Docker-24-2496ED?style=flat-square&logo=docker&logoColor=white)](https://docker.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-24-4D4D4D?style=flat-square&logo=keycloak&logoColor=white)](https://keycloak.org/)

</div>
