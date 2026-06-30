# C4 Model — C2 Container Diagram

> **Projeto:** POC Vivo – Integração Arquitetural  
> **Artefato:** C2 – Container Diagram  
> **Versão:** 1.0  
> **Status:** Draft

---

# 1. Objetivo

O diagrama C2 detalha os principais containers da **Plataforma de Integração de Biometria**, mostrando as aplicações executáveis, bancos, serviços de apoio e suas relações técnicas.

No C4 Model, um **container** não significa necessariamente Docker. Um container representa uma unidade executável ou armazenadora de dados, como:

- aplicação backend;
- gateway;
- banco de dados;
- serviço legado;
- provedor de identidade;
- aplicação frontend;
- fila;
- cache;
- serviço externo.

Nesta POC, o C2 mostra como o parceiro externo acessa a solução e como a chamada atravessa os componentes internos até o legado SOAP.

---

# 2. Sistema em Foco

O sistema em foco continua sendo:

```text
Plataforma de Integração de Biometria
```

Responsabilidade geral:

- Expor biometria facial via API REST.
- Proteger acesso com OAuth2/JWT.
- Encapsular o legado SOAP.
- Traduzir contratos REST/SOAP.
- Garantir logging, rastreabilidade e observabilidade.
- Preservar o legado como origem oficial dos dados.

---

# 3. Containers Identificados

## 3.1 API Gateway

**Tipo:** Aplicação Spring Boot / Spring Cloud Gateway  
**Responsabilidade:** Ponto único de entrada para parceiros externos.

Responsabilidades principais:

- Receber chamadas externas.
- Validar JWT emitido pelo Keycloak.
- Verificar escopo `biometria:read`.
- Propagar `X-Correlation-Id`.
- Roteamento para a Biometria Core API.
- Logging de borda.
- Preparação para rate limit e quotas.
- Isolar a topologia interna.

O gateway não deve conter regra de negócio.

---

## 3.2 Biometria Core API

**Tipo:** Microserviço Spring Boot REST  
**Responsabilidade:** Implementar o caso de uso de consulta de biometria.

Responsabilidades principais:

- Expor endpoint REST versionado.
- Validar CPF.
- Orquestrar consulta de biometria.
- Chamar porta de integração com legado.
- Traduzir resposta SOAP para REST.
- Tratar erros técnicos e funcionais.
- Aplicar logs estruturados.
- Propagar correlation ID.
- Expor OpenAPI.
- Expor health checks.

Endpoint principal:

```http
GET /api/v1/biometria/{cpf}
```

---

## 3.3 Legacy SOAP Service

**Tipo:** Aplicação Spring Boot SOAP fake  
**Responsabilidade:** Simular o serviço legado SOAP interno.

Responsabilidades principais:

- Expor operação SOAP fictícia.
- Receber CPF.
- Consultar base H2.
- Retornar biometria fictícia.
- Simular cenários de erro.
- Representar o sistema legado Oracle/SOAP do case.

---

## 3.4 H2 Database

**Tipo:** Banco relacional em memória ou arquivo  
**Responsabilidade:** Simular o banco Oracle legado.

Responsabilidades principais:

- Armazenar dados fictícios de biometria.
- Representar a tabela Oracle onde imagens estariam armazenadas.
- Permitir execução simples da POC localmente.

Observação:

O H2 é uma escolha de POC. Em ambiente real, este papel seria ocupado por Oracle.

---

## 3.5 Keycloak

**Tipo:** Identity Provider / Authorization Server  
**Responsabilidade:** Emitir tokens OAuth2/JWT.

Responsabilidades principais:

- Realm da POC.
- Client Credentials.
- Client de parceiro externo.
- Escopo `biometria:read`.
- Emissão de JWT.
- Exposição de JWKS para validação pelo gateway.

---

## 3.6 Parceiro Externo

**Tipo:** Sistema consumidor  
**Responsabilidade:** Consumir a API de biometria.

Responsabilidades principais:

- Obter token OAuth2.
- Chamar API REST versionada.
- Enviar header `Authorization`.
- Opcionalmente enviar `X-Correlation-Id`.

---

# 4. Relações entre Containers

| Origem | Destino | Protocolo | Descrição |
|---|---|---|---|
| Parceiro Externo | Keycloak | HTTPS / OAuth2 | Solicita token via Client Credentials |
| Keycloak | Parceiro Externo | JWT | Retorna token assinado |
| Parceiro Externo | API Gateway | HTTPS REST | Consulta biometria por CPF |
| API Gateway | Keycloak | JWKS / JWT Metadata | Valida assinatura e issuer |
| API Gateway | Biometria Core API | HTTP REST | Roteia requisição autorizada |
| Biometria Core API | Legacy SOAP Service | SOAP / HTTP | Consulta biometria no legado |
| Legacy SOAP Service | H2 Database | JDBC | Consulta dados fictícios |

---

# 5. Fluxo Principal

```text
1. Parceiro solicita token ao Keycloak.
2. Keycloak emite JWT com escopo biometria:read.
3. Parceiro chama GET /api/v1/biometria/{cpf} no Gateway.
4. Gateway valida token e escopo.
5. Gateway gera ou propaga X-Correlation-Id.
6. Gateway roteia a chamada para a Biometria Core API.
7. Core valida CPF.
8. Core executa o caso de uso de consulta.
9. Core chama o SOAP Adapter.
10. SOAP Adapter chama o Legacy SOAP Service.
11. Legacy SOAP consulta o H2.
12. Legacy SOAP retorna dados fictícios.
13. Core converte resposta SOAP para REST.
14. Gateway devolve resposta ao parceiro.
```

---

# 6. Responsabilidades de Segurança

## API Gateway

- Validação primária do JWT.
- Validação de escopo.
- Bloqueio de chamadas inválidas.
- Proteção da rota pública.
- Não exposição dos serviços internos.

## Keycloak

- Emissão de token.
- Gestão de client.
- Gestão de escopo.
- Assinatura do JWT.

## Biometria Core API

- Validação funcional do CPF.
- Não exposição de dados sensíveis nos logs.
- Tratamento seguro de erros.
- Defesa em profundidade, se necessário.

---

# 7. Responsabilidades de Observabilidade

## API Gateway

Logs esperados:

- Requisição recebida.
- Token válido/inválido.
- Escopo negado.
- Rota encaminhada.
- Status final.
- Duração da chamada.
- Correlation ID.

## Biometria Core API

Logs esperados:

- Requisição recebida.
- CPF validado.
- Chamada SOAP iniciada.
- Chamada SOAP finalizada.
- Tempo da chamada SOAP.
- Erros funcionais/técnicos.
- Correlation ID.

## Legacy SOAP Service

Logs esperados:

- Requisição SOAP recebida.
- Consulta H2 iniciada/finalizada.
- Resposta gerada.
- Erro simulado.
- Correlation ID, quando propagado.

---

# 8. Dados Sensíveis

A arquitetura trata biometria facial, que é dado pessoal sensível.

Portanto:

- CPF completo não deve ser logado.
- Imagem/Base64 não deve ser logada.
- JWT completo não deve ser logado.
- SOAP envelope completo não deve ser logado.
- Payload sensível não deve ser registrado.

---

# 9. Estratégia de Versionamento

A API pública nasce versionada por URI:

```http
/api/v1/biometria/{cpf}
```

O API Gateway deve rotear explicitamente a versão `v1`.

Exemplo:

```text
/api/v1/biometria/** → Biometria Core API
```

---

# 10. Resiliência no Nível C2

A POC deve prever os seguintes pontos:

- Timeout entre Gateway e Core.
- Timeout entre Core e Legacy SOAP.
- Tratamento de indisponibilidade do legado.
- Retorno 502 para falha técnica no legado.
- Retorno 504 para timeout.
- Logs de falha com correlation ID.
- Futuro circuit breaker.
- Futuro retry controlado.
- Futuro rate limit por parceiro.

---

# 11. Decisões Relacionadas

| ADR | Relação com o C2 |
|---|---|
| ADR-001 | Define Core API como fachada REST sobre SOAP |
| ADR-002 | Define API Gateway como ponto único de entrada |
| ADR-003 | Define isolamento do legado dentro do Core |
| ADR-004 | Define Keycloak e OAuth2/JWT |
| ADR-005 | Define logs, métricas e tracing |
| ADR-006 | Define rota versionada `/api/v1` |

---

# 12. Containers Deliberadamente Omitidos

Não aparecem no C2 como containers principais:

- Swagger UI, pois é documentação técnica do Core.
- OpenAPI, pois é artefato de contrato.
- Logs/Métricas, pois podem ser descritos como preocupação transversal.
- Prometheus/Grafana, pois são evolução futura.
- Oracle real, pois na POC será representado por H2.
- Receita Federal, pois foi removida do escopo para evitar desvio do case.

---

# 13. Notas de Arquitetura

- O Gateway é recomendado mesmo que o Core pudesse expor a API diretamente.
- O Gateway representa governança corporativa.
- O Core representa a lógica de integração.
- O Legacy SOAP representa a restrição real do case.
- O H2 simplifica a execução local sem descaracterizar o cenário.
- Keycloak reforça segurança e aderência a OAuth2/JWT.
- A solução evita overengineering ao não incluir integrações fora do escopo, como Receita Federal.

---

# 14. Checklist do Artefato

## Documentação

- [x] Containers identificados.
- [x] Responsabilidades descritas.
- [x] Relações técnicas descritas.
- [x] Segurança descrita.
- [x] Observabilidade descrita.
- [x] Versionamento descrito.
- [x] Resiliência descrita.
- [x] ADRs relacionadas.
- [ ] Gerar Mermaid C2.
- [ ] Gerar Structurizr DSL C2.
- [ ] Gerar Draw.io C2.

## Implementação futura

- [ ] Criar módulo `biometria-gateway`.
- [ ] Criar módulo `biometria-core-api`.
- [ ] Criar módulo `biometria-legacy-soap`.
- [ ] Configurar Keycloak.
- [ ] Configurar H2.
- [ ] Configurar comunicação Gateway → Core.
- [ ] Configurar comunicação Core → SOAP.
- [ ] Configurar logs e correlation ID.
- [ ] Configurar Actuator.

---

# 15. Próximo Artefato

**C2 — Container Diagram em Mermaid**

Arquivo sugerido:

```text
docs/diagrams/c4/C2-Container.mmd
```
