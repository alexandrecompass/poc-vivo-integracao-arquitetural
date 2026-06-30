# Estratégia de Observabilidade — POC Vivo Biometria

> Documento de referência sobre como métricas, logs e tracing são coletados, correlacionados
> e visualizados nesta POC. Complementa as seções 15–17 e 27 do `AGENTS.md`.

---

## 1. Os três pilares

```
┌──────────────┐    ┌───────────────┐    ┌──────────────┐
│   MÉTRICAS   │    │     LOGS      │    │   TRACING    │
│              │    │               │    │              │
│  Micrometer  │    │  Logback JSON │    │ Brave / B3   │
│  Prometheus  │    │  Loki         │    │ Zipkin       │
│  Grafana     │    │  Grafana      │    │ Grafana      │
└──────────────┘    └───────────────┘    └──────────────┘
         ↑                  ↑                    ↑
         └──────────────────┴────────────────────┘
                         GRAFANA
                  (interface única de observabilidade)
```

Os três pilares convertem no **Grafana** como ponto único de análise.  
A correlação entre eles é feita pelo campo `traceId` presente em **logs e spans**.

---

## 2. Métricas

### Como funciona

Cada módulo Spring Boot expõe `/actuator/prometheus` via `micrometer-registry-prometheus`.  
O **Prometheus** scrapa este endpoint a cada 15 segundos dentro da rede Docker.  
O **Grafana** lê o Prometheus como datasource e exibe os dados em dashboards.

### Métricas automáticas (sem código adicional)

| Métrica | Descrição |
|---|---|
| `http.server.requests` | Latência, status e método de toda requisição HTTP |
| `jvm.memory.used` | Uso de memória JVM por área |
| `jvm.gc.pause` | Pausas de GC |
| `process.cpu.usage` | CPU do processo |
| `hikaricp.connections.*` | Pool de conexões JDBC (legacy-soap) |

### Métricas customizadas de negócio

**biometria-core-api** (`BiometriaMetrics`):

| Métrica | Tipo | Descrição |
|---|---|---|
| `biometria.consultas.total` | Counter | Todas as consultas recebidas |
| `biometria.consultas.sucesso` | Counter | Consultas com CPF encontrado |
| `biometria.consultas.nao_encontrada` | Counter | CPF não encontrado no legado |
| `biometria.legado.falhas` | Counter | Falhas técnicas ao chamar SOAP |
| `biometria.legado.latencia` | Timer | Latência da chamada SOAP (p50/p95/p99) |
| `biometria.api.latencia` | Timer | Latência total do endpoint REST |

**biometria-legacy-soap** (`SoapMetrics`):

| Métrica | Tipo | Descrição |
|---|---|---|
| `soap.requests.total` | Counter | Total de requisições SOAP recebidas |
| `soap.requests.blocked` | Counter | Bloqueadas pelo InternalSecretInterceptor |
| `soap.requests.success` | Counter | Respondidas com sucesso |
| `soap.requests.not_found` | Counter | CPF não encontrado no H2 |
| `soap.db.latencia` | Timer | Latência da consulta ao H2 |

**biometria-gateway** (`GatewayMetricsFilter`):

| Métrica | Tipo | Descrição |
|---|---|---|
| `gateway.requests.total` | Counter | Todas as requisições recebidas |
| `gateway.requests.authorized` | Counter | Com JWT e escopo válidos |
| `gateway.requests.denied.notoken` | Counter | Sem token |
| `gateway.requests.denied.scope` | Counter | Token sem escopo `biometria:read` |
| `gateway.routing.latencia` | Timer | Tempo de roteamento downstream |

### Acesso ao Prometheus

| Via | URL |
|---|---|
| Direto | `http://localhost:9090` |
| Gateway | `http://localhost:8080/monitoring/prometheus` |
| Scrape core-api | `http://localhost:8080/management/core-api/prometheus` |
| Scrape legacy-soap | `http://localhost:8080/management/legacy-soap/prometheus` |

---

## 3. Logs

### Como funciona

