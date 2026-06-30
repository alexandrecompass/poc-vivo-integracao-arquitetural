# ADR-002 — Uso de API Gateway como Ponto Único de Entrada Corporativa

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Governança de acesso, segurança, roteamento e exposição de APIs para parceiros externos

---

## 1. Contexto

A Vivo está implantando um ecossistema de integração entre sistemas internos, microserviços e parceiros externos.

O cenário da POC envolve a exposição de dados de biometria facial por meio de uma API REST moderna, construída sobre um legado que originalmente só é acessível via SOAP. Como esses dados serão consumidos por parceiros externos, a solução precisa respeitar padrões corporativos de segurança, rastreabilidade, versionamento, logging, observabilidade e governança.

Nesse contexto, surge a necessidade de definir se o microserviço **Biometria Core API** será exposto diretamente aos consumidores ou se haverá uma camada intermediária de entrada, representada por um **API Gateway**.

Embora tecnicamente o microserviço core consiga expor diretamente o endpoint REST, a exposição direta de serviços internos para parceiros externos cria riscos de governança, segurança, operação e evolução.

Por isso, esta ADR define a adoção de um API Gateway como ponto único de entrada para consumidores externos.

---

## 2. Problema

A exposição direta de um microserviço interno para parceiros externos pode gerar os seguintes problemas:

- Cada serviço precisaria implementar individualmente políticas de autenticação.
- Regras de autorização poderiam ficar duplicadas.
- Rate limiting teria que ser implementado em cada API.
- Logging de borda ficaria espalhado.
- Roteamento e versionamento ficariam menos centralizados.
- Seria mais difícil aplicar políticas corporativas uniformes.
- Alterações internas poderiam impactar consumidores externos.
- A superfície de ataque aumentaria.
- A governança de APIs ficaria mais frágil.
- A rastreabilidade entre chamadas externas e internas seria menos padronizada.

Além disso, em um ecossistema corporativo com múltiplas APIs e parceiros, é esperado que exista uma camada de borda responsável por padronizar e controlar o acesso aos serviços internos.

O desafio é manter o microserviço core focado na lógica de integração com o legado SOAP, sem misturar responsabilidades de borda, segurança corporativa, roteamento e governança de tráfego.

---

## 3. Decisão

A solução adotará um **API Gateway** como ponto único de entrada para parceiros externos.

O gateway ficará responsável por:

- Receber requisições externas.
- Validar tokens OAuth2/JWT.
- Aplicar políticas de segurança de borda.
- Propagar o token ou contexto de autenticação para serviços internos.
- Propagar ou gerar `correlationId`.
- Realizar roteamento para a Biometria Core API.
- Centralizar logs de entrada.
- Preparar a solução para rate limiting.
- Preparar a solução para controle de quotas.
- Isolar detalhes de topologia interna.
- Permitir evolução da arquitetura sem impacto direto nos consumidores.

O microserviço **Biometria Core API** continuará responsável por:

- Contrato REST de biometria.
- Validação funcional do CPF.
- Orquestração do caso de uso.
- Integração com SOAP Adapter.
- Mapeamento de dados legado → REST.
- Tratamento de erros de negócio e integração.
- Observabilidade do fluxo interno.

A lógica de negócio não deverá ser implementada no gateway.

O gateway deve ser tratado como camada de borda, não como camada de domínio.

---

## 4. Justificativa

A decisão de adotar um API Gateway está alinhada a cenários corporativos de integração em que múltiplos consumidores externos acessam serviços internos.

O gateway permite separar claramente as responsabilidades:

- A borda cuida de acesso, segurança, roteamento e políticas.
- O core cuida do caso de uso e da integração com o legado.
- O legado permanece isolado.

Essa separação evita que cada microserviço precise reinventar mecanismos transversais de segurança, observabilidade e controle de tráfego.

Além disso, em um cenário da Vivo, onde governança de APIs é requisito explícito, a presença de um gateway fortalece a argumentação arquitetural, pois demonstra preocupação com padronização corporativa, segurança e evolução futura.

---

## 5. Responsabilidades do API Gateway

O API Gateway deve possuir responsabilidades bem delimitadas.

