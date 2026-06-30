# ADR-005 — Observabilidade, Logs, Métricas e Tracing

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Rastreabilidade ponta a ponta, diagnóstico operacional e governança técnica de integrações

---

## 1. Contexto

A POC Vivo — Integração Arquitetural tem como objetivo expor dados de biometria facial de clientes por meio de uma API REST moderna, protegida por API Gateway e integrada a um legado SOAP.

O fluxo arquitetural envolve múltiplos componentes:

```text
Parceiro Externo
  → API Gateway
  → Biometria Core API
  → SOAP Adapter
  → Legacy SOAP Service
  → H2 simulando Oracle
```

Em ambientes corporativos, especialmente em integrações com legados e parceiros externos, não basta que a aplicação funcione. É necessário conseguir responder rapidamente perguntas operacionais como:

- Quem chamou a API?
- Quando chamou?
- Qual CPF foi consultado, sem expor o dado completo?
- Qual foi o correlation ID?
- A chamada chegou ao gateway?
- A chamada chegou ao core?
- A chamada chegou ao SOAP?
- Quanto tempo o legado demorou para responder?
- O erro ocorreu no gateway, no core ou no legado?
- Foi erro de autenticação, autorização, negócio, integração ou infraestrutura?
- Qual parceiro está gerando mais erro?
- Qual rota está mais lenta?
- O legado está degradado?

Como o dado tratado é biometria facial, a solução também precisa observar sem vazar informações sensíveis.

Esta ADR define a estratégia de observabilidade da POC.

---

## 2. Problema

Sem uma estratégia clara de observabilidade, a solução apresentaria riscos importantes:

- Dificuldade de diagnóstico em caso de falha.
- Falta de rastreabilidade ponta a ponta.
- Logs inconsistentes entre gateway, core e legado.
- Ausência de correlation ID.
- Dificuldade para identificar lentidão no SOAP.
- Falta de métricas de sucesso, erro e latência.
- Risco de logs contendo CPF completo.
- Risco de logs contendo imagem facial ou Base64.
- Dificuldade de auditoria técnica por parceiro.
- Dificuldade de demonstrar maturidade arquitetural no case.

Além disso, integrações com legados costumam ter falhas intermitentes, timeouts, respostas inválidas e comportamento instável. Sem logs e métricas adequadas, a operação fica reativa e pouco confiável.

---

## 3. Decisão

A solução adotará uma estratégia de observabilidade baseada em:

- **Logs estruturados**.
- **Correlation ID** ponta a ponta.
- **Métricas técnicas**.
- **Tracing distribuído preparado para evolução**.
- **Health checks**.
- **Logging seguro sem dados sensíveis**.
- **Padronização de eventos relevantes de integração**.

Na POC, a implementação mínima deverá contemplar:

- Logs estruturados no API Gateway.
- Logs estruturados na Biometria Core API.
- Logs estruturados no Legacy SOAP Service.
- Header `X-Correlation-Id`.
- Propagação do correlation ID entre serviços.
- Métricas via Spring Boot Actuator.
- Health checks via Actuator.
- Registro do tempo da chamada SOAP.
- Tratamento de erro com logs técnicos.
- Mascaramento de CPF.
- Proibição de log de imagem facial/Base64.

Como evolução, a arquitetura estará preparada para OpenTelemetry, Prometheus, Grafana, Loki/ELK e Jaeger/Tempo.

---

## 4. Justificativa

A observabilidade é essencial em arquiteturas distribuídas.

Neste case, a chamada atravessa várias camadas e protocolos diferentes. O consumidor externo usa REST, o gateway valida segurança, o core executa o caso de uso e o adapter chama um legado SOAP.

Sem correlação de logs, cada componente pareceria um sistema isolado.

O uso de `X-Correlation-Id` permite rastrear uma mesma requisição em todos os pontos.

Logs estruturados facilitam indexação e consulta em ferramentas como:

