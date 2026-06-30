# ADR-004 — Autenticação e Autorização com OAuth2, JWT e Keycloak

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Segurança de APIs, autenticação de parceiros externos e autorização para consulta de biometria facial

---

## 1. Contexto

A POC Vivo — Integração Arquitetural tem como objetivo expor informações de biometria facial de clientes, atualmente disponíveis apenas por meio de um serviço SOAP legado, através de uma API REST moderna.

O consumo dessa API será realizado por parceiros externos ou sistemas corporativos autorizados. Como biometria facial é dado pessoal sensível, a solução precisa adotar mecanismos robustos de autenticação, autorização, rastreabilidade e governança de acesso.

O case técnico menciona explicitamente a necessidade de seguir padrões de governança da Vivo, incluindo segurança OAuth2, logging e observabilidade. Portanto, a API não deve ser exposta sem controle de identidade, autorização e rastreabilidade.

Esta ADR define o uso de **OAuth2**, **JWT** e **Keycloak** como base da estratégia de autenticação e autorização da POC.

---

## 2. Problema

A exposição de uma API de biometria facial sem um modelo claro de segurança cria riscos graves:

- Acesso indevido a dados sensíveis.
- Falta de rastreabilidade por consumidor.
- Dificuldade de auditar chamadas.
- Ausência de controle por escopo.
- Exposição direta de serviços internos.
- Possibilidade de uso abusivo da API.
- Dificuldade de revogação de acesso.
- Falta de padronização corporativa.
- Maior risco de vazamento de dados pessoais.

Além disso, em um cenário de parceiros externos, a autenticação não deve depender de mecanismos frágeis, como:

- API key simples sem expiração.
- Usuário e senha compartilhados.
- Basic Auth.
- IP whitelist como único controle.
- Tokens opacos sem validação padronizada.
- Segurança implementada manualmente em cada endpoint.

A solução precisa permitir controle de acesso moderno, verificável, extensível e compatível com arquitetura corporativa.

---

## 3. Decisão

A solução adotará **OAuth2 com JWT**, utilizando **Keycloak** como provedor de identidade e autorização para a POC.

O **API Gateway** será responsável pela validação primária do token JWT antes de encaminhar requisições para a **Biometria Core API**.

O fluxo de autenticação recomendado para parceiros externos será o **Client Credentials Flow**, por ser adequado para comunicação sistema-sistema.

A autorização será baseada em escopos, com o escopo inicial:

```text
biometria:read
```

Apenas clientes autorizados com esse escopo poderão consultar:

```http
GET /api/v1/biometria/{cpf}
```

A Biometria Core API poderá aplicar validações defensivas adicionais, mas a autenticação e autorização principais ficarão centralizadas no gateway.

---

## 4. Justificativa

OAuth2 é um padrão amplamente utilizado para autorização em APIs modernas.

JWT permite que o gateway valide tokens de forma stateless, verificando assinatura, expiração, emissor e escopos sem consultar o provedor de identidade a cada requisição.

Keycloak é uma solução adequada para POC porque fornece:

- Realms.
- Clients.
- Client Credentials Flow.
- Emissão de JWT.
- Scopes.
- Roles.
- JWKS endpoint.
- Configuração local via Docker.
- Integração com Spring Security.

Essa combinação permite demonstrar uma arquitetura realista e aderente a cenários corporativos.

---

## 5. Modelo de Segurança Proposto

A solução será composta por:

```text
Parceiro Externo
    ↓ solicita token
Keycloak
    ↓ retorna JWT
Parceiro Externo
    ↓ chama API com Bearer Token
API Gateway
    ↓ valida JWT e escopo
Biometria Core API
    ↓ executa caso de uso
Legacy SOAP Service
```

O parceiro não acessa diretamente a Biometria Core API nem o serviço SOAP legado.

O acesso externo passa pelo gateway.

---

## 6. Fluxo de Autenticação

Fluxo recomendado para a POC:

```text
1. Parceiro externo possui client_id e client_secret.
2. Parceiro solicita token ao Keycloak.
3. Keycloak autentica o client.
4. Keycloak emite JWT com escopo biometria:read.
5. Parceiro chama API REST usando Authorization: Bearer <token>.
6. API Gateway valida o token.
7. API Gateway valida escopo.
8. API Gateway encaminha requisição para Biometria Core API.
9. Core processa a consulta.
10. Resposta retorna ao parceiro.
```

