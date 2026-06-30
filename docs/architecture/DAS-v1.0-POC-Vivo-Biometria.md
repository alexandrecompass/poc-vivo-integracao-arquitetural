# Documento de Arquitetura da Solução (DAS)

## POC Vivo – Integração Arquitetural — Biometria Facial

| Campo | Valor |
|---|---|
| **Versão** | 1.0 (Consolidado) |
| **Status** | Aprovado |
| **Projeto** | POC Vivo – Integração Arquitetural |
| **Domínio** | Biometria Facial |

---

## Histórico de Versões

| Versão | Data | Descrição |
|---|---|---|
| 0.1 | 2026-06-30 | Estrutura inicial e escopo arquitetural |
| 0.2 | 2026-06-30 | Consolidação das ADRs 001–006 |
| 1.0 | 2026-06-30 | Documento único consolidado com LGPD e estratégia de migração |

---

## 1. Objetivo

Modernizar a exposição das informações de biometria facial armazenadas em um sistema legado Oracle, acessível exclusivamente via SOAP, disponibilizando-as por meio de uma API REST aderente aos padrões corporativos de governança da Vivo: segurança OAuth2/JWT, versionamento, logging estruturado, rastreabilidade e observabilidade.

---

## 2. Problema

O sistema legado de biometria expõe apenas um serviço SOAP interno. Parceiros externos exigem REST. O acesso deve ser autenticado, autorizado, rastreável e versionado. Nenhuma modernização deve alterar o sistema legado.

---

## 3. Escopo

### Incluído

- API REST versionada (`/api/v1/biometria/{cpf}`)
- API Gateway (Spring Cloud Gateway)
- Keycloak (OAuth2 / JWT / Client Credentials)
- Biometria Core API (Spring Boot REST)
- Legacy SOAP Service (Spring Boot SOAP fictício)
- H2 Database simulando Oracle
- OpenAPI / Swagger
- Logs estruturados JSON
- Correlation ID ponta a ponta
- Métricas (Micrometer / Actuator)
- Health Checks
- Diagramas C4 (C1, C2, C3)
- Diagramas de sequência

### Fora do Escopo

- Oracle real
- Biometria real / imagens reais
- Integração Receita Federal
- Kubernetes / Service Mesh
- Circuit Breaker (planejado como evolução)

---

## 4. Premissas

- O legado permanece inalterado durante toda a POC.
- SOAP é o único canal disponível para consulta de biometria.
- OAuth2 será provido pelo Keycloak (POC local).
- A autenticação e autorização ocorrem no API Gateway.
- Dados de biometria são fictícios em toda a POC.

---

## 5. Restrições

- Modernização sem alterar o sistema legado.
- Contrato REST deve ser estável e versionado.
- Dados sensíveis não devem aparecer nos logs.
- Compatibilidade com futuros parceiros e versões de API.

---

## 6. Requisitos Funcionais

| ID | Requisito |
|---|---|
| RF01 | Consultar biometria facial por CPF via API REST |
| RF02 | Converter resposta SOAP para contrato REST padronizado |
| RF03 | Validar autenticação e autorização OAuth2/JWT |
| RF04 | Registrar auditoria técnica com correlation ID |
| RF05 | Retornar erros padronizados com códigos funcionais |

---

## 7. Requisitos Não Funcionais

| ID | Requisito |
|---|---|
| RNF01 | Disponibilidade: tolerância a falha do legado com resposta 502/504 |
| RNF02 | Segurança: autenticação OAuth2, autorização por escopo, TLS |
| RNF03 | Observabilidade: logs, métricas, health checks, tracing preparado |
| RNF04 | Versionamento: URI versioning, `/api/v1` |
| RNF05 | Rastreabilidade: Correlation ID em todas as requisições |
| RNF06 | Proteção de dados: mascaramento de CPF e biometria nos logs |
| RNF07 | Conformidade: aderência à LGPD para dado sensível (biometria facial) |