- ELK.
- OpenSearch.
- Loki.
- Datadog.
- Splunk.
- CloudWatch.
- Grafana.

Métricas ajudam a entender comportamento agregado:

- Quantidade de chamadas.
- Taxa de erro.
- Latência média.
- P95/P99.
- Falhas por componente.
- Tempo do legado SOAP.

Tracing distribuído, ainda que não seja completamente implementado na POC, deve estar previsto como evolução natural.

---

## 5. Conceitos Aplicados

### 5.1 Logs estruturados

Logs estruturados são logs em formato chave-valor ou JSON, permitindo busca e agregação.

Exemplo:

```json
{
  "timestamp": "2026-06-30T12:00:00Z",
  "level": "INFO",
  "service": "biometria-core-api",
  "event": "soap_call_finished",
  "correlationId": "b2c9c8b1-91f5-44e8-9f8e-3dfc79c7e001",
  "cpfMasked": "***.***.***-09",
  "durationMs": 183,
  "status": "SUCCESS"
}
```

---

### 5.2 Correlation ID

Correlation ID é um identificador único associado a uma requisição.

Ele deve acompanhar a chamada desde a entrada no gateway até o retorno final.

Header adotado:

```http
X-Correlation-Id
```

Regra:

- Se o consumidor enviar o header, o gateway propaga.
- Se não enviar, o gateway gera.
- Todos os serviços internos devem registrar o mesmo valor.

---

### 5.3 Métricas

Métricas representam comportamento agregado da solução.

Exemplos:

- Total de requisições.
- Total de erros.
- Latência do endpoint.
- Latência da chamada SOAP.
- Quantidade de respostas 2xx, 4xx e 5xx.
- Quantidade de timeouts.
- Quantidade de biometria não encontrada.
- Quantidade de falhas de autenticação.
- Quantidade de falhas de autorização.

---

### 5.4 Tracing distribuído

Tracing permite visualizar uma requisição atravessando múltiplos serviços.

Exemplo de spans:

```text
gateway.request
  → core.consultarBiometria
    → soap.consultarBiometriaPorCpf
      → legacy.database.query
```

Na POC, o tracing pode ser documentado e preparado, mesmo que a implementação completa fique como evolução.

---

### 5.5 Health checks

Health checks permitem verificar disponibilidade técnica dos serviços.

Endpoints esperados:

```http
/actuator/health
/actuator/info
/actuator/metrics
```

Cada serviço Spring Boot deve expor health check.

---

## 6. Alternativas Consideradas

### 6.1 Logs simples em texto livre

Essa alternativa foi descartada como padrão principal.

Logs em texto livre são fáceis de escrever, mas difíceis de consultar, filtrar e correlacionar.

Exemplo ruim:

```text
Chamando legado para cpf 12345678909
```

Problemas:

- CPF completo exposto.
- Sem correlation ID.
- Sem evento padronizado.
- Difícil indexação.
- Difícil busca por campos.
- Dificuldade para dashboards.

---

### 6.2 Logar payloads completos para facilitar debug

Essa alternativa foi descartada com força.

Como o domínio envolve biometria facial, logar payloads completos poderia expor dados sensíveis.

Não devem ser logados:

- Imagem facial.
- Base64 da imagem.
- CPF completo.
- Token JWT completo.
- Client secret.
- XML SOAP completo.
- Headers sensíveis.
- Stack traces expostos ao consumidor.

Debug não justifica vazamento de dado sensível.

---

### 6.3 Implementar tracing completo antes da POC funcional

Essa alternativa foi considerada, mas não será obrigatória no escopo mínimo.

Tracing completo com OpenTelemetry, collector, Jaeger/Tempo e dashboards pode consumir tempo significativo.

Para a POC, a decisão é:

- Implementar logs estruturados e correlation ID.
- Expor métricas e health checks.
- Documentar tracing como evolução arquitetural.
- Preparar nomes e pontos de instrumentação.