Exemplo conceitual de requisição para token:

```http
POST /realms/vivo-poc/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&
client_id=partner-biometria-client&
client_secret=<secret>&
scope=biometria:read
```

Exemplo conceitual de chamada autenticada:

```http
GET /api/v1/biometria/12345678909
Authorization: Bearer <jwt>
X-Correlation-Id: 7f1d5c90-6e09-4d8f-91a9-9b6cf7a01f4a
```

---

## 7. Por que Client Credentials?

O endpoint de biometria será consumido por parceiros externos ou sistemas corporativos, não necessariamente por usuários finais interativos.

Para esse cenário, o fluxo mais adequado é **Client Credentials**, pois representa autenticação máquina-máquina.

Esse fluxo permite identificar o consumidor técnico da API por meio de:

- `client_id`;
- escopos autorizados;
- claims do token;
- políticas associadas ao client.

Fluxos como Authorization Code seriam mais adequados para aplicações com usuários finais autenticando interativamente em um navegador.

---

## 8. Claims Esperadas no JWT

O token JWT deve conter informações suficientes para autenticação, autorização e rastreabilidade.

Claims esperadas:

```json
{
  "iss": "http://localhost:8081/realms/vivo-poc",
  "sub": "service-account-partner-biometria-client",
  "aud": "biometria-api",
  "azp": "partner-biometria-client",
  "client_id": "partner-biometria-client",
  "scope": "biometria:read",
  "iat": 1782820000,
  "exp": 1782823600
}
```

Observação: os nomes exatos das claims podem variar conforme configuração do Keycloak e do Spring Security.

O importante é garantir que o gateway consiga validar:

- emissor;
- assinatura;
- expiração;
- audiência, se configurada;
- escopo exigido.

---

## 9. Estratégia de Autorização

A autorização será baseada em escopo.

Escopo inicial:

```text
biometria:read
```

Esse escopo permite apenas consulta de biometria.

Caso novas operações sejam adicionadas futuramente, novos escopos deverão ser criados.

Exemplos futuros:

```text
biometria:read
biometria:write
biometria:audit
biometria:admin
```

Para a POC, apenas `biometria:read` deve existir.

Isso evita permissões genéricas e reforça o princípio do menor privilégio.

---

## 10. Papel do API Gateway

O gateway será responsável por:

- Rejeitar requisições sem token.
- Rejeitar tokens inválidos.
- Rejeitar tokens expirados.
- Rejeitar tokens emitidos por issuer não confiável.
- Rejeitar chamadas sem escopo adequado.
- Propagar contexto técnico para o core.
- Registrar logs de autenticação/autorização.
- Propagar `X-Correlation-Id`.

O gateway não deve:

- Decodificar biometria.
- Chamar diretamente o SOAP.
- Implementar regra de negócio.
- Persistir dados sensíveis.
- Registrar token completo em logs.

---

## 11. Papel da Biometria Core API

A Biometria Core API receberá chamadas já autenticadas pelo gateway.

Ainda assim, ela poderá aplicar segurança defensiva, como:

- aceitar apenas chamadas vindas da rede interna;
- validar headers técnicos;
- validar correlation ID;
- opcionalmente validar JWT novamente;
- verificar escopo defensivamente em ambientes mais rígidos.

Para a POC, a validação principal ficará no gateway para demonstrar separação de responsabilidades.

---

## 12. Alternativas Consideradas

### 12.1 Basic Auth

Basic Auth foi descartado.

Embora simples, não é adequado para APIs modernas consumidas por parceiros externos com dados sensíveis.

Problemas:

- Credenciais enviadas repetidamente.
- Menor granularidade de autorização.
- Revogação menos flexível.
- Ausência de escopos.
- Menor aderência a padrões modernos de governança.

---

### 12.2 API Key simples

API Key foi considerada, mas descartada como mecanismo principal.

Embora API keys sejam comuns para identificação de consumidores, isoladamente elas não oferecem a mesma robustez de OAuth2/JWT.

Problemas:

- Geralmente não carregam escopos.
- Podem ser difíceis de revogar seletivamente.
- Não representam bem autorização granular.
- Podem acabar sendo usadas como segredo permanente.
- Não oferecem assinatura e claims padronizadas como JWT.

API Key poderia ser usada como controle complementar, mas não como segurança principal.

---

### 12.3 Validação manual de token no código

Foi descartada.

Implementar validação JWT manualmente aumenta risco de erro, como:

- não validar assinatura corretamente;
- ignorar expiração;
- aceitar issuer inválido;
- não validar audience;
- interpretar escopos de forma inconsistente.

A solução deve usar Spring Security OAuth2 Resource Server no gateway.

---

### 12.4 Segurança apenas na Biometria Core API

Essa alternativa foi considerada, mas não será adotada como estratégia principal.

Embora seja possível proteger diretamente o core, isso reduz o papel do gateway como ponto de governança corporativa.

A decisão adotada centraliza segurança de borda no gateway e mantém o core focado no caso de uso.

---

### 12.5 Autenticação mTLS como mecanismo principal

mTLS é uma opção robusta para comunicação sistema-sistema e pode ser usada em ambientes corporativos.

Porém, para a POC, mTLS adicionaria complexidade operacional maior do que o necessário.

A solução documentará mTLS como evolução possível para ambientes produtivos de alta criticidade.

---

## 13. Consequências Positivas

A decisão traz os seguintes benefícios:

- Segurança alinhada a APIs modernas.
- Controle de acesso por escopo.
- Autenticação adequada para parceiros externos.
- Integração natural com Spring Security.
- Validação stateless de tokens JWT.
- Facilidade de revogação/configuração por client.
- Melhor rastreabilidade por consumidor.
- Aderência a governança corporativa.
- Separação entre borda e domínio.
- Redução de exposição dos serviços internos.
- Demonstração clara de maturidade arquitetural.

---

## 14. Consequências Negativas

A decisão também adiciona alguns custos:

- Necessidade de configurar Keycloak.
- Necessidade de configurar clients e scopes.
- Necessidade de entender fluxo OAuth2.
- Mais componentes em execução local.
- Possibilidade de falhas por configuração incorreta.
- Curva de aprendizado maior para executar a POC.

Esses custos são aceitáveis porque segurança é requisito central do case.

---

## 15. Trade-offs

A solução escolhe segurança e governança em troca de maior complexidade local.

Para uma POC mínima, seria possível expor a API sem autenticação ou com um token fixo. Porém, isso enfraqueceria fortemente a proposta arquitetural.

Como o case avalia arquitetura de integração, a adoção de OAuth2/JWT com Keycloak demonstra alinhamento com práticas corporativas reais.

---

## 16. Impacto na Arquitetura C4

### 16.1 C1 — System Context

No C1, o parceiro externo aparece como ator/sistema consumidor.

O Keycloak pode aparecer como sistema externo de identidade/autorização.

O sistema da POC depende do Keycloak para autenticar e autorizar chamadas.

### 16.2 C2 — Container Diagram

No C2, Keycloak deve aparecer explicitamente como container/sistema de identidade.

Fluxos relevantes:

```text
Parceiro Externo → Keycloak
Parceiro Externo → API Gateway
API Gateway → Keycloak/JWKS
API Gateway → Biometria Core API
```

O gateway valida o token usando metadados/JWKS do Keycloak.

### 16.3 C3 — Component Diagram

No C3 do gateway, podem aparecer:

- Security Configuration.
- JWT Authentication Filter.
- Scope Authorization Filter.
- Correlation ID Filter.
- Route Filter.

No C3 do core, segurança aparece apenas como validação defensiva/contexto propagado.

---

## 17. Impacto na Observabilidade

A autenticação e autorização devem gerar eventos observáveis.

Logs esperados no gateway:

- Token ausente.
- Token inválido.
- Token expirado.
- Escopo insuficiente.
- Client autorizado.
- Client ID.
- Rota acessada.
- Status HTTP.
- Correlation ID.

Dados que não devem ser logados:

- JWT completo.
- Client secret.
- Dados biométricos.
- Imagem facial.
- CPF completo.
- Payload sensível.

