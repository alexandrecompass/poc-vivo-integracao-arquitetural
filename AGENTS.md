# AGENTS.md — Guia Normativo para Agentes de Implementação

> Este documento é a fonte de verdade para qualquer agente (Claude, Codex ou humano)
> que implementar código neste repositório. Leia completamente antes de escrever qualquer linha.

---

## 1. Contexto do Projeto

POC técnica para o case de Arquiteto de Integração da Vivo.

Objetivo: expor biometria facial de um sistema legado Oracle/SOAP via API REST moderna,
segura, versionada e observável.

Repositório: `poc-vivo-integracao-arquitetural`

Documentação arquitetural completa: `docs/architecture/DAS-v1.0-POC-Vivo-Biometria.md`

---

## 2. Estrutura do Repositório

```
poc-vivo-integracao-arquitetural/
├── biometria-gateway/          ← Módulo 1: API Gateway (Spring Cloud Gateway)
├── biometria-core-api/         ← Módulo 2: REST API + integração SOAP
├── biometria-legacy-soap/      ← Módulo 3: Serviço SOAP fake (legado simulado)
├── bruno/
│   ├── biometria-legacy-soap/  ← Coleção Bruno para o módulo SOAP
│   ├── biometria-core-api/     ← Coleção Bruno para o módulo REST
│   └── biometria-gateway/      ← Coleção Bruno para o módulo Gateway (E2E)
├── docker/
│   ├── docker-compose.yml      ← Orquestração local completa
│   └── keycloak/
│       └── realm-export.json   ← Realm e client pré-configurados
├── docs/                       ← Documentação arquitetural (não modificar)
├── scripts/
│   └── test-api.sh             ← Script de smoke test manual
├── pom.xml                     ← POM raiz multi-módulo
├── .gitignore
├── AGENTS.md                   ← Este arquivo
├── GITFLOW.md
└── README.md
```

---

## 3. Stack Tecnológica

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework principal | Spring Boot 3.2.x |
| Gateway | Spring Cloud Gateway (módulo biometria-gateway) |
| SOAP | Spring-WS (módulo biometria-legacy-soap) |
| Cliente SOAP | Spring-WS WebServiceTemplate (módulo biometria-core-api) |
| Segurança | Spring Security OAuth2 Resource Server (JWT) |
| Banco de dados | H2 in-memory (simula Oracle) |
| ORM | Spring Data JPA + Hibernate |
| Build | Maven 3.9+ (multi-módulo) |
| Testes | JUnit 5 + Mockito + Spring Boot Test |
| Documentação API | SpringDoc OpenAPI 2.x (Swagger UI) |
| Logs | SLF4J + Logback (JSON estruturado via logstash-logback-encoder) |
| Métricas | Spring Boot Actuator + Micrometer |
| Identity Provider | Keycloak 24+ (rodando via Docker) |
| Containerização | Docker + Docker Compose |

---

## 4. Módulos e Responsabilidades

### 4.1 biometria-legacy-soap (Módulo 3 — implementar primeiro)

**O que é:** simulação do sistema legado. Expõe um serviço SOAP que consulta uma base H2.

**Responsabilidades:**
- Expor endpoint SOAP: `http://localhost:8083/ws/biometria`
- Operação SOAP: `ConsultarBiometria` recebe CPF, retorna dados fictícios
- Banco H2 com tabela `BIOMETRIA` populada via seed com CPFs válidos
- Simular cenários de erro (CPF não encontrado, falha técnica)
- Expor WSDL em: `http://localhost:8083/ws/biometria.wsdl`

**NÃO é responsabilidade deste módulo:**
- Autenticar requisições
- Conhecer REST, JWT, OAuth2 ou API Gateway
- Expor métricas ou health checks públicos (apenas `/actuator/health` interno)

---

### 4.2 biometria-core-api (Módulo 2 — implementar segundo)

**O que é:** microserviço REST que implementa o caso de uso de consulta de biometria.

**Responsabilidades:**
- Expor `GET /api/v1/biometria/{cpf}` (autenticado)
- Expor `GET /api/v1/cpf/{cpf}/validar` (público, sem autenticação)
- Validar JWT emitido pelo Keycloak (Resource Server)
- Verificar escopo `biometria:read`
- Validar CPF (formato + algoritmo)
- Chamar o SOAP legado via `WebServiceTemplate`
- Traduzir resposta SOAP para DTO REST
- Mascarar CPF nos logs
- Propagar e gerar `X-Correlation-Id`
- Tratar erros com `GlobalExceptionHandler`
- Expor OpenAPI/Swagger em `/swagger-ui.html`
- Expor `/actuator/health` e `/actuator/metrics`

**NÃO é responsabilidade deste módulo:**
- Autenticar no lugar do Gateway (o Gateway já valida antes de rotear)
- Conhecer detalhes do Oracle real
- Armazenar dados

---

### 4.3 biometria-gateway (Módulo 1 — implementar por último)

**O que é:** ponto único de entrada para parceiros externos.

**Responsabilidades:**
- Rotear `GET /api/v1/biometria/**` para `biometria-core-api:8082`
- Validar JWT via JWKS do Keycloak
- Verificar escopo `biometria:read`
- Gerar `X-Correlation-Id` se ausente e propagar downstream
- Adicionar log de borda (entrada + resposta + duração)
- Retornar `X-Correlation-Id` na resposta ao parceiro
- Expor `/actuator/health`

**NÃO é responsabilidade deste módulo:**
- Conter regra de negócio
- Conhecer SOAP
- Validar CPF

---

## 5. Portas e Endereços

| Serviço | Porta | URL Base |
|---|---|---|
| biometria-gateway | 8080 | `http://localhost:8080` |
| biometria-core-api | 8082 | `http://localhost:8082` |
| biometria-legacy-soap | 8083 | `http://localhost:8083` |
| Keycloak | 8081 | `http://localhost:8081` |
| H2 Console (legacy-soap) | 8083 | `http://localhost:8083/h2-console` |

---

## 6. Pacote Base

```
br.com.vivo.poc
```

| Módulo | Pacote raiz |
|---|---|
| biometria-legacy-soap | `br.com.vivo.poc.legado` |
| biometria-core-api | `br.com.vivo.poc.biometria` |
| biometria-gateway | `br.com.vivo.poc.gateway` |

---

## 7. Estrutura de Pacotes — biometria-core-api

```
br.com.vivo.poc.biometria
├── api
│   ├── BiometriaController          ← GET /api/v1/biometria/{cpf}
│   ├── CpfValidadorController       ← GET /api/v1/cpf/{cpf}/validar
│   ├── dto
│   │   ├── BiometriaResponse        ← DTO de resposta REST
│   │   ├── CpfValidacaoResponse     ← DTO de resposta da validação
│   │   └── ErrorResponse            ← DTO de erro padronizado
│   └── mapper
│       └── BiometriaResponseMapper  ← Domínio → DTO REST
│
├── application
│   ├── ConsultarBiometriaUseCase    ← Orquestra consulta
│   ├── ValidarCpfUseCase            ← Valida CPF (algoritmo)
│   └── exception
│       ├── CpfInvalidoException
│       ├── BiometriaNaoEncontradaException
│       ├── LegadoIndisponivelException
│       └── LegadoTimeoutException
│
├── domain
│   ├── Biometria                    ← Modelo interno
│   ├── Cpf                          ← Value Object com validação
│   ├── BiometriaStatus
│   └── OrigemBiometria
│
├── port
│   └── BiometriaLegadoPort          ← Interface de saída
│
├── adapter
│   └── soap
│       ├── SoapBiometriaLegadoAdapter   ← Implementa BiometriaLegadoPort
│       ├── SoapBiometriaClient          ← Chama WebServiceTemplate
│       ├── SoapBiometriaMapper          ← SOAP → domínio
│       └── SoapFaultTranslator          ← SOAP Fault → exceção interna
│
└── infrastructure
    ├── config
    │   ├── SoapClientConfig         ← WebServiceTemplate bean
    │   ├── SecurityConfig           ← OAuth2 Resource Server
    │   └── ObservabilityConfig      ← Filtros e beans de observabilidade
    ├── error
    │   └── GlobalExceptionHandler   ← @ControllerAdvice
    ├── logging
    │   ├── CorrelationIdFilter      ← Gera/propaga X-Correlation-Id
    │   ├── RequestLoggingFilter     ← Loga entrada/saída com duração
    │   └── CpfMasker                ← Mascara CPF nos logs
    └── metrics
        └── BiometriaMetrics         ← Micrometer counters e timers
```

---

## 8. Estrutura de Pacotes — biometria-legacy-soap

```
br.com.vivo.poc.legado
├── ws
│   ├── BiometriaEndpoint            ← @Endpoint SOAP
│   └── config
│       └── WsConfig                 ← MessageDispatcherServlet, WSDL
├── domain
│   └── Biometria                    ← Entidade JPA
├── repository
│   └── BiometriaRepository          ← Spring Data JPA
├── service
│   └── BiometriaService             ← Lógica de consulta
└── seed
    └── BiometriaSeedLoader          ← @Component que popula H2 na startup
```

---

## 9. Estrutura de Pacotes — biometria-gateway

```
br.com.vivo.poc.gateway
├── GatewayApplication               ← Main class
├── config
│   ├── GatewayRoutesConfig          ← Rotas Spring Cloud Gateway
│   └── SecurityConfig               ← OAuth2 JWT validation
└── filter
    ├── CorrelationIdGatewayFilter    ← Gera/propaga X-Correlation-Id
    └── LoggingGatewayFilter          ← Log de borda
```

---

## 10. Seed de CPFs Válidos

O módulo `biometria-legacy-soap` deve popular o H2 com **pelo menos 10 CPFs válidos** na startup.

**Algoritmo de validação do CPF (para gerar CPFs válidos no seed):**

O CPF tem 11 dígitos. Os dois últimos são dígitos verificadores.

Cálculo do 1º dígito verificador:
- Multiplique os 9 primeiros dígitos por 10, 9, 8, 7, 6, 5, 4, 3, 2 (do primeiro ao nono)
- Some os produtos
- Calcule resto = soma % 11
- Se resto < 2 → dígito = 0; senão → dígito = 11 - resto

Cálculo do 2º dígito verificador:
- Multiplique os 10 primeiros dígitos (incluindo o 1º verificador) por 11, 10, 9, 8, 7, 6, 5, 4, 3, 2
- Some os produtos
- Calcule resto = soma % 11
- Se resto < 2 → dígito = 0; senão → dígito = 11 - resto

**CPFs válidos pré-gerados para o seed (use estes exatamente):**

```
529.982.247-25
111.444.777-35
871.263.988-70
568.723.895-10
048.960.196-30
726.877.891-20
984.731.265-40
015.465.327-55
312.799.405-82
452.618.973-66
```

Estes CPFs passam no algoritmo de validação. Armazene apenas dígitos (sem pontuação) no banco.

---

## 11. Contrato SOAP (biometria-legacy-soap)

Namespace: `http://vivo.com.br/poc/biometria`

Request:
```xml
<ConsultarBiometriaRequest>
  <cpf>52998224725</cpf>
</ConsultarBiometriaRequest>
```

Response (encontrado):
```xml
<ConsultarBiometriaResponse>
  <cpf>52998224725</cpf>
  <biometriaDisponivel>true</biometriaDisponivel>
  <imagemBase64>aW1hZ2VtLWZpY3RpY2lhLWJhc2U2NA==</imagemBase64>
  <origem>LEGADO_SOAP</origem>
  <dataConsulta>2026-06-30T12:00:00</dataConsulta>
</ConsultarBiometriaResponse>
```

Response (não encontrado): retornar SOAP Fault com código `BIOMETRIA_NAO_ENCONTRADA`.

---

## 12. Endpoint REST (biometria-core-api)

### GET /api/v1/biometria/{cpf}

Autenticado. Requer escopo `biometria:read`.

CPF: 11 dígitos numéricos sem pontuação.

Response 200:
```json
{
  "cpf": "***.982.247-**",
  "biometriaDisponivel": true,
  "imagemBase64": "aW1hZ2VtLWZpY3RpY2lhLWJhc2U2NA==",
  "origem": "LEGADO_SOAP",
  "dataConsulta": "2026-06-30T12:00:00Z",
  "correlationId": "uuid-aqui"
}
```

### GET /api/v1/cpf/{cpf}/validar

**Público** (sem autenticação). Valida apenas o número do CPF pelo algoritmo.

Response 200:
```json
{
  "cpf": "***.982.247-**",
  "valido": true,
  "mensagem": "CPF válido."
}
```

Response 200 (inválido):
```json
{
  "cpf": "***.000.000-**",
  "valido": false,
  "mensagem": "CPF inválido."
}
```

> Nota: o endpoint retorna HTTP 200 em ambos os casos. `valido: false` não é erro — é resultado da validação.

---

## 13. Keycloak — Configuração do Realm

Realm: `vivo-poc`

Client:
- Client ID: `parceiro-externo`
- Grant type: Client Credentials
- Client secret: `parceiro-externo-secret`
- Scope: `biometria:read`

Token URL: `http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token`

JWKS URL: `http://localhost:8081/realms/vivo-poc/protocol/openid-connect/certs`

O arquivo `docker/keycloak/realm-export.json` deve conter o realm pré-configurado para import automático via variável `KC_IMPORT`.

---

## 14. Segurança

- `biometria-core-api` é Resource Server. Valida JWT no header `Authorization: Bearer <token>`.
- O escopo deve ser verificado como `SCOPE_biometria:read` no Spring Security.
- O endpoint `/api/v1/cpf/{cpf}/validar` é **público** — deve ser configurado como `permitAll()` no `SecurityConfig`.
- O endpoint `/actuator/health` é **público** em todos os módulos.
- Nenhum outro endpoint é público no core-api.

---

## 15. Estratégia de Logging

### 15.1 Biblioteca e formato

Todos os módulos usam **SLF4J + Logback** com saída **JSON estruturado** via
`net.logstash.logback:logstash-logback-encoder:7.4`.

Nenhum módulo usa `System.out.println`. Nenhum módulo usa Lombok `@Slf4j` —
declarar o logger explicitamente:

```java
private static final Logger log = LoggerFactory.getLogger(MinhaClasse.class);
```

---

### 15.2 logback-spring.xml padrão (usar em todos os módulos)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <springProperty scope="context" name="appName"
                  source="spring.application.name" defaultValue="unknown-service"/>

  <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeContext>false</includeContext>
      <customFields>{"service":"${appName}"}</customFields>
      <fieldNames>
        <timestamp>timestamp</timestamp>
        <message>message</message>
        <logger>logger</logger>
        <level>level</level>
        <thread>thread</thread>
      </fieldNames>
      <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
        <maxDepthPerCause>8</maxDepthPerCause>
        <shortenedClassNameLength>20</shortenedClassNameLength>
        <rootCauseFirst>true</rootCauseFirst>
      </throwableConverter>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON_CONSOLE"/>
  </root>

  <!-- Reduzir ruído de libs internas -->
  <logger name="org.springframework" level="WARN"/>
  <logger name="org.hibernate" level="WARN"/>
  <logger name="com.zaxxer.hikari" level="WARN"/>
  <logger name="org.apache.cxf" level="WARN"/>

