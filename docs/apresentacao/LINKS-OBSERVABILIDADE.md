# Links de Observabilidade — POC Vivo Biometria

> Stack ativa com `docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d`

---

## Swagger / OpenAPI

| Link | Descrição |
|------|-----------|
| [Swagger UI — via Gateway](http://localhost:8080/swagger-ui/index.html) | Interface interativa — clique **Authorize**, use Client Credentials e teste os endpoints |
| [OpenAPI Spec (JSON)](http://localhost:8080/api-docs) | Especificação OpenAPI 3.0 em JSON — importe no Postman ou Insomnia |
| [Swagger UI — direto (dev)](http://localhost:8082/swagger-ui/index.html) | Acesso direto ao Core API — sem JWT do Gateway (apenas dev local) |

**Como autenticar no Swagger UI:**
```bash
# 1. Abra http://localhost:8080/swagger-ui/index.html
# 2. Clique no botão "Authorize" (cadeado)
# 3. Em "oauth2 (OAuth2, clientCredentials)", clique "Authorize"
#    client_id: parceiro-externo  |  client_secret: parceiro-secret
# 4. Marque "biometria:read" e confirme
# OU cole o token manualmente em "bearerAuth":
TOKEN=$(curl -s -X POST http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=parceiro-externo&client_secret=parceiro-secret" \
  | jq -r .access_token)
echo $TOKEN
```

---

## Grafana

| Link | Descrição |
|------|-----------|
| [Dashboard Principal — Vivo Biometria](http://localhost:3000/d/vivo-biometria-poc) | Visão geral: consultas, latência, logs, JVM, segurança |
| [Painel: RPS por Resultado](http://localhost:3000/d/vivo-biometria-poc?viewPanel=10) | Taxa de requisições: sucesso / erro / não-encontrado |
| [Painel: Latência SOAP p50/p95/p99](http://localhost:3000/d/vivo-biometria-poc?viewPanel=11) | Histograma de latência do legado — threshold crítico 500ms |
| [Painel: JVM Heap — Core API](http://localhost:3000/d/vivo-biometria-poc?viewPanel=20) | Uso de memória JVM do biometria-core-api |
| [Painel: Status dos Serviços](http://localhost:3000/d/vivo-biometria-poc?viewPanel=4) | UP/DOWN: Core API, Legacy SOAP, Gateway |
| [Explore: Logs Loki](http://localhost:3000/explore?orgId=1&left=%7B"datasource":"loki"%7D) | Busca livre nos logs JSON estruturados |
| [Alertas ativos](http://localhost:3000/alerting/list) | Lista de alertas Grafana disparados |
| [Login Grafana](http://localhost:3000) | admin / admin (trocar em produção) |

---

## Prometheus

| Link | Descrição |
|------|-----------|
| [Prometheus UI](http://localhost:9090) | Consultas PromQL, targets, regras |
| [Targets — status de scrape](http://localhost:9090/targets) | Status de coleta: biometria-gateway, core-api, legacy-soap |
| [Regras de alertas](http://localhost:9090/rules) | Alertas definidos em `docker/prometheus/alerts.yml` |
| [Alertas ativos](http://localhost:9090/alerts) | Alertas em estado FIRING ou PENDING |
| [Métrica: consultas totais](http://localhost:9090/graph?g0.expr=biometria_consultas_total&g0.tab=0) | `biometria_consultas_total` por resultado |
| [Métrica: latência SOAP](http://localhost:9090/graph?g0.expr=histogram_quantile(0.95%2Csum(rate(biometria_legado_latencia_seconds_bucket%5B5m%5D))by(le))&g0.tab=0) | p95 latência legado SOAP |
| [Métrica: status serviços](http://localhost:9090/graph?g0.expr=up&g0.tab=0) | `up{job=~"biometria-.*"}` — 1=UP, 0=DOWN |

---

## Zipkin (Distributed Tracing)

| Link | Descrição |
|------|-----------|
| [Zipkin UI](http://localhost:9411) | Busca de traces distribuídos por serviço / traceId |
| [Traces: biometria-gateway](http://localhost:9411/zipkin/?serviceName=biometria-gateway) | Traces com origem no Gateway |
| [Traces: biometria-core-api](http://localhost:9411/zipkin/?serviceName=biometria-core-api) | Traces do Core API (inclui chamadas SOAP) |
| [Traces: biometria-legacy-soap](http://localhost:9411/zipkin/?serviceName=biometria-legacy-soap) | Traces do Legacy SOAP |
| [Dependências entre serviços](http://localhost:9411/zipkin/dependency/) | Grafo de dependências derivado dos traces |

---

## Endpoints de Saúde dos Serviços (via Gateway)

| Link | Descrição |
|------|-----------|
| [Health — Core API](http://localhost:8080/management/core-api/health) | Spring Actuator health com LegadoSoapHealthIndicator |
| [Health — Legacy SOAP](http://localhost:8080/management/legacy-soap/health) | Spring Actuator health do SOAP |
| [Health — Gateway](http://localhost:8080/actuator/health) | Health do próprio Gateway |
| [Prometheus — Core API](http://localhost:8080/management/core-api/prometheus) | Métricas raw do Core API via Gateway |
| [Prometheus — Legacy SOAP](http://localhost:8080/management/legacy-soap/prometheus) | Métricas raw do Legacy SOAP via Gateway |
| [Prometheus — Gateway](http://localhost:8080/actuator/prometheus) | Métricas raw do Gateway |

---

## H2 Console — Banco de Dados In-Memory (Legacy SOAP)

| Link | Descrição |
|------|-----------|
| [H2 Console](http://localhost:8083/h2-console) | Interface web do banco H2 — visualize os dados seed de biometria |

**Credenciais de acesso:**
```
JDBC URL : jdbc:h2:mem:biometriadb
User Name: sa
Password : (deixe vazio)
```

---

## Loki

| Link | Descrição |
|------|-----------|
| [Loki ready](http://localhost:3100/ready) | Status do Loki |
| [Labels disponíveis](http://localhost:3100/loki/api/v1/labels) | Labels indexados (app, level, traceId, etc.) |

---

## Keycloak (Identity Provider)

| Link | Descrição |
|------|-----------|
| [Admin Console](http://localhost:8081/admin/master/console/) | Admin do Keycloak — user: admin / admin |
| [Realm vivo-poc](http://localhost:8081/realms/vivo-poc) | Informações do realm |
| [JWKS — chaves públicas JWT](http://localhost:8081/realms/vivo-poc/protocol/openid-connect/certs) | Chaves RS256 usadas pelo Gateway para validar JWTs |
| [Token endpoint](http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token) | POST client_credentials → JWT |

---

## Comandos rápidos

```bash
# Subir stack completa (apps + observabilidade)
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d

# Obter token JWT
TOKEN=$(curl -s -X POST http://localhost:8081/realms/vivo-poc/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=parceiro-externo&client_secret=parceiro-secret" \
  | jq -r .access_token)

# Consultar biometria (CPF seed válido)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/biometria/12345678909 | jq

# Verificar métricas biometria
curl -s http://localhost:8080/actuator/prometheus | grep biometria_

# Ver logs em tempo real (Core API)
docker logs -f biometria-core-api --tail 50

# Smoke test completo
bash scripts/test-api.sh
```

---

*Gerado em: 30/06/2026 · POC Vivo — Integração Arquitetural de Biometria Facial*