Cada módulo usa `logstash-logback-encoder` para gerar logs em **JSON estruturado no stdout**.  
O **Promtail** lê os logs dos containers Docker e os envia para o **Loki**.  
O **Grafana** lê o Loki como datasource e permite filtrar, buscar e correlacionar logs.

### Estrutura do log JSON

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

Campos obrigatórios em todos os logs:

| Campo | Origem | Descrição |
|---|---|---|
| `timestamp` | Logback | Timestamp ISO-8601 UTC |
| `level` | Logback | INFO / WARN / ERROR |
| `service` | `spring.application.name` | Nome do módulo |
| `correlationId` | MDC via `CorrelationIdFilter` | ID de rastreamento externo |
| `traceId` | MDC via `micrometer-tracing-bridge-brave` | ID do trace B3 |
| `spanId` | MDC via `micrometer-tracing-bridge-brave` | ID do span atual |
| `event` | Código | Nome semântico do evento (ver tabela abaixo) |

### Eventos de log por camada

| Módulo | Evento | Nível |
|---|---|---|
| gateway | `gateway_request_received` | INFO |
| gateway | `gateway_request_routed` | INFO |
| gateway | `gateway_response_sent` | INFO |
| gateway | `gateway_token_invalid` | WARN |
| gateway | `gateway_scope_denied` | WARN |
| gateway | `gateway_secret_invalid` | WARN |
| core-api | `biometria_request_received` | INFO |
| core-api | `soap_call_started` | INFO |
| core-api | `soap_call_finished` | INFO |
| core-api | `biometria_not_found` | WARN |
| core-api | `legado_timeout` | ERROR |
| core-api | `legado_unavailable` | ERROR |
| legacy-soap | `soap_request_received` | INFO |
| legacy-soap | `soap_secret_invalid` | WARN |
| legacy-soap | `db_query_executed` | INFO |
| legacy-soap | `biometria_found` | INFO |
| legacy-soap | `biometria_not_found_db` | WARN |

### Regras de sanitização (obrigatórias)

- **NUNCA** logar CPF completo — usar `CpfMasker.mask(cpf)` → `"*********25"`
- **NUNCA** logar `imagemBase64` (dado sensível LGPD)
- **NUNCA** logar `Authorization: Bearer ...` ou qualquer JWT
- **NUNCA** logar o envelope SOAP completo
- **NUNCA** logar `X-Gateway-Secret` ou `X-Core-Secret`

### Acesso aos logs

| Via | Descrição |
|---|---|
| `docker logs <container> -f` | Stdout local |
| `http://localhost:3000` → Grafana → Explore → Loki | Interface web com filtros |
| `http://localhost:8080/monitoring/grafana` | Via Gateway |

---

## 4. Tracing

### Como funciona

1. `micrometer-tracing-bridge-brave` intercepta toda requisição e cria um **trace** com spans
2. O `traceId` e `spanId` são automaticamente injetados no MDC do Logback — todos os logs
   da requisição já contêm esses campos sem nenhum código adicional
3. `zipkin-reporter-brave` coleta os spans completos (com duração, tags e metadados) e os
   envia ao **Zipkin** em `http://zipkin:9411/api/v2/spans`
4. O **Grafana** acessa o Zipkin como datasource e exibe a cadeia de spans de cada requisição

### Fluxo de propagação do traceId

```
Parceiro
  → Gateway (gera traceId B3, injeta no header downstream)
    → biometria-core-api (recebe traceId, cria span filho)
      → biometria-legacy-soap (recebe traceId via header SOAP, cria span filho)
```

Cada camada cria um **span filho** do span pai, formando a árvore de spans de uma requisição.

### Correlação logs ↔ traces

O Grafana é configurado com `derivedFields` no datasource Loki: ao visualizar um log com
`traceId`, aparece um link direto para o trace no Zipkin. Isso permite navegar de um erro
no log para o trace completo da requisição — **sem copiar e colar IDs manualmente**.

### Configuração por módulo (`application.yml`)

```yaml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0   # 100% na POC; reduzir para 0.1 em produção
```

Para desenvolvimento local (sem Docker Compose rodando):