### 5.1 Responsabilidades permitidas

- Autenticação de tokens JWT.
- Validação de issuer, audience e assinatura.
- Roteamento para serviços internos.
- Aplicação de políticas de CORS, quando aplicável.
- Controle de headers técnicos.
- Geração ou propagação de correlation ID.
- Logging técnico de entrada e saída.
- Rate limit, quando habilitado.
- Circuit breaker de borda, se aplicável.
- Reescrita simples de path, quando necessária.
- Bloqueio de chamadas não autorizadas.
- Encaminhamento para versões específicas de APIs.

### 5.2 Responsabilidades proibidas

O gateway não deve:

- Implementar regra de negócio.
- Consultar diretamente o legado SOAP.
- Fazer transformação complexa de domínio.
- Conhecer o modelo de biometria legado.
- Persistir dados de negócio.
- Decidir se um CPF possui biometria.
- Realizar validações funcionais complexas.
- Substituir a camada de aplicação do microserviço core.

Essa separação é importante para evitar que o gateway se torne um ponto de acoplamento excessivo.

---

## 6. Alternativas Consideradas

### 6.1 Expor diretamente a Biometria Core API

Essa alternativa foi considerada por ser mais simples para a POC.

A Biometria Core API poderia expor diretamente:

```http
GET /api/v1/biometria/{cpf}
```

E também poderia validar o token JWT diretamente.

Essa abordagem reduziria o número de componentes, simplificaria a execução local e diminuiria o esforço inicial.

Porém, ela foi descartada como arquitetura principal porque enfraquece o argumento corporativo do case. O enunciado menciona parceiros externos, governança, segurança OAuth2, logging e observabilidade. Esses requisitos indicam uma necessidade clara de borda controlada.

A exposição direta também tornaria mais difícil evoluir para múltiplas APIs e múltiplos consumidores externos.

---

### 6.2 Implementar autenticação apenas dentro do microserviço core

Essa alternativa também foi considerada.

Nesse cenário, o core validaria JWT, aplicaria autorização, faria logging e integraria com o legado SOAP.

O problema é que a Biometria Core API passaria a acumular responsabilidades transversais de borda e responsabilidades de negócio.

Isso aumentaria acoplamento e reduziria a clareza arquitetural.

A decisão adotada permite que o core continue protegido e especializado no domínio de biometria.

---

### 6.3 Usar apenas um reverse proxy simples

Um reverse proxy simples, como Nginx, poderia encaminhar chamadas para a API interna.

Essa alternativa resolveria parte do problema de roteamento, mas não atenderia plenamente aos requisitos de governança de APIs.

Um reverse proxy tradicional não oferece, por padrão, a mesma clareza conceitual para:

- Validação OAuth2/JWT integrada.
- Políticas de API.
- Versionamento por rota.
- Observabilidade específica de APIs.
- Rate limit por consumidor.
- Controle de quota.
- Integração com identidade corporativa.

Por isso, essa alternativa foi descartada como solução principal.

---

### 6.4 Colocar transformação SOAP/REST diretamente no Gateway

Essa alternativa foi descartada.

Embora alguns gateways suportem transformação de payload, colocar a tradução SOAP/REST no gateway geraria alto acoplamento entre borda e legado.

O gateway passaria a conhecer detalhes do contrato SOAP, XML, WSDL e modelo legado.

Isso violaria a separação de responsabilidades e dificultaria testes e manutenção.

A transformação deve permanecer no SOAP Adapter dentro da Biometria Core API.

---

## 7. Consequências Positivas

A adoção do API Gateway traz benefícios relevantes:

- Centralização do ponto de entrada.
- Melhor governança de APIs.
- Redução da superfície de exposição dos serviços internos.
- Validação centralizada de autenticação.
- Possibilidade de aplicar rate limiting.
- Possibilidade de aplicar quotas por parceiro.
- Padronização de headers técnicos.
- Melhor rastreabilidade de chamadas externas.
- Isolamento da topologia interna.
- Facilidade para versionamento e roteamento.
- Possibilidade de evolução para múltiplas APIs.
- Melhor aderência a padrões corporativos.
- Separação clara entre borda e domínio.

---

## 8. Consequências Negativas

A decisão também possui custos:

- Introdução de mais um componente.
- Mais complexidade de configuração.
- Mais um ponto de operação e monitoramento.
- Potencial aumento de latência.
- Necessidade de configurar rotas e segurança corretamente.
- Possibilidade de falhas no gateway impactarem consumidores.
- Necessidade de testes integrados adicionais.

Esses custos são considerados aceitáveis para um cenário corporativo com parceiros externos e dados sensíveis.

---

## 9. Trade-offs

A arquitetura adota maior maturidade de governança em troca de maior complexidade inicial.

Para uma aplicação pequena e interna, talvez o gateway fosse desnecessário. Porém, para uma integração corporativa envolvendo parceiros externos, OAuth2, versionamento, rastreabilidade e dados sensíveis, o gateway é uma escolha arquitetural coerente.

O gateway não deve ser visto apenas como um detalhe técnico, mas como uma fronteira arquitetural entre o ecossistema externo e os serviços internos.

---

## 10. Impacto na Arquitetura C4

### 10.1 C1 — System Context

No diagrama de contexto, os parceiros externos interagem com a solução corporativa por meio de uma API exposta.

O gateway pode aparecer como parte do sistema da Vivo ou como um sistema/container de borda, dependendo do nível de abstração escolhido.

A ideia principal no C1 é mostrar que parceiros externos não acessam diretamente o legado SOAP nem o banco Oracle.

---

### 10.2 C2 — Container Diagram

No nível C2, o API Gateway deve aparecer explicitamente como container.

Containers principais:

- Parceiro Externo.
- API Gateway.
- Keycloak.
- Biometria Core API.
- Legacy SOAP Service.
- Banco H2 simulando Oracle.

Fluxo principal:

```text
Parceiro Externo
  → API Gateway
  → Biometria Core API
  → Legacy SOAP Service
  → H2/Oracle simulado
```

Fluxo de segurança:

```text
Parceiro Externo
  → Keycloak
  → obtém token JWT
  → API Gateway
  → valida token
  → encaminha para Biometria Core API
```

---

### 10.3 C3 — Component Diagram

No C3, o gateway pode ser detalhado com componentes como:

- Route Configuration.
- JWT Authentication Filter.
- Correlation ID Filter.
- Request Logging Filter.
- Error Handling Filter.
- Biometria Route.

Porém, o C3 principal do case provavelmente deverá focar mais na Biometria Core API.

O detalhamento do gateway pode ser complementar, caso haja tempo.

---

## 11. Impacto na Segurança

O API Gateway será responsável por validar tokens emitidos pelo Keycloak.

A validação deverá considerar:

- Assinatura do token.
- Issuer.
- Audience.
- Expiração.
- Scopes.
- Roles, se aplicável.
- Client ID do consumidor.
- Ambiente de origem, se aplicável.

A autenticação esperada é OAuth2 com JWT.

Para parceiros externos, o fluxo sugerido é:

- Client Credentials, para comunicação sistema-sistema.
- Authorization Code, apenas se houver usuários finais interativos.

Para a POC, o fluxo mais coerente é **Client Credentials**, pois parceiros externos provavelmente consultarão a API de forma sistêmica.

O gateway deverá rejeitar chamadas sem token, com token inválido ou com escopo insuficiente.

Exemplo de escopo:

```text
biometria:read
```

---

## 12. Impacto na Autorização

A autorização poderá ser baseada em escopos.

Exemplo:

```http
GET /api/v1/biometria/{cpf}
Authorization: Bearer <token>
```

Token esperado com escopo:

```json
{
  "scope": "biometria:read",
  "client_id": "partner-client"
}
```

O gateway pode validar o escopo antes de encaminhar a chamada ao core.

A Biometria Core API também pode realizar uma validação defensiva, caso necessário, mas a autorização principal ficará na borda.

Essa abordagem é conhecida como defesa em profundidade quando combinada com validações internas mínimas.

---

## 13. Impacto na Observabilidade

O gateway será o primeiro ponto observável da requisição.

Ele deverá registrar:

- Método HTTP.
- Path.
- Status HTTP.
- Tempo total de resposta.
- Client ID.
- Correlation ID.
- IP de origem, se aplicável.
- Rota de destino.
- Falhas de autenticação.
- Falhas de autorização.
- Falhas de roteamento.

O gateway não deverá registrar:

- Imagem facial.
- Payload sensível.
- Dados biométricos.
- Token completo.
- CPF completo em texto aberto.

O CPF deve ser mascarado quando aparecer em path ou logs.

Exemplo:

```text
cpf=***.***.***-09
```

ou

```text
cpfHash=sha256(...)
```

Para a POC, pode-se usar mascaramento simples no log e documentar hashing como evolução.

---

## 14. Impacto no Correlation ID

O gateway deverá verificar se a requisição contém um header de correlação.

Header recomendado:

```http
X-Correlation-Id
```

Se o header existir, o gateway deverá propagá-lo.

Se não existir, o gateway deverá gerar um novo identificador.

Esse valor deve ser propagado para:

- Biometria Core API.
- SOAP Adapter.
- Legacy SOAP Service, quando possível.
- Logs de todos os serviços.

Essa estratégia permite rastrear uma chamada ponta a ponta.

---

## 15. Impacto no Versionamento

O gateway pode rotear diferentes versões da API.

Versão inicial:

```http
/api/v1/biometria/{cpf}
```

No futuro:

```http
/api/v2/biometria/{cpf}
```

O gateway pode manter rotas separadas:

```text
/api/v1/** → biometria-core-api-v1
/api/v2/** → biometria-core-api-v2
```

Na POC, haverá apenas v1, mas a documentação deve registrar a estratégia.

---

## 16. Impacto na Resiliência

O gateway poderá contribuir com mecanismos de resiliência de borda, como:

- Timeout.
- Retry controlado, quando seguro.
- Circuit breaker.
- Rate limiting.
- Bulkhead, em cenários mais avançados.

Para operações de consulta, retry pode ser considerado mais seguro do que em operações mutáveis. Porém, mesmo em consulta, retry deve ser limitado para evitar sobrecarga no core e no legado SOAP.

Na POC, a resiliência poderá ser documentada e parcialmente preparada. A implementação completa pode ficar como evolução, dependendo do tempo.

---

## 17. Rate Limiting e Quotas

Como o endpoint expõe dado sensível e pode ser consumido por parceiros externos, é recomendável prever rate limiting.

Exemplo de política futura:

```text
partner-client-a → 100 req/min
partner-client-b → 500 req/min
```

Na POC, o rate limiting pode ser documentado como decisão de arquitetura e deixado como evolução técnica.

Se implementado, poderá ser feito no gateway com filtros específicos.

---

## 18. Impacto na LGPD e Dados Sensíveis

Biometria facial é dado pessoal sensível.

Por isso, o gateway ajuda a reforçar controles de exposição, como:

- Exigir autenticação.
- Exigir autorização por escopo.
- Registrar acesso técnico.
- Evitar exposição direta do core.
- Centralizar bloqueios.
- Facilitar auditoria de consumo por parceiro.

A arquitetura deve evitar logs com dados biométricos ou payloads completos.

A documentação deve deixar claro que a POC usa dados fictícios.

---

## 19. Impacto na Operação

A operação precisará monitorar o gateway além do core.

Indicadores importantes:

- Disponibilidade do gateway.
- Latência por rota.
- Erros 4xx.
- Erros 5xx.
- Falhas de autenticação.
- Falhas de autorização.
- Taxa de chamadas por parceiro.
- Saturação de rotas.
- Timeouts para o core.

Em ambiente real, o gateway seria integrado a uma plataforma corporativa de observabilidade.

Na POC, logs estruturados e actuator já serão suficientes para demonstrar a intenção arquitetural.

---

## 20. Tecnologia Sugerida para a POC

Para a POC, o gateway poderá ser implementado com:

- Spring Cloud Gateway.
- Spring Security OAuth2 Resource Server.
- Keycloak como emissor de tokens JWT.
- Actuator para health check.
- Logs estruturados com correlation ID.

Essa escolha mantém a stack alinhada ao ecossistema Java/Spring sugerido no case técnico.

---

## 21. Estrutura Recomendada do Gateway

Sugestão de pacotes:

```text
br.com.vivo.poc.gateway
  config
    GatewayRoutesConfig
    SecurityConfig

  filter
    CorrelationIdGlobalFilter
    RequestLoggingGlobalFilter

  security
    JwtAuthenticationConfig

  error
    GatewayErrorHandler
```

O gateway deve ser pequeno e objetivo.

A maior parte da lógica deve permanecer no core.

---

## 22. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Gateway virar camada de negócio | Alto | Restringir responsabilidades |
| Configuração incorreta de JWT | Alto | Testes e validação de issuer/audience |
| Logs com dados sensíveis | Alto | Mascaramento e política de logging |
| Gateway indisponível | Alto | Health checks e monitoramento |
| Latência adicional | Médio | Métricas e tuning |
| Rotas mal configuradas | Médio | Testes de integração |
| Retry indevido sobrecarregar legado | Médio | Retry limitado e documentado |

---

## 23. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- Existir um módulo/aplicação de gateway.
- O parceiro externo consumir a API pelo gateway.
- O gateway rotear chamadas para a Biometria Core API.
- O gateway validar JWT emitido pelo Keycloak.
- Chamadas sem token forem bloqueadas.
- Chamadas com token inválido forem bloqueadas.
- O correlation ID for gerado ou propagado.
- Logs de borda forem registrados.
- O gateway não contiver regra de negócio.
- O gateway não chamar diretamente o SOAP.
- A decisão estiver refletida no C2.
- O README explicar o papel do gateway.

---

## 24. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-002-api-gateway-ponto-unico-entrada.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Atualizar README com a decisão de gateway.
- [ ] Representar o gateway no diagrama C2.
- [ ] Avaliar se o gateway aparecerá também no C1.
- [ ] Registrar estratégia de autenticação no documento de arquitetura.

### Código — Gateway

- [ ] Criar módulo/aplicação `biometria-gateway`.
- [ ] Configurar Spring Cloud Gateway.
- [ ] Criar rota `/api/v1/biometria/**` para a Biometria Core API.
- [ ] Configurar validação OAuth2 Resource Server.
- [ ] Configurar issuer do Keycloak.
- [ ] Implementar filtro de correlation ID.
- [ ] Implementar filtro de logging básico.
- [ ] Garantir que o gateway não contenha regra de negócio.
- [ ] Configurar Actuator.
- [ ] Configurar logs estruturados.

### Segurança

- [ ] Criar realm/client no Keycloak para a POC.
- [ ] Criar client credentials para parceiro fictício.
- [ ] Criar escopo `biometria:read`.
- [ ] Validar token JWT no gateway.
- [ ] Bloquear chamadas não autenticadas.
- [ ] Bloquear chamadas sem escopo adequado, se implementado.

### Testes

- [ ] Testar chamada sem token.
- [ ] Testar chamada com token inválido.
- [ ] Testar chamada com token válido.
- [ ] Testar roteamento para o core.
- [ ] Testar propagação de correlation ID.
- [ ] Testar geração de correlation ID quando ausente.
- [ ] Testar logs de entrada.
- [ ] Testar indisponibilidade do core.

---

## 25. Decisão Final

A solução adotará um API Gateway como ponto único de entrada para parceiros externos.

O gateway será responsável por segurança de borda, roteamento, propagação de contexto técnico, logging inicial e preparação para políticas corporativas de API.

A Biometria Core API permanecerá responsável pela lógica de integração, validação funcional, tradução REST/SOAP e tratamento de erros.

Essa decisão reforça a separação de responsabilidades e aproxima a POC de uma arquitetura corporativa realista.

---

## 26. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Criar ADR-003 sobre Adapter e Anti-Corruption Layer.
2. Criar ADR-004 sobre OAuth2/JWT com Keycloak.
3. Atualizar o DAS com as decisões ADR-001 e ADR-002.
4. Criar os diagramas C1, C2 e C3.
5. Criar a estrutura inicial dos módulos:
   - `biometria-gateway`
   - `biometria-core-api`
   - `biometria-legacy-soap`
6. Implementar o fluxo mínimo:
   - Gateway → Core → SOAP fake → H2.