---

## 8. Arquitetura Proposta

```
Parceiro Externo
    │
    ├─► Keycloak (OAuth2 / Client Credentials → JWT)
    │
    └─► API Gateway
            │  valida JWT, escopo, propaga Correlation ID
            │
            └─► Biometria Core API
                    │  valida CPF, executa use case
                    │
                    └─► SOAP Adapter → Legacy SOAP Service → H2 (Oracle simulado)
```

---

## 9. Decisões Arquiteturais

### ADR-001 — Exposição do Legado SOAP via API REST

Padrões aplicados: Facade, Adapter, Anti-Corruption Layer, Clean Architecture.

O sistema legado nunca é acessado diretamente por parceiros externos. A Biometria Core API atua como fachada REST e o SOAP Adapter como camada de anti-corrupção, impedindo que contratos XML/WSDL vazem para o modelo interno.

---

### ADR-002 — API Gateway como Ponto Único de Entrada

O API Gateway é responsável por: autenticação OAuth2/JWT, verificação de escopo, propagação de Correlation ID, roteamento, logging de borda e preparação futura de rate limit e quotas.

O Gateway não contém regra de negócio.

---

### ADR-003 — Adapter e Anti-Corruption Layer para o Legado

A porta `BiometriaLegadoPort` desacopla o use case da implementação SOAP. O adapter `SoapBiometriaLegadoAdapter` implementa a porta e concentra toda a complexidade de serialização, mapeamento e tradução de falhas SOAP.

O use case não conhece SOAP, XML nem WSDL.

---

### ADR-004 — Segurança: OAuth2 / JWT / Keycloak

Fluxo: Client Credentials. Escopo obrigatório: `biometria:read`. O Keycloak emite JWT assinado. O Gateway valida assinatura e escopo via JWKS. Tokens não são logados em nenhuma camada.

---

### ADR-005 — Observabilidade: Logs, Métricas e Tracing

- Logs: JSON estruturado com nível, timestamp, correlation ID, CPF mascarado
- Correlation ID: gerado no Gateway se ausente, propagado internamente, retornado na resposta
- Métricas: Micrometer + Spring Boot Actuator (consultas, erros, latência SOAP)
- Health Checks: `/actuator/health`
- Tracing: preparado para OpenTelemetry (instrumentação futura)

---

### ADR-006 — Versionamento de APIs por URI

Versão inicial: `GET /api/v1/biometria/{cpf}`. Mudanças incompatíveis geram nova versão de URI (`/api/v2/...`). Consumidores de versões anteriores não são impactados enquanto a versão antiga for mantida ativa.

---

## 10. Componentes Principais

| Componente | Responsabilidade |
|---|---|
| API Gateway | Ponto único de entrada, autenticação, roteamento |
| Biometria Core API | Use case, validação, tradução SOAP→REST |
| Legacy SOAP Service | Simulação do legado (POC) |
| H2 Database | Simulação do Oracle (POC) |
| Keycloak | Emissão e validação de tokens OAuth2/JWT |
| SoapBiometriaLegadoAdapter | Isolamento SOAP, mapeamento e tradução de falhas |
| GlobalExceptionHandler | Padronização de respostas de erro REST |
| CorrelationIdFilter | Rastreabilidade ponta a ponta |

Estrutura de pacotes detalhada no diagrama C3.

---

## 11. Segurança

- TLS obrigatório em ambiente produtivo
- OAuth2 Client Credentials com escopo `biometria:read`
- JWT assinado pelo Keycloak, validado pelo Gateway via JWKS
- CPF mascarado nos logs (`***.***.***-XX`)
- Imagem/Base64 nunca registrada em log
- JWT completo nunca registrado em log
- SOAP envelope nunca registrado em log
- Stack trace não exposto ao consumidor
- Defesa em profundidade: Gateway valida JWT, Core valida CPF, SOAP permanece inacessível externamente