</configuration>
```

---

### 15.3 Campos obrigatórios em todo log

Inserir via MDC antes de qualquer log da requisição:

| Campo MDC | Tipo | Descrição |
|---|---|---|
| `correlationId` | UUID string | Gerado no Gateway, propagado via `X-Correlation-Id` |
| `service` | string | Nome do módulo (`spring.application.name`) |

Campos estruturados adicionais (passar como argumentos estruturados, não concatenar na string):

```java
log.info("soap_call_started",
    StructuredArguments.keyValue("event", "soap_call_started"),
    StructuredArguments.keyValue("cpf", cpfMasker.mask(cpf)),
    StructuredArguments.keyValue("correlationId", correlationId)
);
```

---

### 15.4 Eventos de log obrigatórios por camada

#### biometria-gateway

| Evento | Nível | Quando |
|---|---|---|
| `gateway_request_received` | INFO | Entrada de toda requisição |
| `gateway_token_valid` | DEBUG | JWT validado com sucesso |
| `gateway_token_invalid` | WARN | JWT ausente, expirado ou inválido |
| `gateway_scope_denied` | WARN | Token sem escopo `biometria:read` |
| `gateway_correlation_generated` | DEBUG | `X-Correlation-Id` gerado pelo gateway |
| `gateway_request_routed` | INFO | Roteamento para downstream concluído |
| `gateway_response_sent` | INFO | Resposta enviada ao parceiro (status + duração) |
| `gateway_downstream_error` | ERROR | Downstream retornou 5xx |

#### biometria-core-api

| Evento | Nível | Quando |
|---|---|---|
| `api_request_received` | INFO | Entrada no controller |
| `cpf_validation_failed` | WARN | CPF inválido rejeitado |
| `usecase_started` | INFO | Use case iniciado com CPF mascarado |
| `soap_call_started` | INFO | Chamada ao SOAP legado iniciada |
| `soap_call_finished` | INFO | Chamada ao SOAP concluída (duração em ms) |
| `soap_fault_received` | WARN | SOAP retornou fault |
| `soap_timeout` | ERROR | Timeout na chamada SOAP |
| `soap_unavailable` | ERROR | SOAP inacessível |
| `biometria_not_found` | WARN | CPF não encontrado no legado |
| `api_response_sent` | INFO | Resposta REST enviada (status + duração) |
| `gateway_secret_missing` | WARN | Header `X-Gateway-Secret` ausente |
| `gateway_secret_invalid` | WARN | Header `X-Gateway-Secret` inválido |

#### biometria-legacy-soap

| Evento | Nível | Quando |
|---|---|---|
| `soap_request_received` | INFO | Requisição SOAP recebida |
| `core_secret_missing` | WARN | Header `X-Core-Secret` ausente |
| `core_secret_invalid` | WARN | Header `X-Core-Secret` inválido |
| `acesso_direto_bloqueado` | WARN | Requisição bloqueada pelo interceptor |
| `db_query_started` | DEBUG | Consulta ao H2 iniciada |
| `db_query_finished` | DEBUG | Consulta ao H2 concluída |
| `biometria_found` | INFO | Biometria localizada no banco |
| `biometria_not_found` | WARN | CPF não encontrado no banco |
| `soap_response_sent` | INFO | Resposta SOAP enviada |

---

### 15.5 Regras absolutas de sanitização

**Nunca logar — em nenhuma camada, em nenhum nível:**

| Dado | Por quê |
|---|---|
| CPF completo (11 dígitos) | Dado pessoal — LGPD Art. 11 |
| `imagemBase64` | Dado biométrico sensível |
| Token JWT completo | Credencial de acesso |
| SOAP envelope completo | Pode conter qualquer um dos acima |
| Header `Authorization` completo | Credencial |
| Header `X-Core-Secret` / `X-Gateway-Secret` | Segredo interno |

**Máscara de CPF obrigatória:**

Implementar `CpfMasker` como componente `@Component` reutilizável em todos os módulos.
Regra: exibir apenas os 2 últimos dígitos.

```java
// Entrada:  "52998224725"
// Saída:    "***.982.247-**"  ← não — use formato simples
// Saída:    "*********25"     ← aceitável na POC
```

Formato aceitável na POC: `"*".repeat(9) + cpf.substring(9)` → `"*********25"`.

---

### 15.6 Propagação do Correlation ID

Fluxo de propagação:

```
Parceiro → [X-Correlation-Id: uuid] → Gateway
  Gateway: se ausente, gera UUID v4
  Gateway → [X-Correlation-Id: uuid] → Core API (header HTTP)
    Core API: insere no MDC: MDC.put("correlationId", uuid)
    Core API → [X-Correlation-Id: uuid] → SOAP (header HTTP customizado via MessageCallback)
      SOAP: insere no MDC se presente
  Core API ← resposta com X-Correlation-Id no header
Gateway ← propaga X-Correlation-Id na resposta
Parceiro ← [X-Correlation-Id: uuid] na resposta
```

**Regra:** todo log gerado durante uma requisição deve conter o `correlationId` no MDC.
Limpar o MDC no `finally` do filtro após a resposta.

---

## 16. Estratégia de Tracing

### 16.1 Abordagem na POC

O tracing distribuído completo (Jaeger, Zipkin, OTLP) é infraestrutura de produção.
Na POC, o tracing é implementado em **dois níveis**:

**Nível 1 — Correlation ID manual (obrigatório agora):**
Já coberto na seção 15.6. Garante rastreabilidade ponta a ponta sem dependência externa.

**Nível 2 — Micrometer Tracing (preparação, ativar na POC):**
Adicionar as dependências abaixo em `biometria-core-api` e `biometria-gateway`.
Elas instrumentam automaticamente `WebClient`, `RestTemplate`, `WebServiceTemplate`
e Spring MVC sem alterar código de negócio.

---

### 16.2 Dependências de tracing (adicionar nos POMs)

```xml
<!-- Micrometer Tracing Bridge — usa Brave/B3 por padrão -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Exportador para logs (sem Zipkin externo na POC) -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing</artifactId>
</dependency>

<!-- Integração com Logback: injeta traceId e spanId no MDC automaticamente -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-reporter-wavefront</artifactId>
  <scope>test</scope>
</dependency>
```

Na POC, usar apenas `micrometer-tracing-bridge-brave`. Isso injeta `traceId` e `spanId`
automaticamente no MDC do Logback, fazendo com que todos os logs já contenham esses campos
sem nenhum código adicional.

---

### 16.3 Campos de tracing no log JSON

Com `micrometer-tracing-bridge-brave` ativo, cada linha de log JSON terá:

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

`traceId` e `spanId` são injetados automaticamente. `correlationId` é inserido via MDC
pelo `CorrelationIdFilter`.

---

### 16.4 Sampling

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% na POC; reduzir em produção (ex: 0.1)
```

---

### 16.5 Preparação para OpenTelemetry (produção futura)

Quando a POC evoluir para produção, substituir `micrometer-tracing-bridge-brave` por:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

E configurar:

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

Nenhuma mudança de código de negócio será necessária — apenas troca de dependência e config.

---

## 17. Estratégia de Métricas

### 17.1 Stack

**Micrometer** como façade de métricas + **Spring Boot Actuator** para exposição.
Na POC: métricas expostas via `/actuator/metrics` (JSON) e `/actuator/prometheus` (Prometheus scrape format).
Em produção: Prometheus + Grafana coletam e exibem os dados.

---

### 17.2 Dependências (adicionar nos POMs)

```xml
<!-- Actuator -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer core (incluído pelo Actuator, mas declarar explicitamente) -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-core</artifactId>
</dependency>

<!-- Exportador Prometheus (expõe /actuator/prometheus) -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

### 17.3 Configuração do Actuator (application.yml — todos os módulos)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.75, 0.95, 0.99
  tracing:
    sampling:
      probability: 1.0
```

---

### 17.4 Métricas automáticas (Spring Boot Actuator / Micrometer)

Estas métricas são geradas **automaticamente** sem código adicional:

| Métrica | Descrição |
|---|---|
| `http.server.requests` | Latência, status e método de toda requisição HTTP recebida |
| `jvm.memory.used` | Uso de memória JVM |
| `jvm.gc.pause` | Pausas de GC |
| `process.cpu.usage` | CPU do processo |
| `hikaricp.connections.*` | Pool de conexões (módulo legacy-soap) |
| `spring.webservices.requests` | Requisições SOAP recebidas (legacy-soap) |

---

### 17.5 Métricas customizadas obrigatórias — biometria-core-api

Criar `br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics` com `MeterRegistry` injetado.

```java
// Contadores
Counter consultasTotal;       // biometria.consultas.total
Counter consultasSucesso;     // biometria.consultas.sucesso
Counter consultasNaoEncontrada; // biometria.consultas.nao_encontrada
Counter legadoFalhas;         // biometria.legado.falhas

// Timers (latência)
Timer legadoLatencia;         // biometria.legado.latencia  ← latência da chamada SOAP
Timer apiLatencia;            // biometria.api.latencia     ← latência total do endpoint

// Gauge (estado)
// Não obrigatório na POC, mas documentar como evolução futura:
// biometria.legado.disponivel  ← 1.0 se SOAP respondeu na última chamada, 0.0 se falhou
```

Todos os contadores e timers devem incluir a tag `service` com o nome do módulo.

**Implementação do Timer SOAP:**

```java
return legadoLatencia.record(() -> soapClient.consultar(cpf));
```

**Incremento de contador no use case:**

```java
try {
    Biometria resultado = legadoPort.consultarPorCpf(cpf);
    metrics.incrementarSucesso();
    return resultado;
} catch (BiometriaNaoEncontradaException e) {
    metrics.incrementarNaoEncontrada();
    throw e;
} catch (Exception e) {
    metrics.incrementarFalhaLegado();
    throw e;
} finally {
    metrics.incrementarTotal();
}
```