Exemplo de log seguro:

```json
{
  "event": "api_access_granted",
  "clientId": "partner-biometria-client",
  "scope": "biometria:read",
  "path": "/api/v1/biometria/**",
  "status": 200,
  "correlationId": "7f1d5c90-6e09-4d8f-91a9-9b6cf7a01f4a"
}
```

---

## 18. Impacto na LGPD

Biometria facial é dado pessoal sensível.

A estratégia de OAuth2/JWT contribui para LGPD porque permite:

- Identificar quem acessou.
- Controlar finalidade por escopo.
- Restringir acesso a consumidores autorizados.
- Auditar chamadas.
- Revogar acesso de parceiros.
- Reduzir exposição indevida.

A solução deve deixar claro que:

- Os dados da POC são fictícios.
- Logs não devem armazenar imagem facial.
- Logs não devem armazenar CPF completo.
- Tokens e secrets não devem ser versionados.
- Acesso deve ser mínimo e justificado.

---

## 19. Impacto no README e Execução Local

O README deverá explicar:

- Como subir Keycloak.
- Qual realm será usado.
- Como criar/configurar client.
- Como obter token.
- Como chamar a API pelo gateway.
- Como testar chamada sem token.
- Como testar chamada com token válido.
- Como testar escopo insuficiente, se implementado.

Exemplo de chamada esperada:

```bash
curl -X GET http://localhost:8080/api/v1/biometria/12345678909 \
  -H "Authorization: Bearer <token>" \
  -H "X-Correlation-Id: demo-001"
```

---

## 20. Configuração Recomendada

### 20.1 Realm

```text
vivo-poc
```

### 20.2 Client

```text
partner-biometria-client
```

### 20.3 Grant Type

```text
client_credentials
```

### 20.4 Scope

```text
biometria:read
```

### 20.5 Audience

```text
biometria-api
```

### 20.6 Gateway Resource Server

O gateway deverá validar tokens emitidos pelo issuer:

```text
http://localhost:<porta-keycloak>/realms/vivo-poc
```

A porta será definida no Docker Compose da POC.

---

## 21. Regras de Implementação

A implementação deverá seguir as regras abaixo:

1. Toda chamada externa deve passar pelo API Gateway.
2. Toda chamada ao endpoint de biometria deve exigir token JWT.
3. O token deve ser emitido pelo Keycloak.
4. O token deve conter escopo `biometria:read`.
5. O gateway deve rejeitar requisições sem token.
6. O gateway deve rejeitar tokens inválidos ou expirados.
7. O gateway deve rejeitar escopo insuficiente.
8. Secrets não devem ser commitados em repositório.
9. O JWT completo não deve ser registrado em logs.
10. Dados biométricos não devem ser registrados em logs.
11. CPF deve ser mascarado ou omitido em logs.
12. O core não deve ser exposto diretamente para parceiros externos.

---

## 22. Tratamento de Erros de Segurança

Respostas esperadas:

| Cenário | HTTP | Observação |
|---|---:|---|
| Token ausente | 401 | Não autenticado |
| Token inválido | 401 | Assinatura, issuer ou formato inválido |
| Token expirado | 401 | Credencial vencida |
| Escopo insuficiente | 403 | Autenticado, mas sem permissão |
| Client não autorizado | 403 | Consumidor não habilitado |
| Erro interno de validação | 500 | Falha inesperada no gateway |

A resposta não deve revelar detalhes internos sensíveis.

---

## 23. Segurança em Ambiente Produtivo

Embora a POC use ambiente local, em produção seriam recomendados controles adicionais:

- TLS obrigatório.
- mTLS entre parceiros e borda, se aplicável.
- Rotação de secrets.
- Vault para credenciais.
- Rate limiting por client.
- Quotas por parceiro.
- Auditoria centralizada.
- SIEM.
- WAF.
- Detecção de abuso.
- Revisão periódica de permissões.
- Expiração curta de tokens.
- Segregação por ambiente.
- Integração com IAM corporativo.

Esses itens podem ser mencionados como evolução futura no documento de arquitetura.

---

## 24. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- Existir Keycloak configurado para a POC.
- Existir realm `vivo-poc`.
- Existir client para parceiro fictício.
- O fluxo Client Credentials funcionar.
- O gateway validar JWT emitido pelo Keycloak.
- O endpoint `/api/v1/biometria/{cpf}` exigir autenticação.
- O escopo `biometria:read` for documentado.
- Chamadas sem token retornarem 401.
- Chamadas com token inválido retornarem 401.
- Chamadas sem escopo adequado retornarem 403, se a validação de escopo for implementada.
- Logs não exibirem token completo.
- Logs não exibirem CPF completo.
- A decisão estiver refletida no C1/C2.
- O README explicar como obter token e chamar a API.

---

## 25. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-004-oauth2-jwt-keycloak.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Atualizar README com estratégia de segurança.
- [ ] Documentar fluxo Client Credentials.
- [ ] Documentar escopo `biometria:read`.
- [ ] Documentar exemplo de obtenção de token.
- [ ] Documentar exemplo de chamada autenticada.
- [ ] Refletir Keycloak nos diagramas C1 e C2.

### Infraestrutura

- [ ] Adicionar Keycloak ao Docker Compose.
- [ ] Definir realm `vivo-poc`.
- [ ] Definir client `partner-biometria-client`.
- [ ] Habilitar Client Credentials.
- [ ] Criar escopo `biometria:read`.
- [ ] Configurar audience `biometria-api`, se aplicável.
- [ ] Garantir que secrets não sejam commitados.
- [ ] Criar instruções para configuração local.

### Código — Gateway

- [ ] Adicionar dependência de Spring Security OAuth2 Resource Server.
- [ ] Configurar issuer URI do Keycloak.
- [ ] Configurar validação JWT.
- [ ] Configurar proteção da rota `/api/v1/biometria/**`.
- [ ] Configurar exigência de escopo `biometria:read`.
- [ ] Implementar tratamento de 401.
- [ ] Implementar tratamento de 403.
- [ ] Garantir que token completo não seja logado.
- [ ] Propagar contexto técnico para o core.

### Código — Core

- [ ] Manter endpoint preparado para chamadas vindas do gateway.
- [ ] Validar headers técnicos, se necessário.
- [ ] Evitar exposição direta em documentação pública.
- [ ] Não duplicar regra pesada de autenticação sem necessidade.
- [ ] Registrar logs com clientId propagado, se disponível.

### Testes

- [ ] Testar chamada sem token.
- [ ] Testar chamada com token inválido.
- [ ] Testar chamada com token expirado, se possível.
- [ ] Testar chamada com token válido.
- [ ] Testar chamada sem escopo adequado, se possível.
- [ ] Testar propagação de correlation ID com token válido.
- [ ] Testar que logs não expõem JWT.
- [ ] Testar que logs não expõem CPF completo.

---

## 26. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Token inválido aceito | Alto | Usar Spring Security e validar issuer |
| Escopo não validado | Alto | Configurar autorização por escopo |
| Secret versionado | Alto | Usar variáveis de ambiente |
| JWT completo em log | Alto | Sanitização de logs |
| CPF completo em log | Alto | Mascaramento |
| Keycloak mal configurado | Médio | README e configuração automatizada |
| Gateway ignorando audience | Médio | Validar audience quando configurada |
| Complexidade local excessiva | Médio | Documentar execução simples |

---

## 27. Decisão Final

A solução adotará OAuth2 com JWT e Keycloak para autenticação e autorização.

O fluxo principal será Client Credentials, adequado para comunicação sistema-sistema com parceiros externos.

O API Gateway será responsável pela validação primária dos tokens e pela autorização baseada em escopo.

A Biometria Core API permanecerá protegida atrás do gateway e focada na lógica de aplicação e integração com o legado SOAP.

Essa decisão reforça segurança, governança, rastreabilidade e aderência a padrões corporativos de APIs.

---

## 28. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Criar ADR-005 sobre observabilidade, logs, métricas e tracing.
2. Atualizar o DAS com a decisão de segurança.
3. Atualizar os diagramas C1 e C2 incluindo Keycloak.
4. Criar estrutura inicial do Docker Compose.
5. Criar configuração local do Keycloak.
6. Criar o módulo Gateway com Resource Server.
7. Criar exemplos de chamadas autenticadas no README.