---

## 12. LGPD e Proteção de Dados Sensíveis

Biometria facial é dado pessoal **sensível** conforme o Art. 11 da Lei Geral de Proteção de Dados (Lei nº 13.709/2018). O tratamento desse dado requer base legal adequada (Art. 11, II), finalidade específica, e controles reforçados em todas as camadas.

### 12.1 Classificação do Dado

| Dado | Classificação LGPD | Controle |
|---|---|---|
| CPF | Dado pessoal | Mascaramento em logs |
| Imagem biométrica / Base64 | Dado sensível | Nunca em log, tráfego criptografado |
| Token JWT | Dado de autenticação | Nunca em log |
| Correlation ID | Metadado técnico | Pode ser logado sem restrição |

### 12.2 Princípios Aplicados

**Finalidade:** a biometria só é consultada para a finalidade autorizada pelo parceiro (verificação de identidade). Uso secundário ou ampliação de finalidade requer nova base legal.

**Necessidade / Minimização:** a resposta da API expõe apenas os campos estritamente necessários. CPF retorna mascarado. A imagem Base64 só é retornada quando explicitamente solicitada e necessária.

**Adequação:** os parceiros externos só acessam a API mediante autenticação OAuth2 e escopo explícito (`biometria:read`). Cada parceiro deve possuir um client registrado e rastreável.

**Segurança:** TLS obrigatório, autenticação por token, correlation ID para rastreabilidade, sem exposição do legado ao exterior.

**Não discriminação:** o acesso à biometria não deve ser utilizado para fins discriminatórios. A finalidade de cada parceiro deve ser contratualmente definida pela área jurídica da Vivo.

**Responsabilização:** toda consulta deve ser auditável via correlation ID. Logs estruturados devem ser retidos pelo prazo definido pela política de retenção da Vivo.

### 12.3 Controles Técnicos Obrigatórios em Produção

- Criptografia em trânsito: TLS 1.2+ obrigatório em todas as camadas
- Criptografia em repouso: imagens biométricas no Oracle devem ser criptografadas
- Auditoria: toda consulta registrada com timestamp, parceiro identificado, correlation ID e CPF mascarado
- Retenção de logs: seguir política de retenção da Vivo (mínimo exigido para rastreabilidade e investigação)
- Minimização: não armazenar em cache dado biométrico sem necessidade justificada
- Consentimento / Base legal: verificar com a área jurídica qual base legal se aplica para cada parceiro (consentimento, legítimo interesse, cumprimento de obrigação legal, etc.)
- Titular: garantir que o titular possa exercer os direitos previstos no Art. 18 da LGPD (acesso, correção, eliminação, portabilidade)

### 12.4 Recomendações para Ambiente Produtivo

- Avaliar substituição da imagem Base64 na resposta REST por URL temporária com tempo de expiração (reduz superfície de exposição)
- Registrar cada acesso em log de auditoria imutável (ex: S3 com Object Lock, ou solução equivalente corporativa)
- Estabelecer contrato de processamento de dados (DPA) com cada parceiro externo
- Submeter a arquitetura à revisão do DPO (Data Protection Officer) da Vivo antes do go-live
- Incluir análise de impacto à proteção de dados (DPIA) para o fluxo completo

---

## 13. Observabilidade

### 13.1 Logs Estruturados

Formato JSON com campos mínimos: `timestamp`, `level`, `correlationId`, `service`, `event`, `durationMs`, `httpStatus`.

Campos proibidos nos logs: `cpf` completo, `imagemBase64`, `accessToken`, `soapEnvelope`.

### 13.2 Correlation ID

Gerado pelo Gateway se ausente na requisição. Propagado internamente via header `X-Correlation-Id`. Inserido no MDC do log em cada serviço. Retornado na resposta ao parceiro.

### 13.3 Métricas

Métricas expostas via `/actuator/metrics` (Micrometer):