```yaml
spring:
  profiles:
    active: local

# application-local.yml
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans  # silencioso se Zipkin offline
```

### Acesso ao Zipkin

| Via | URL |
|---|---|
| Direto | `http://localhost:9411` |
| Gateway | `http://localhost:8080/monitoring/zipkin` |

---

## 5. Gateway como ponto único de acesso

O `biometria-gateway` (porta 8080) expõe todas as ferramentas de observabilidade via proxy,
eliminando a necessidade de conhecer as portas internas de cada serviço.

### Mapa de rotas de observabilidade

```
GET /actuator/health                       → gateway próprio
GET /actuator/prometheus                   → gateway próprio

GET /management/core-api/health            → biometria-core-api:8082/actuator/health
GET /management/core-api/prometheus        → biometria-core-api:8082/actuator/prometheus
GET /management/core-api/metrics           → biometria-core-api:8082/actuator/metrics

GET /management/legacy-soap/health         → biometria-legacy-soap:8083/actuator/health
GET /management/legacy-soap/prometheus     → biometria-legacy-soap:8083/actuator/prometheus
GET /management/legacy-soap/metrics        → biometria-legacy-soap:8083/actuator/metrics

GET /monitoring/prometheus/**              → prometheus:9090/...
GET /monitoring/grafana/**                 → grafana:3000/...
GET /monitoring/zipkin/**                  → zipkin:9411/...
```

**Segurança das rotas de management:** rotas `/management/**` e `/monitoring/**` são abertas
sem JWT na POC (ferramentas de monitoramento não têm client OAuth2). Em produção, proteger com
IP allowlist da rede interna ou scope OAuth2 de operador (`biometria:ops`).

---

## 6. Infraestrutura Docker necessária

```
docker/
├── docker-compose.yml            ← inclui prometheus, grafana, loki, promtail, zipkin
├── prometheus/
│   └── prometheus.yml            ← scrape_configs para os 3 módulos
├── promtail/
│   └── promtail-config.yml       ← coleta logs Docker por label de projeto
└── grafana/
    └── provisioning/
        └── datasources/
            └── datasources.yml   ← Prometheus + Loki (com derivedFields) + Zipkin
```

Ver seção 27 do `AGENTS.md` para o conteúdo completo de cada arquivo.

---

## 7. Verificação pós-deploy

Após `docker compose up`, verificar em ordem:

```bash
# 1. Health de todos os módulos via gateway
curl http://localhost:8080/actuator/health
curl http://localhost:8080/management/core-api/health
curl http://localhost:8080/management/legacy-soap/health

# 2. Prometheus scraping com sucesso (deve listar os 3 jobs como UP)
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[].health'

# 3. Métricas customizadas disponíveis
curl http://localhost:8080/management/core-api/prometheus | grep biometria_

# 4. Disparar uma requisição real e verificar o traceId no Zipkin
TOKEN=$(curl -s -X POST http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=parceiro-externo&client_secret=parceiro-externo-secret&scope=biometria:read" \
  | jq -r '.access_token')

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/biometria/52998224725 -v 2>&1 | grep "X-Correlation-Id"

# 5. Acessar Grafana e verificar datasources (Prometheus, Loki, Zipkin todos verdes)
open http://localhost:3000   # admin / admin
```

---

## 8. Evolução futura (pós-POC)

| Item | Descrição |
|---|---|
| **OpenTelemetry** | Substituir `micrometer-tracing-bridge-brave` por `bridge-otel` + exportador OTLP para Tempo |
| **Alertas Prometheus** | Criar `alertmanager.yml` com alertas para `biometria.legado.falhas > 10` e latência p95 > 500ms |
| **Dashboard Grafana versionado** | Exportar JSON do dashboard para `docker/grafana/dashboards/` e provisionar automaticamente |
| **Retenção de logs** | Configurar `retention_period` no Loki para compliance LGPD (dado sensível = mínimo necessário) |
| **Sampling de traces** | Reduzir `probability` de 1.0 para 0.1 em produção (1 em 10 requisições tracadas) |