---

### 6.4 Observabilidade apenas no Core

Essa alternativa foi descartada.

Como a arquitetura inclui gateway e legado SOAP, observar apenas o core não permite diagnóstico ponta a ponta.

O gateway deve registrar entrada e decisão de segurança.

O core deve registrar regra e integração.

O legado fake deve registrar recebimento e consulta simulada.

---

## 7. Consequências Positivas

A decisão traz os seguintes benefícios:

- Melhor diagnóstico de falhas.
- Rastreabilidade ponta a ponta.
- Maior maturidade operacional.
- Melhor governança técnica.
- Facilidade para demonstrar arquitetura em entrevista.
- Menor tempo para identificar problemas.
- Proteção contra logs sensíveis.
- Melhor visibilidade da latência do legado.
- Base para dashboards futuros.
- Base para auditoria técnica por parceiro.
- Preparação para OpenTelemetry.
- Melhor aderência a padrões corporativos.

---

## 8. Consequências Negativas

A decisão adiciona responsabilidades:

- Mais código de filtros/interceptors.
- Necessidade de padronizar eventos.
- Necessidade de cuidado com dados sensíveis.
- Necessidade de testar logs indiretamente.
- Possível aumento de volume de logs.
- Necessidade de configurar Actuator.
- Necessidade de evitar logs excessivos.

Esses custos são aceitáveis, pois observabilidade é requisito explícito do case.

---

## 9. Trade-offs

A arquitetura escolhe observabilidade operacional em troca de complexidade moderada.

A POC não precisa implementar um stack completo de observabilidade com Grafana, Prometheus e tracing distribuído para ser bem avaliada. Porém, precisa demonstrar que a arquitetura foi pensada para operação real.

Portanto, o equilíbrio adotado é:

- Implementar logs estruturados.
- Implementar correlation ID.
- Implementar Actuator.
- Registrar tempos críticos.
- Documentar evolução para tracing e métricas avançadas.

---

## 10. Eventos de Log Padronizados

A solução deverá adotar nomes de eventos claros e consistentes.

### 10.1 Gateway

Eventos sugeridos:

```text
gateway_request_received
gateway_token_validated
gateway_token_invalid
gateway_scope_denied
gateway_route_forwarded
gateway_response_returned
gateway_route_error
```

### 10.2 Biometria Core API

Eventos sugeridos:

```text
biometria_request_received
biometria_cpf_validated
biometria_use_case_started
biometria_soap_call_started
biometria_soap_call_finished
biometria_not_found
biometria_response_returned
biometria_error
```

### 10.3 Legacy SOAP Service

Eventos sugeridos:

```text
legacy_soap_request_received
legacy_soap_database_query_started
legacy_soap_database_query_finished
legacy_soap_response_returned
legacy_soap_fault_returned
```

---

## 11. Campos Obrigatórios nos Logs

Campos mínimos recomendados:

```text
timestamp
level
service
event
correlationId
durationMs
status
httpMethod
path
httpStatus
clientId
cpfMasked
errorType
```

Nem todos os campos aparecem em todos os logs.

Exemplo de log no gateway:

```json
{
  "service": "biometria-gateway",
  "event": "gateway_route_forwarded",
  "correlationId": "demo-001",
  "clientId": "partner-biometria-client",
  "httpMethod": "GET",
  "path": "/api/v1/biometria/**",
  "route": "biometria-core-api",
  "status": "FORWARDED"
}
```

Exemplo de log no core:

```json
{
  "service": "biometria-core-api",
  "event": "biometria_soap_call_finished",
  "correlationId": "demo-001",
  "cpfMasked": "***.***.***-09",
  "durationMs": 142,
  "status": "SUCCESS"
}
```

---

## 12. Política de Dados Sensíveis em Logs

É proibido registrar:

- Imagem facial.
- Base64 da imagem.
- CPF completo.
- Token JWT completo.
- Client secret.
- Authorization header completo.
- XML SOAP completo.
- SOAP Envelope completo com dados pessoais.
- Stack trace em resposta HTTP.
- Dados internos do banco.
- Senhas ou secrets.

É permitido registrar:

- CPF mascarado.
- Hash do CPF, se implementado.
- Client ID.
- Correlation ID.
- Status técnico.
- Tempo de resposta.
- Tipo de erro.
- Serviço chamado.
- Escopo, sem token completo.

Exemplo de CPF mascarado:

```text
***.***.***-09
```

---

## 13. Estratégia de Mascaramento de CPF

Para a POC, o mascaramento poderá preservar apenas os dois últimos dígitos.

Entrada:

```text
12345678909
```

Log:

```text
***.***.***-09
```

Em ambiente produtivo, recomenda-se avaliar hashing com salt ou tokenização para rastreabilidade sem exposição direta.

---

## 14. Propagação do Correlation ID

### 14.1 Gateway

O gateway deverá:

- Ler `X-Correlation-Id`.
- Gerar UUID se ausente.
- Registrar nos logs.
- Propagar para o core.

### 14.2 Core

O core deverá:

- Ler `X-Correlation-Id`.
- Colocar o valor no MDC/log context.
- Propagar para o adapter SOAP.
- Registrar nos logs.

### 14.3 Legacy SOAP

O legado fake deverá:

- Receber correlation ID por header HTTP ou metadado simulado.
- Registrar nos logs.
- Retornar normalmente.

Em um legado real, talvez não seja possível propagar esse header. Nesse caso, o core ainda deve registrar a chamada externa com correlation ID.

---

## 15. Métricas Recomendadas

### 15.1 Gateway

- `gateway.requests.total`
- `gateway.requests.duration`
- `gateway.auth.failures.total`
- `gateway.authorization.failures.total`
- `gateway.routes.errors.total`

### 15.2 Core

- `biometria.requests.total`
- `biometria.requests.duration`
- `biometria.success.total`
- `biometria.not_found.total`
- `biometria.errors.total`
- `biometria.soap.duration`
- `biometria.soap.errors.total`
- `biometria.soap.timeout.total`

### 15.3 Legacy SOAP

- `legacy.soap.requests.total`
- `legacy.soap.duration`
- `legacy.soap.errors.total`
- `legacy.database.query.duration`

Na POC, parte dessas métricas pode ser coberta por Actuator/Micrometer automaticamente, com métricas customizadas documentadas como evolução.

---

## 16. Health Checks

Cada aplicação Spring Boot deverá expor health check.

### 16.1 Gateway

```http
GET /actuator/health
```

Deve indicar se o gateway está no ar.

### 16.2 Core

```http
GET /actuator/health
```

Deve indicar se o core está no ar.

Pode futuramente incluir health check do legado SOAP.

### 16.3 Legacy SOAP

```http
GET /actuator/health
```

Deve indicar se o serviço SOAP fake e H2 estão disponíveis.

---

## 17. Impacto no Tratamento de Erros

Toda falha relevante deve gerar log estruturado.

Exemplos:

| Cenário | Serviço | Evento |
|---|---|---|
| Token ausente | Gateway | gateway_token_invalid |
| Escopo insuficiente | Gateway | gateway_scope_denied |
| CPF inválido | Core | biometria_error |
| Biometria não encontrada | Core | biometria_not_found |
| SOAP indisponível | Core | biometria_soap_call_failed |
| Falha no H2/Oracle fake | Legacy | legacy_soap_fault_returned |
| Timeout | Core | biometria_soap_timeout |

O log deve conter o correlation ID.

---

## 18. Impacto na Segurança

Observabilidade não pode comprometer segurança.

A solução deve seguir o princípio:

```text
Registrar o suficiente para diagnosticar,
mas nunca registrar o suficiente para vazar dados sensíveis.
```

Isso é especialmente importante porque biometria facial é dado sensível.

O sistema deve evitar logs verbosos em produção.

Em desenvolvimento, logs de debug também não devem exibir payloads sensíveis.

---

## 19. Impacto na Arquitetura C4

### 19.1 C1 — System Context

Observabilidade não precisa aparecer como componente principal no C1, exceto se for representada como sistema externo corporativo de monitoramento.

Opcional:

```text
Solução Vivo → Plataforma de Observabilidade
```

### 19.2 C2 — Container Diagram

No C2, pode aparecer uma plataforma de observabilidade como container externo/opcional.

Exemplo:

```text
Gateway → Logs/Metrics
Core → Logs/Metrics
Legacy SOAP → Logs/Metrics
```

Para manter o C2 limpo, observabilidade pode ser descrita em nota.

### 19.3 C3 — Component Diagram

No C3, devem aparecer componentes como:

- CorrelationIdFilter.
- RequestLoggingFilter.
- GlobalExceptionHandler.
- SoapIntegrationLogger.
- Metrics instrumentation.

---

## 20. Impacto no README

O README deverá explicar:

- Como visualizar logs.
- Como enviar `X-Correlation-Id`.
- Como consultar health checks.
- Quais dados não são logados.
- Quais eventos aparecem no fluxo.
- Como identificar uma chamada ponta a ponta.
- Como testar erro no legado fake.

Exemplo:

```bash
curl -X GET http://localhost:8080/api/v1/biometria/12345678909 \
  -H "Authorization: Bearer <token>" \
  -H "X-Correlation-Id: demo-001"
```

Depois, o usuário deve conseguir buscar `demo-001` nos logs dos serviços.

---

## 21. Regras de Implementação

1. Toda requisição deve possuir correlation ID.
2. O gateway deve gerar correlation ID se ausente.
3. O core deve propagar correlation ID para chamadas ao legado.
4. Logs devem ser estruturados.
5. Logs devem conter nome do serviço.
6. Logs devem conter nome do evento.
7. Logs devem conter status técnico.
8. Logs devem conter duração em pontos críticos.
9. CPF completo não deve ser logado.
10. Imagem/Base64 não deve ser logada.
11. Token JWT completo não deve ser logado.
12. SOAP XML completo não deve ser logado.
13. Erros devem ser registrados com tipo técnico.
14. Health checks devem estar disponíveis.
15. Métricas devem estar preparadas via Actuator/Micrometer.

---

## 22. Estrutura Recomendada de Código

### 22.1 Gateway

```text
br.com.vivo.poc.gateway
  filter
    CorrelationIdGlobalFilter
    RequestLoggingGlobalFilter

  config
    ObservabilityConfig

  error
    GatewayErrorHandler
```

### 22.2 Core

```text
br.com.vivo.poc.biometria
  infrastructure
    logging
      CorrelationIdFilter
      LoggingUtils
      CpfMasker

    metrics
      BiometriaMetrics

    error
      GlobalExceptionHandler
```

### 22.3 Legacy SOAP

```text
br.com.vivo.poc.legacy
  infrastructure
    logging
      CorrelationIdFilter
      LegacyLoggingUtils
```

---

## 23. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- O gateway gerar ou propagar `X-Correlation-Id`.
- O core registrar logs com o mesmo correlation ID.
- O legado fake registrar logs com o mesmo correlation ID, quando possível.
- O CPF não aparecer completo nos logs.
- O Base64 da biometria não aparecer nos logs.
- O token JWT completo não aparecer nos logs.
- O tempo da chamada SOAP for registrado.
- Erros forem registrados com evento padronizado.
- Actuator estiver habilitado nos serviços.
- Health checks estiverem acessíveis.
- O README explicar como rastrear uma chamada.
- O DAS mencionar a estratégia de observabilidade.
- O C3 refletir filtros/logging/exception handler quando aplicável.

---

## 24. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-005-observabilidade-logs-metricas-tracing.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Atualizar README com estratégia de observabilidade.
- [ ] Documentar header `X-Correlation-Id`.
- [ ] Documentar política de logs seguros.
- [ ] Documentar eventos de log padronizados.
- [ ] Documentar métricas esperadas.
- [ ] Documentar health checks.
- [ ] Refletir observabilidade no C3.

### Código — Gateway

- [ ] Criar filtro global de correlation ID.
- [ ] Criar filtro global de logging.
- [ ] Gerar correlation ID quando ausente.
- [ ] Propagar `X-Correlation-Id`.
- [ ] Logar rota, método, status e duração.
- [ ] Não logar JWT completo.
- [ ] Configurar Actuator.
- [ ] Expor `/actuator/health`.

### Código — Core

- [ ] Criar filtro de correlation ID.
- [ ] Configurar MDC/log context.
- [ ] Criar utilitário de mascaramento de CPF.
- [ ] Logar início e fim da consulta.
- [ ] Logar início e fim da chamada SOAP.
- [ ] Logar duração da chamada SOAP.
- [ ] Logar erros com tipo padronizado.
- [ ] Não logar Base64.
- [ ] Não logar CPF completo.
- [ ] Configurar Actuator.
- [ ] Expor `/actuator/health`.

### Código — Legacy SOAP

- [ ] Ler correlation ID recebido.
- [ ] Logar requisição SOAP recebida.
- [ ] Logar consulta ao H2/Oracle fake.
- [ ] Logar resposta técnica.
- [ ] Não logar imagem/Base64.
- [ ] Configurar Actuator.
- [ ] Expor `/actuator/health`.

### Testes

- [ ] Testar geração de correlation ID no gateway.
- [ ] Testar propagação de correlation ID para o core.
- [ ] Testar propagação de correlation ID para o legado fake.
- [ ] Testar mascaramento de CPF.
- [ ] Testar que Base64 não aparece em logs.
- [ ] Testar health check do gateway.
- [ ] Testar health check do core.
- [ ] Testar health check do legado.
- [ ] Testar logs de erro em falha SOAP.
- [ ] Testar logs de biometria não encontrada.

---

## 25. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| CPF completo em logs | Alto | CpfMasker e revisão de logs |
| Base64 da imagem em logs | Alto | Proibir payload logging |
| JWT completo em logs | Alto | Sanitizar headers |
| Ausência de correlation ID | Médio | Filtro obrigatório no gateway |
| Logs inconsistentes | Médio | Eventos padronizados |
| Excesso de logs | Médio | Níveis INFO/WARN/ERROR adequados |
| Falta de métricas do SOAP | Médio | Registrar duração da chamada |
| Actuator exposto indevidamente | Médio | Restringir exposição em produção |

---

## 26. Decisão Final

A solução adotará uma estratégia de observabilidade baseada em logs estruturados, correlation ID, métricas via Actuator/Micrometer, health checks e preparação para tracing distribuído.

O objetivo é garantir rastreabilidade ponta a ponta sem comprometer a segurança dos dados sensíveis.

A observabilidade será aplicada nos três principais pontos da arquitetura:

- API Gateway.
- Biometria Core API.
- Legacy SOAP Service.

Essa decisão reforça a maturidade operacional da solução e atende aos diferenciais avaliados no case técnico.

---

## 27. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Criar ADR-006 sobre estratégia de versionamento de APIs.
2. Atualizar o DAS com observabilidade.
3. Criar diagramas C1, C2 e C3.
4. Criar estrutura inicial do repositório.
5. Criar módulos `biometria-gateway`, `biometria-core-api` e `biometria-legacy-soap`.
6. Implementar primeiro o fluxo mínimo sem segurança completa.
7. Adicionar segurança.
8. Adicionar observabilidade.
9. Consolidar README e OpenAPI.