- `biometria.consultas.total` — total de consultas por parceiro
- `biometria.consultas.sucesso` — consultas com retorno 200
- `biometria.consultas.nao_encontrada` — retorno 404
- `biometria.legado.falhas` — falhas técnicas no SOAP (502/504)
- `biometria.legado.latencia` — latência da chamada SOAP (percentis)
- `biometria.api.latencia` — latência total da API

### 13.4 Health Checks

`/actuator/health` exposto na Biometria Core API e no Gateway. O Legacy SOAP Service deve ser monitorado como dependência downstream.

### 13.5 Tracing Distribuído

A solução está preparada para OpenTelemetry. O Correlation ID já proporciona rastreabilidade básica. A instrumentação com `spring-boot-starter-actuator` + `micrometer-tracing` deve ser adicionada na evolução pós-POC.

---

## 14. Versionamento da API

Estratégia: **URI Versioning**.

| Versão | Rota | Status |
|---|---|---|
| v1 | `/api/v1/biometria/{cpf}` | Ativa (POC) |
| v2+ | `/api/v2/biometria/{cpf}` | Futura, quando houver breaking change |

Regras:
- Novos campos opcionais na resposta não requerem nova versão
- Remoção de campos, mudança de tipo ou semântica requerem nova versão
- Versões antigas são mantidas por prazo contratual com parceiros
- O Gateway roteia explicitamente cada versão

---

## 15. Resiliência

| Cenário | Comportamento |
|---|---|
| SOAP indisponível | HTTP 502 com `LEGADO_INDISPONIVEL` |
| SOAP timeout | HTTP 504 com `TIMEOUT_LEGADO` |
| CPF inválido | HTTP 400 com `CPF_INVALIDO` |
| Token ausente/inválido | HTTP 401 com `NAO_AUTENTICADO` |
| Escopo insuficiente | HTTP 403 com `ACESSO_NEGADO` |
| Biometria não encontrada | HTTP 404 com `BIOMETRIA_NAO_ENCONTRADA` |
| Erro inesperado | HTTP 500 com `ERRO_INTERNO` |

Evoluções planejadas: Circuit Breaker (Resilience4j), Retry controlado, Rate Limit por parceiro.

---

## 16. Estratégia de Migração para Microsserviços Plenos

A arquitetura atual é intencional e transitória: encapsula o legado SOAP sem substituí-lo. A migração para microsserviços plenos deve ocorrer de forma gradual, preservando estabilidade e compatibilidade.

### 16.1 Estado Atual (POC)

```
Parceiro → API Gateway → Biometria Core API → SOAP Adapter → Legacy SOAP → Oracle
```

O legado é o System of Record. O Core API é uma fachada sobre ele.

### 16.2 Fase 1 — Estabilização (pós-POC, 0–3 meses)

Objetivo: colocar a solução em produção com o legado preservado.

- Substituir H2 por Oracle real
- Conectar ao SOAP corporativo real
- Adicionar Circuit Breaker (Resilience4j)
- Adicionar Retry com backoff exponencial
- Ativar TLS em todas as rotas
- Configurar Keycloak em ambiente corporativo
- Ativar coleta de métricas com Prometheus/Grafana
- Ativar tracing distribuído com OpenTelemetry

### 16.3 Fase 2 — Estrangulamento do Legado (3–12 meses)

Objetivo: reduzir dependência do SOAP introduzindo uma camada de dados própria.

Padrão aplicado: **Strangler Fig** — o novo sistema cresce em torno do legado até substituí-lo completamente.

Ações:
- Criar base de dados própria do domínio Biometria (PostgreSQL ou Oracle dedicado)
- Implementar pipeline de sincronização do legado para a nova base (CDC ou batch)
- Modificar o SOAP Adapter para consultar a nova base em primeiro lugar (cache de dados)
- Manter fallback ao SOAP legado enquanto a nova base não estiver completa
- Expor evento `BiometriaConsultada` via mensageria (Kafka / SQS) para outros domínios

