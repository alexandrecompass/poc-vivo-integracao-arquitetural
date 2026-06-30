# ADR-001 — Exposição do Legado SOAP por meio de API REST

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Modernização de integração entre legado SOAP e ecossistema REST corporativo

---

## 1. Contexto

A Vivo está implantando um novo ecossistema de integração entre sistemas internos e parceiros externos. Nesse cenário, o time de Produtos precisa disponibilizar informações de biometria facial de clientes para consumo por parceiros externos.

Atualmente, os dados de biometria facial estão armazenados em um sistema legado baseado em Oracle, com imagens persistidas em tabela. Esse legado não expõe uma API REST moderna e é acessível apenas por meio de um serviço SOAP interno.

A área de Negócios deseja que essa informação esteja disponível por meio de uma API REST, seguindo padrões corporativos de governança, segurança, versionamento, logging, observabilidade e rastreabilidade.

O desafio principal é modernizar a forma de consumo sem alterar diretamente o sistema legado, reduzindo acoplamento, risco operacional e impacto em sistemas já existentes.

---

## 2. Problema

O sistema legado possui limitações típicas de ambientes corporativos antigos:

- Exposição apenas por SOAP.
- Forte acoplamento ao modelo de dados legado.
- Contratos menos amigáveis para parceiros externos.
- Maior dificuldade de versionamento e evolução independente.
- Menor aderência a padrões modernos de APIs REST.
- Possível ausência de mecanismos modernos de observabilidade.
- Alto risco ao alterar diretamente o sistema de origem.

Ao mesmo tempo, o ecossistema corporativo moderno exige:

- APIs REST versionadas.
- Autenticação e autorização com OAuth2/JWT.
- Contratos documentados em OpenAPI.
- Logging estruturado.
- Correlação entre requisições.
- Observabilidade por métricas, logs e tracing.
- Tratamento padronizado de erros.
- Governança sobre exposição de dados sensíveis.

Portanto, a solução precisa permitir que parceiros externos consumam uma interface REST moderna sem depender diretamente do contrato SOAP legado.

---

## 3. Decisão

Será criado um microserviço REST denominado, inicialmente, **Biometria Core API**, responsável por expor o endpoint:

```http
GET /api/v1/biometria/{cpf}
```

Esse microserviço atuará como uma **fachada moderna** sobre o sistema legado SOAP.

A comunicação com o legado será feita por meio de um componente de integração dedicado, implementado como **SOAP Adapter**, aplicando os padrões:

- **Facade**
- **Adapter**
- **Anti-Corruption Layer**
- **Ports and Adapters**
- **Clean Architecture**

A API REST não deverá expor detalhes do contrato SOAP, nomes técnicos do legado, estruturas internas do Oracle ou qualquer detalhe de implementação do sistema de origem.

O microserviço REST será responsável por:

- Receber a requisição REST.
- Validar parâmetros de entrada.
- Acionar o caso de uso de consulta de biometria.
- Delegar a chamada ao adapter SOAP.
- Traduzir a resposta legada para um contrato REST moderno.
- Padronizar erros.
- Registrar logs técnicos.
- Propagar correlation ID.
- Preservar rastreabilidade da operação.

---

## 4. Justificativa

A decisão de criar uma API REST intermediária evita que parceiros externos consumam diretamente o serviço SOAP legado.

Essa abordagem reduz acoplamento e permite que o legado permaneça estável enquanto o ecossistema externo evolui. Em vez de forçar todos os consumidores a entenderem SOAP, XML, WSDL e estruturas legadas, o sistema oferece uma interface REST simples, versionada e documentada.

Além disso, a API REST pode incorporar requisitos corporativos modernos sem exigir alterações no legado, como:

- Segurança OAuth2/JWT.
- Observabilidade.
- Logs estruturados.
- Versionamento de contrato.
- Padronização de erro.
- Auditoria técnica.
- Controle de exposição de dados sensíveis.

Essa decisão também cria uma camada de proteção entre o domínio moderno e o domínio legado. Essa camada impede que conceitos, limitações e nomes internos do sistema antigo vazem para os contratos públicos.

---

## 5. Alternativas Consideradas

### 5.1 Expor o SOAP diretamente para parceiros externos

Essa alternativa foi descartada.

Embora pareça simples, ela cria vários problemas:

- Obriga parceiros externos a consumir SOAP.
- Expõe contrato legado diretamente.
- Aumenta o acoplamento entre consumidores e sistema antigo.
- Dificulta governança REST.
- Dificulta documentação via OpenAPI.
- Reduz flexibilidade para evolução futura.
- Pode expor detalhes internos indesejados.

Essa abordagem também dificulta a aplicação uniforme de padrões modernos como JWT, correlation ID, logs estruturados e tratamento REST padronizado de erros.

---

### 5.2 Alterar o sistema legado para expor REST diretamente

Essa alternativa também foi descartada para a POC.

Apesar de tecnicamente possível, alterar o legado aumenta risco operacional, especialmente quando se trata de sistemas críticos com dados sensíveis. A alteração direta exigiria entendimento profundo do sistema antigo, ciclo de homologação mais longo e maior risco de regressão.

Além disso, a proposta do case parte da premissa de que o dado está em Oracle e é acessado apenas por SOAP interno. Portanto, a solução deve respeitar essa restrição e propor modernização por integração, não por reescrita imediata.

---

### 5.3 Criar apenas um API Gateway chamando diretamente o SOAP

Essa alternativa foi considerada, mas não será adotada como solução principal.

O API Gateway deve atuar como ponto de entrada, roteamento e aplicação de políticas transversais. Porém, colocar transformação SOAP/REST, regra de negócio, validação e mapeamento de domínio diretamente no gateway causaria excesso de responsabilidade.

Essa abordagem deixaria o gateway mais complexo e dificultaria testes, evolução e manutenção.

O gateway deve continuar sendo uma camada de borda, enquanto o microserviço core concentra a lógica de integração e transformação.

---

### 5.4 Reescrever o domínio legado como microserviço pleno

Essa alternativa representa uma possível evolução futura, mas está fora do escopo inicial.

Uma reescrita completa exigiria migração de dados, redefinição de ownership, estratégia de sincronização, plano de coexistência, reconciliação e governança de golden source.

Para a POC, a melhor estratégia é modernizar a exposição do serviço sem deslocar imediatamente a origem oficial dos dados.

---

## 6. Consequências Positivas

A decisão traz os seguintes benefícios:

- Redução do acoplamento com o legado.
- Interface REST moderna para parceiros.
- Isolamento do contrato SOAP.
- Melhor governança de APIs.
- Possibilidade de versionamento independente.
- Facilidade de documentação com OpenAPI.
- Maior controle sobre segurança.
- Facilidade para logs e rastreabilidade.
- Melhor experiência para consumidores externos.
- Possibilidade de evolução gradual para arquitetura de microserviços.
- Separação clara de responsabilidades.
- Código mais testável.
- Menor risco operacional sobre o legado.

---

## 7. Consequências Negativas

A decisão também adiciona alguns custos e responsabilidades:

- Criação de mais um serviço na arquitetura.
- Necessidade de monitorar a nova camada REST.
- Possível aumento de latência pela intermediação.
- Necessidade de tratamento robusto para falhas SOAP.
- Necessidade de mapear erros legados para erros REST.
- Necessidade de garantir consistência na tradução de contratos.
- Mais um componente para deploy, operação e troubleshooting.

Esses impactos são considerados aceitáveis diante dos benefícios de governança, segurança e desacoplamento.

---

## 8. Trade-offs

A arquitetura favorece **desacoplamento, governança e evolução gradual** em troca de **maior complexidade operacional**.

A alternativa de expor SOAP diretamente seria mais simples no curto prazo, mas criaria dependência forte com o legado e dificultaria evolução futura.

A alternativa de reescrever o legado seria mais moderna no longo prazo, mas teria custo e risco maiores para o escopo de uma POC.

Portanto, a decisão escolhida equilibra pragmatismo e maturidade arquitetural.

---

## 9. Impacto na Arquitetura C4

Essa decisão impacta diretamente os diagramas C1, C2 e C3.

### C1 — System Context

No nível de contexto, os parceiros externos não conversam diretamente com o legado. Eles consomem uma API corporativa moderna.

### C2 — Container

No nível de containers, a solução terá pelo menos:

- API Gateway.
- Biometria Core API.
- Legacy SOAP Service.
- Banco H2 simulando Oracle.
- Keycloak como provedor OAuth2/JWT.

### C3 — Component

No nível de componentes, o Biometria Core API será dividido em:

- REST Controller.
- Application Use Case.
- Domain Model.
- SOAP Port.
- SOAP Adapter.
- DTO Mapper.
- Exception Handler.
- Logging/Tracing components.

---

## 10. Impacto na Segurança

A API REST será protegida na borda por um API Gateway integrado ao Keycloak.

A Biometria Core API deverá receber apenas chamadas autenticadas e autorizadas vindas do gateway ou de ambientes internos autorizados.

Os tokens JWT deverão carregar informações mínimas necessárias para autorização e rastreabilidade, como:

- subject;
- client_id;
- scopes;
- roles, se aplicável;
- issued_at;
- expiration.

A API não deverá registrar em logs dados sensíveis de biometria facial, imagens, payloads binários ou informações pessoais em excesso.

O CPF poderá ser mascarado nos logs, mantendo apenas elementos mínimos para troubleshooting quando necessário.

---

## 11. Impacto na Observabilidade

A camada REST permitirá adicionar observabilidade de forma mais consistente do que depender exclusivamente do legado.

A solução deverá registrar:

- Início da requisição.
- Correlation ID.
- CPF mascarado.
- Tempo de resposta da API.
- Tempo de resposta do SOAP.
- Status de sucesso ou erro.
- Tipo de falha.
- Serviço de destino chamado.
- Resultado técnico da integração.

A solução também deverá preparar pontos para métricas, como:

- Quantidade de consultas.
- Taxa de erro.
- Latência média.
- Latência do legado SOAP.
- Timeouts.
- Erros por tipo.
- Chamadas recusadas por autenticação.

---

## 12. Impacto no Tratamento de Erros

O contrato REST não deve propagar erros técnicos do SOAP diretamente.

Erros legados devem ser traduzidos para respostas REST padronizadas.

Exemplos:

| Cenário | Resposta REST sugerida |
|---|---|
| CPF inválido | 400 Bad Request |
| Biometria não encontrada | 404 Not Found |
| Falha no legado SOAP | 502 Bad Gateway |
| Timeout no legado | 504 Gateway Timeout |
| Token ausente ou inválido | 401 Unauthorized |
| Sem permissão | 403 Forbidden |
| Erro inesperado | 500 Internal Server Error |

O retorno deve seguir um padrão consistente, preferencialmente inspirado em Problem Details.

---

## 13. Impacto no Versionamento

O contrato REST será versionado por URI.

Versão inicial:

```http
/api/v1/biometria/{cpf}
```

Essa abordagem foi escolhida por ser simples, explícita e compatível com múltiplos tipos de consumidores.

A evolução futura poderá incluir:

```http
/api/v2/biometria/{cpf}
```

A versão v1 deverá permanecer estável enquanto houver consumidores ativos.

Mudanças incompatíveis devem ser introduzidas em nova versão.

---

## 14. Impacto na Clean Architecture

A decisão reforça a separação entre camadas.

A estrutura recomendada para o Biometria Core API é:

```text
controller
  recebe HTTP e transforma request em comando/consulta

application
  orquestra o caso de uso

domain
  representa conceitos do negócio

ports
  define contratos de saída

adapters
  implementa integração SOAP

infrastructure
  configura clientes, logs, exceptions e observabilidade
```

O domínio e os casos de uso não devem depender diretamente de SOAP, XML, WSDL ou detalhes do legado.

A dependência deve apontar para interfaces internas, permitindo testes unitários e substituição futura do adapter.

---

## 15. Impacto na Estratégia de Testes

A decisão permite testar o sistema em diferentes níveis:

### Testes unitários

- Validação de CPF.
- Use case de consulta de biometria.
- Mapeamento de resposta SOAP para REST.
- Tratamento de erros.

### Testes de integração

- Controller REST.
- Adapter SOAP usando mock/fake.
- Serialização e desserialização.
- Contrato OpenAPI.

### Testes de resiliência

- Timeout do legado.
- SOAP indisponível.
- Resposta inválida.
- Biometria inexistente.

---

## 16. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Latência elevada no SOAP | Alto | Timeout, métricas e futuro cache |
| Falha do legado | Alto | Tratamento 502/504 e observabilidade |
| Vazamento de dados sensíveis em logs | Alto | Mascaramento e política de logging |
| Acoplamento indevido ao contrato SOAP | Médio | Adapter e Anti-Corruption Layer |
| Crescimento excessivo do core | Médio | Separação por use cases e ports |
| Gateway com lógica de negócio | Médio | Manter gateway apenas como borda |

---

## 17. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- A API REST expuser o endpoint `/api/v1/biometria/{cpf}`.
- O contrato SOAP não for exposto aos consumidores REST.
- Existir um adapter dedicado para comunicação SOAP.
- O domínio não depender diretamente de classes SOAP.
- Os erros SOAP forem traduzidos para erros REST padronizados.
- O CPF for validado antes da chamada ao legado.
- Logs estruturados forem gerados nos principais pontos da integração.
- A documentação OpenAPI estiver disponível.
- Os diagramas C1, C2 e C3 refletirem a decisão.
- O README explicar a decisão arquitetural.

---

## 18. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-001-exposicao-legado-soap-via-rest.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Registrar a decisão no README principal.
- [ ] Refletir a decisão nos diagramas C1, C2 e C3.

### Código — Biometria Core API

- [ ] Criar endpoint `GET /api/v1/biometria/{cpf}`.
- [ ] Criar controller REST.
- [ ] Criar use case de consulta.
- [ ] Criar porta de saída para consulta no legado.
- [ ] Criar adapter SOAP.
- [ ] Criar mapper entre resposta SOAP e resposta REST.
- [ ] Criar tratamento global de exceções.
- [ ] Criar validação de CPF.
- [ ] Criar logs estruturados.
- [ ] Propagar correlation ID.

### Código — Legacy SOAP

- [ ] Criar serviço SOAP fictício.
- [ ] Criar base H2 simulando Oracle.
- [ ] Criar massa de dados fictícia.
- [ ] Retornar biometria fictícia por CPF.
- [ ] Simular cenários de erro.

### Testes

- [ ] Testar consulta com sucesso.
- [ ] Testar CPF inválido.
- [ ] Testar biometria não encontrada.
- [ ] Testar falha SOAP.
- [ ] Testar timeout ou indisponibilidade simulada.
- [ ] Testar mapeamento SOAP → REST.
- [ ] Testar contrato do endpoint REST.

---

## 19. Decisão Final

A solução adotará uma API REST moderna como fachada sobre o serviço SOAP legado, mantendo o legado isolado e protegido.

Essa decisão permite atender aos requisitos do case técnico com uma abordagem realista, evolutiva e alinhada a práticas corporativas de arquitetura de integração.

A API REST será a interface oficial para consumidores modernos, enquanto o SOAP permanecerá encapsulado atrás de um adapter, evitando vazamento de complexidade legada para parceiros externos.

---

## 20. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Criar ADR-002 sobre o uso de API Gateway.
2. Criar ADR-003 sobre Adapter e Anti-Corruption Layer.
3. Criar os diagramas C1, C2 e C3.
4. Criar a estrutura inicial do repositório.
5. Implementar o Legacy SOAP fake.
6. Implementar o Biometria Core API.
7. Implementar o Gateway.
8. Consolidar OpenAPI, README e testes.