---

### 17.6 Métricas customizadas — biometria-gateway

Criar `br.com.vivo.poc.gateway.filter.GatewayMetricsFilter` como `GlobalFilter`:

```java
// Counters
gateway.requests.total          // todas as requisições recebidas
gateway.requests.authorized     // requisições com JWT válido
gateway.requests.denied.notoken // sem token
gateway.requests.denied.scope   // token sem escopo

// Timer
gateway.routing.latencia        // tempo entre receber e rotear downstream
```

---

### 17.7 Métricas customizadas — biometria-legacy-soap

Criar `br.com.vivo.poc.legado.ws.metrics.SoapMetrics`:

```java
// Counters
soap.requests.total             // total de requisições SOAP recebidas
soap.requests.blocked           // bloqueadas pelo InternalSecretInterceptor
soap.requests.success           // respondidas com sucesso
soap.requests.not_found         // CPF não encontrado

// Timer
soap.db.latencia               // latência da consulta ao H2
```

---

### 17.8 Health checks customizados

#### biometria-core-api — HealthIndicator do SOAP

Criar `br.com.vivo.poc.biometria.infrastructure.health.LegadoSoapHealthIndicator`
implementando `HealthIndicator`:

```java
@Override
public Health health() {
    try {
        // ping leve no SOAP — buscar WSDL ou CPF de teste
        soapClient.consultarPorCpf("52998224725");
        return Health.up()
            .withDetail("legado", "disponível")
            .withDetail("url", soapUrl)
            .build();
    } catch (Exception e) {
        return Health.down()
            .withDetail("legado", "indisponível")
            .withDetail("erro", e.getMessage())
            .build();
    }
}
```

Exposto em `/actuator/health/legadoSoap`.

#### biometria-gateway — HealthIndicator do Core API

Criar `br.com.vivo.poc.gateway.health.CoreApiHealthIndicator` que faz GET em
`http://biometria-core-api:8082/actuator/health` e repassa o status.

Exposto em `/actuator/health/coreApi`.

---

### 17.9 Endpoint Prometheus e exemplo de scrape

Com `micrometer-registry-prometheus` ativo, as métricas ficam disponíveis em:

```
GET http://localhost:<porta>/actuator/prometheus
```

Exemplo de saída esperada:

```
# HELP biometria_consultas_total Total de consultas de biometria
# TYPE biometria_consultas_total counter
biometria_consultas_total{application="biometria-core-api",} 42.0

# HELP biometria_legado_latencia_seconds Latência da chamada ao SOAP legado
# TYPE biometria_legado_latencia_seconds summary
biometria_legado_latencia_seconds{application="biometria-core-api",quantile="0.95",} 0.087
biometria_legado_latencia_seconds_count{application="biometria-core-api",} 42.0
biometria_legado_latencia_seconds_sum{application="biometria-core-api",} 1.834
```

O agente deve verificar que o endpoint `/actuator/prometheus` responde com HTTP 200
e contém pelo menos uma métrica customizada `biometria_*` antes de commitar.

---

## 19. Tratamento de Erros

Todos os erros devem retornar `ErrorResponse`:

```json
{
  "timestamp": "2026-06-30T12:00:00Z",
  "status": 502,
  "error": "LEGADO_INDISPONIVEL",
  "message": "Não foi possível consultar a biometria neste momento.",
  "path": "/api/v1/biometria/52998224725",
  "correlationId": "uuid-aqui"
}
```

Mapeamento de exceções para HTTP:

| Exceção | HTTP | Código |
|---|---|---|
| `CpfInvalidoException` | 400 | `CPF_INVALIDO` |
| `BiometriaNaoEncontradaException` | 404 | `BIOMETRIA_NAO_ENCONTRADA` |
| `LegadoIndisponivelException` | 502 | `LEGADO_INDISPONIVEL` |
| `LegadoTimeoutException` | 504 | `TIMEOUT_LEGADO` |
| `Exception` | 500 | `ERRO_INTERNO` |

---

## 17. Testes

**Cobertura mínima obrigatória (JaCoCo, contador `LINE`, bundle completo):** ver tabela de
mínimos por módulo na seção 24. Cada módulo declara `jacoco.coverage.minimum` no próprio
`pom.xml`. Verificado via `mvn verify -pl <módulo>` (goal `jacoco:check`).

**biometria-legacy-soap:**
- Testes unitários: `BiometriaServiceTest`
- Testes de integração: `BiometriaEndpointIT` usando Spring Boot Test + MockWebServiceClient

**biometria-core-api:**
- `CpfTest` — testa o value object com CPFs válidos e inválidos (usar os 10 do seed + pelo menos 5 inválidos)
- `ConsultarBiometriaUseCaseTest` — mock da `BiometriaLegadoPort`
- `ValidarCpfUseCaseTest`
- `SoapBiometriaLegadoAdapterTest` — mock do `SoapBiometriaClient`
- `SoapFaultTranslatorTest`
- `BiometriaControllerIT` — `@SpringBootTest` com mock do SOAP
- `CpfValidadorControllerIT`

**biometria-gateway:**
- `CorrelationIdGatewayFilterTest`
- `GatewayRoutesIT` — WebTestClient com mock do downstream

---

## 18. Regras Arquiteturais — Proibições

1. `BiometriaController` não chama `SoapBiometriaClient` diretamente.
2. `ConsultarBiometriaUseCase` não importa nada do pacote `adapter.soap`.
3. `ConsultarBiometriaUseCase` depende apenas de `BiometriaLegadoPort` (interface).
4. DTO REST (`BiometriaResponse`) nunca reutiliza classe SOAP gerada.
5. Exceção SOAP (`SoapFaultClientException`) nunca vaza para além do `SoapFaultTranslator`.
6. Nenhuma camada acima de `infrastructure` conhece `WebServiceTemplate`.
7. `biometria-gateway` não tem dependência de `biometria-core-api` ou `biometria-legacy-soap`.

---

## 19. Ordem de Implementação Recomendada

1. `biometria-legacy-soap` — SOAP + H2 + seed
2. `biometria-core-api` — domínio, use cases, adapter SOAP, controller REST, testes
3. `biometria-gateway` — rotas, filtros, segurança JWT
4. `docker/docker-compose.yml` + `docker/keycloak/realm-export.json`
5. `README.md` raiz
6. `scripts/test-api.sh`

---

## 20. Convenções de Código

- Java 17: usar records para DTOs imutáveis onde aplicável
- `final` em campos de construtor (injeção por construtor obrigatória — sem `@Autowired` em campo)
- Sem Lombok (manter código legível e explícito na POC)
- Sem comentários óbvios (`// getter`, `// constructor`)
- Nomes em inglês para código, português para mensagens de negócio
- Constantes em `UPPER_SNAKE_CASE`
- Sem `System.out.println` em nenhuma camada — apenas SLF4J

---

## 21. Convenção de Commit

Seguir `GITFLOW.md`. Cada módulo terá seu próprio commit de implementação inicial.

**Validação manual obrigatória antes de commitar:** nenhum agente (Claude, Codex ou humano) deve
criar commits em nome do usuário sem validação manual prévia. Após implementar um módulo, o agente
deve deixar o código compilando, os testes passando e a aplicação pronta para rodar (ex.: via
IntelliJ), além de uma coleção Bruno (`<modulo>/bruno/`) pronta para o usuário disparar as
requisições manualmente e conferir as respostas. Só prosseguir com `git add`/`git commit`/`git push`
depois que o usuário confirmar explicitamente que validou o resultado.

Formato: `<tipo>(<escopo>): <descrição>`

Exemplos:
- `feat(legacy-soap): implementar endpoint SOAP com seed de CPFs válidos`
- `feat(core-api): implementar use case de consulta de biometria com adapter SOAP`
- `feat(gateway): configurar rotas e validação JWT com Keycloak`
- `test(core-api): adicionar testes unitários e de integração`

---

## 22. Coleções Bruno (obrigatório em todo módulo)

Cada módulo deve gerar uma coleção Bruno em `bruno/<nome-do-modulo>/`.

Bruno usa arquivos `.bru` (texto puro, versionável) e um `bruno.json` de coleção.

### Estrutura obrigatória por módulo

```
bruno/biometria-legacy-soap/
├── bruno.json
├── environments/
│   └── local.bru
├── consultar-biometria-cpf-valido.bru
├── consultar-biometria-cpf-nao-encontrado.bru
└── consultar-biometria-sem-secret-bloqueado.bru

bruno/biometria-core-api/
├── bruno.json
├── environments/
│   └── local.bru
├── validar-cpf-valido.bru
├── validar-cpf-invalido.bru
├── consultar-biometria-autenticado.bru
├── consultar-biometria-sem-token.bru
├── consultar-biometria-sem-secret-gateway-bloqueado.bru
├── consultar-biometria-cpf-nao-encontrado.bru
└── consultar-biometria-cpf-invalido.bru

bruno/biometria-gateway/
├── bruno.json
├── environments/
│   └── local.bru
├── obter-token-keycloak.bru
├── consultar-biometria-e2e-sucesso.bru
├── consultar-biometria-e2e-sem-token.bru
├── consultar-biometria-e2e-escopo-invalido.bru
└── consultar-biometria-e2e-cpf-nao-encontrado.bru
```

### Formato `.bru` obrigatório

```bru
meta {
  name: <nome legível do cenário>
  type: http
  seq: <número de ordem>
}

<método em minúsculas> {
  url: {{baseUrl}}/caminho
  body: <none|json|xml>
  auth: <none|bearer>
}

headers {
  Content-Type: application/json
  X-Correlation-Id: test-correlation-id-001
}

body:json {
  {}
}

assert {
  res.status: eq <código esperado>
}
```

### Ambiente local (`local.bru`)

```bru
vars {
  baseUrl: http://localhost:<porta>
  token: <deixar vazio — preencher após obter-token>
}
```

### Regra

O agente deve executar todos os arquivos `.bru` via CLI do Bruno (`bru run`) antes de commitar.
Se o Bruno CLI não estiver disponível, executar via `curl` equivalente e logar o resultado.

---

## 23. Testes de Integração E2E por Módulo

Cada módulo deve conter **um arquivo de teste E2E** que inicia o contexto Spring Boot completo
(`@SpringBootTest(webEnvironment = RANDOM_PORT)`) e executa o fluxo real, sem mocks.

### biometria-legacy-soap — E2E

Classe: `br.com.vivo.poc.legado.ws.BiometriaEndpointE2ETest`

- Inicia o servidor na porta aleatória
- Usa `WebServiceTemplate` apontando para `http://localhost:{porta}/ws`
- Envia header `X-Core-Secret` correto
- Cenários:
  1. CPF `52998224725` → `biometriaDisponivel = true`
  2. CPF `00000000000` → SOAP Fault `BIOMETRIA_NAO_ENCONTRADA`
  3. Chamada sem `X-Core-Secret` → SOAP Fault `UNAUTHORIZED`

### biometria-core-api — E2E

Classe: `br.com.vivo.poc.biometria.BiometriaE2ETest`

- Inicia o servidor na porta aleatória
- Sobe `biometria-legacy-soap` via `@SpringBootTest` separado ou via WireMock simulando o SOAP
- Envia header `X-Gateway-Secret` correto + Bearer token JWT (gerado por `JwtBuilder` de teste)
- Cenários:
  1. CPF válido com token e secret → 200 com dados mascarados
  2. CPF válido sem token → 401
  3. CPF válido sem `X-Gateway-Secret` → 403
  4. CPF inválido → 400
  5. CPF não encontrado no SOAP → 404
  6. SOAP indisponível (WireMock timeout) → 502 ou 504
  7. Endpoint `/api/v1/cpf/{cpf}/validar` sem token → 200 com `valido: true`
  8. Endpoint `/api/v1/cpf/{cpf}/validar` com CPF inválido → 200 com `valido: false`

### biometria-gateway — E2E

Classe: `br.com.vivo.poc.gateway.GatewayE2ETest`

- Inicia o gateway na porta aleatória
- Simula `biometria-core-api` downstream via WireMock ou MockServer
- Usa token JWT real assinado com chave de teste
- Cenários:
  1. Token válido com escopo → 200 proxied
  2. Sem token → 401
  3. Token sem escopo `biometria:read` → 403
  4. `X-Correlation-Id` ausente → gerado automaticamente e presente na resposta
  5. `X-Correlation-Id` presente → propagado e retornado igual
  6. Downstream indisponível → 502

---

## 24. Cobertura com JaCoCo

### Configuração no POM raiz

Adicionar `jacoco-maven-plugin` no `pluginManagement` do POM raiz:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>check</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>${jacoco.coverage.minimum}</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Mínimos de cobertura por módulo

| Módulo | `jacoco.coverage.minimum` | Meta real |
|---|---|---|
| `biometria-core-api` | `0.80` | 100% dos cenários unitários identificados |
| `biometria-legacy-soap` | `0.70` | Melhor esforço |
| `biometria-gateway` | `0.70` | Melhor esforço |

Cada módulo declara a propriedade `jacoco.coverage.minimum` no seu próprio `pom.xml`.

### Exclusões de cobertura (não contar)

Excluir das métricas JaCoCo:
- Classes `*Application.java` (main)
- Classes `*Config.java` (configuração)
- Classes `*Seed*.java` (carga de dados)
- Pacote `*.dto.*` (records/DTOs sem lógica)
- Pacote `*.domain.*` somente se forem entidades JPA puras sem lógica

Configurar via `<excludes>` no plugin.

### Relatório

Após `mvn verify`, o relatório HTML fica em:
`<modulo>/target/site/jacoco/index.html`

O agente deve abrir ou logar o sumário de cobertura antes de commitar.

---

## 25. Protocolo de Validação pré-commit (obrigatório)

O agente **não deve commitar** sem executar todos os passos abaixo em ordem:

```bash
# 1. Compilar o módulo
mvn clean compile -pl <modulo>

# 2. Rodar testes unitários
mvn test -pl <modulo>

# 3. Rodar testes de integração e E2E + relatório JaCoCo
mvn verify -pl <modulo>

# 4. Checar sumário de cobertura JaCoCo (logar % de linhas cobertas)
# Arquivo: <modulo>/target/site/jacoco/index.html
# Ou via: cat <modulo>/target/site/jacoco/jacoco.xml | grep -E 'counter type="LINE"'

# 5. Executar coleção Bruno (ou curl equivalente) para cada cenário
# bru run bruno/<modulo>/ --env local
# Se bru CLI não disponível: executar curls equivalentes e verificar status codes

# 6. Só então commitar
```

Se qualquer etapa falhar: corrigir antes de avançar. Nunca commitar com teste falhando ou cobertura abaixo do mínimo.

---

## 26. O que Este Documento NÃO Cobre

- Detalhes de infraestrutura além do Docker Compose local
- Oracle real (fora do escopo da POC)
- LGPD em produção (ver DAS seção 12)
- Estratégia de migração (ver DAS seção 16)
- Diagramas C4 (ver `docs/diagrams/`)