### 16.4 Fase 3 — Microsserviço Autônomo (12–24 meses)

Objetivo: eliminar a dependência do SOAP legado.

Ações:
- Migração completa dos dados históricos de biometria para a nova base
- Desativação do SOAP Adapter após validação de cobertura dos dados
- Biometria Core API torna-se microsserviço pleno com persistência própria
- Publicação de eventos de domínio para consumidores internos
- Separação de responsabilidades: leitura vs. escrita (CQRS se justificado pelo volume)
- API Gateway evolui para plataforma de API Management corporativa (Kong, AWS API Gateway, Azure APIM)

### 16.5 Critérios de Avanço entre Fases

| Fase | Critério de entrada na próxima fase |
|---|---|
| POC → Fase 1 | Aprovação arquitetural e testes de integração passando |
| Fase 1 → Fase 2 | 90 dias de estabilidade em produção + cobertura de testes > 80% |
| Fase 2 → Fase 3 | 100% dos dados migrados + zero dependência funcional do SOAP |

### 16.6 Riscos da Migração

| Risco | Mitigação |
|---|---|
| Dados históricos incompletos no legado | Auditoria prévia do Oracle antes da migração |
| Indisponibilidade do SOAP durante a fase 2 | Fallback à nova base + alertas de divergência |
| Breaking change no contrato REST | Manter versão v1 ativa até todos os parceiros migrarem para v2 |
| Vazamento de dado sensível durante migração | Pipeline de migração criptografado, com auditoria e acesso restrito |

---

## 17. Evolução Técnica Planejada

| Item | Fase |
|---|---|
| Oracle real + SOAP corporativo | Fase 1 |
| Circuit Breaker (Resilience4j) | Fase 1 |
| Prometheus + Grafana | Fase 1 |
| OpenTelemetry / Jaeger | Fase 1 |
| Rate Limit por parceiro | Fase 1 |
| Base de dados própria do domínio | Fase 2 |
| Sincronização CDC do legado | Fase 2 |
| Publicação de eventos (Kafka) | Fase 2 |
| Kubernetes / Helm | Fase 2 |
| Microsserviço autônomo | Fase 3 |
| CQRS (se justificado) | Fase 3 |
| API Management corporativo | Fase 3 |

---

## 18. Critérios de Sucesso da POC

- Endpoint `GET /api/v1/biometria/{cpf}` funcional
- Integração SOAP simulada com resposta real do H2
- Diagramas C1, C2 e C3 completos
- OpenAPI documentado e validado
- Logs estruturados com correlation ID e mascaramento de CPF
- Testes unitários com JUnit e Mockito
- README.md explicando decisões e como executar
- Documentação arquitetural consolidada neste DAS

---

## 19. Referências

| Artefato | Local |
|---|---|
| ADR-001 | `docs/adrs/ADR-001-exposicao-legado-soap-via-rest.md` |
| ADR-002 | `docs/adrs/ADR-002-api-gateway-ponto-unico-entrada.md` |
| ADR-003 | `docs/adrs/ADR-003-adapter-anti-corruption-layer.md` |
| ADR-004 | `docs/adrs/ADR-004-oauth2-jwt-keycloak.md` |
| ADR-005 | `docs/adrs/ADR-005-observabilidade-logs-metricas-tracing.md` |
| ADR-006 | `docs/adrs/ADR-006-versionamento-apis.md` |
| C1 System Context | `docs/diagrams/c4/C1-System-Context.md` |
| C2 Container | `docs/diagrams/c4/C2-Container.md` |
| C3 Component | `docs/diagrams/c4/C3-Component-Core-API.md` |
| OpenAPI v1 | `docs/openapi/openapi-v1.yaml` |
| Development Blueprint | `docs/architecture/DEVELOPMENT-BLUEPRINT.md` |
| Lei nº 13.709/2018 | https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm |
