# ADR-006 — Estratégia de Versionamento de APIs

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Governança de APIs, evolução de contratos e compatibilidade com parceiros externos

---

## 1. Contexto

A POC Vivo — Integração Arquitetural tem como objetivo expor informações de biometria facial de clientes, originalmente disponíveis apenas por meio de um serviço SOAP legado, através de uma API REST moderna.

A API será consumida por parceiros externos ou sistemas corporativos autorizados. Esse tipo de consumidor exige estabilidade contratual, previsibilidade de mudanças e clareza sobre evolução.

O endpoint inicial previsto para a POC é:

```http
GET /api/v1/biometria/{cpf}
```

Como a solução envolve parceiros externos, governança corporativa e dados sensíveis, mudanças incompatíveis no contrato da API não podem ser feitas de forma silenciosa.

Esta ADR define a estratégia de versionamento da API REST.

---

## 2. Problema

APIs evoluem.

Com o tempo, podem surgir necessidades como:

- Adicionar novos campos.
- Remover campos antigos.
- Alterar formato de resposta.
- Alterar semântica de erros.
- Alterar nomes de atributos.
- Alterar estrutura de autenticação/autorização.
- Alterar paginação, filtros ou headers.
- Separar retorno de imagem facial em outro endpoint.
- Substituir Base64 por URL temporária.
- Incluir metadados de auditoria.
- Adaptar a API a novos parceiros.

Sem uma estratégia clara de versionamento, mudanças podem quebrar consumidores existentes.

Em um cenário corporativo, isso pode gerar:

- Falhas em integrações de parceiros.
- Incidentes produtivos.
- Retrabalho em múltiplos times.
- Dificuldade de governança.
- Perda de confiança na API.
- Necessidade de rollbacks emergenciais.
- Contratos paralelos não documentados.

Portanto, a API precisa ter uma política explícita para evolução compatível e incompatível.

---

## 3. Decisão

A API REST adotará **versionamento por URI**, iniciando pela versão `v1`.

Endpoint inicial:

```http
GET /api/v1/biometria/{cpf}
```

Mudanças incompatíveis deverão ser introduzidas em nova versão de URI, por exemplo:

```http
GET /api/v2/biometria/{cpf}
```

Mudanças compatíveis poderão ser feitas dentro da mesma versão, desde que não quebrem consumidores existentes.

A versão da API deverá estar refletida em:

- URI pública.
- OpenAPI.
- README.
- Documentação de arquitetura.
- Configuração do API Gateway.
- Diagramas C4, quando relevante.

---

## 4. Justificativa

O versionamento por URI foi escolhido por ser simples, explícito e de fácil entendimento para consumidores externos.

Em APIs corporativas consumidas por parceiros, clareza operacional costuma ser mais importante do que pureza conceitual.

Ao visualizar a chamada:

```http
/api/v1/biometria/{cpf}
```

o consumidor sabe imediatamente qual versão está usando.

Essa estratégia também facilita:

- Roteamento no API Gateway.
- Documentação OpenAPI por versão.
- Depreciação planejada.
- Testes automatizados por contrato.
- Suporte simultâneo a versões.
- Comunicação com parceiros.
- Monitoramento de uso por versão.

Embora existam outras abordagens, como versionamento por header ou media type, o versionamento por URI é mais direto para uma POC e mais fácil de defender em uma apresentação técnica.

---

## 5. Escopo da Versão v1

A versão `v1` terá o seguinte escopo inicial:

```http
GET /api/v1/biometria/{cpf}
```

Responsabilidade:

- Consultar biometria facial fictícia por CPF.
- Retornar status de disponibilidade.
- Retornar imagem fictícia em Base64, apenas para fins de POC.
- Indicar origem da informação.
- Retornar data/hora da consulta.
- Padronizar erros REST.

Exemplo conceitual de resposta v1:

```json
{
  "cpf": "***.***.***-09",
  "biometriaDisponivel": true,
  "imagemBase64": "valor-ficticio-base64",
  "origem": "LEGADO_SOAP",
  "dataConsulta": "2026-06-30T12:00:00Z"
}
```

Observação importante: em ambiente real, retornar imagem facial em Base64 diretamente pode não ser a melhor estratégia. Uma evolução futura poderia substituir esse campo por uma URL temporária, tokenizada e auditável.

---

## 6. Mudanças Compatíveis

São consideradas mudanças compatíveis dentro da mesma versão:

- Adicionar campos opcionais na resposta.
- Adicionar headers informativos.
- Adicionar novos códigos de erro sem alterar os existentes.
- Melhorar mensagens sem alterar semântica.
- Adicionar novos endpoints sem alterar os existentes.
- Tornar logs e métricas mais completos.
- Melhorar performance sem mudar contrato.
- Corrigir bug sem alterar comportamento esperado.
- Adicionar exemplos na documentação.
- Adicionar novos scopes para novos endpoints, sem mudar o endpoint atual.

Exemplo de mudança compatível:

```json
{
  "cpf": "***.***.***-09",
  "biometriaDisponivel": true,
  "imagemBase64": "valor-ficticio-base64",
  "origem": "LEGADO_SOAP",
  "dataConsulta": "2026-06-30T12:00:00Z",
  "correlationId": "demo-001"
}
```

Desde que consumidores ignorem campos desconhecidos, essa mudança tende a ser segura.

---

## 7. Mudanças Incompatíveis

São consideradas mudanças incompatíveis e exigem nova versão:

- Remover campo existente.
- Renomear campo existente.
- Alterar tipo de campo.
- Alterar significado de um campo.
- Alterar estrutura da resposta.
- Alterar formato de erro de forma incompatível.
- Alterar path do endpoint.
- Alterar método HTTP.
- Tornar obrigatório um campo antes opcional.
- Alterar autenticação de forma incompatível.
- Alterar escopo exigido do endpoint atual sem transição.
- Substituir Base64 por URL sem manter compatibilidade.
- Alterar contrato OpenAPI quebrando clients existentes.

Exemplo de mudança incompatível:

v1:

```json
{
  "imagemBase64": "valor-ficticio-base64"
}
```

v2:

```json
{
  "imagem": {
    "tipo": "URL_TEMPORARIA",
    "url": "https://..."
  }
}
```

Essa mudança exigiria nova versão.

---

## 8. Alternativas Consideradas

### 8.1 Versionamento por Header

Exemplo:

```http
GET /api/biometria/{cpf}
X-API-Version: 1
```

Essa alternativa foi considerada.

Vantagens:

- Mantém URI mais limpa.
- Permite versionamento fora do path.
- Pode ser elegante em APIs internas.

Desvantagens:

- Menos explícita para consumidores.
- Mais difícil de testar manualmente.
- Pode ser esquecida em chamadas.
- Exige configuração mais cuidadosa no gateway.
- Dificulta leitura rápida de logs e rotas.
- Pode ser menos amigável para parceiros externos.

Foi descartada para a POC por ser menos clara.

---

### 8.2 Versionamento por Media Type

Exemplo:

```http
Accept: application/vnd.vivo.biometria.v1+json
```

Essa alternativa é comum em arquiteturas REST mais rigorosas.

Vantagens:

- Preserva URI.
- Permite negociação de conteúdo.
- É conceitualmente elegante.

Desvantagens:

- Mais complexa para parceiros.
- Menos intuitiva.
- Mais difícil de demonstrar em POC.
- Requer maior disciplina de client.
- Mais difícil de configurar e explicar rapidamente.

Foi descartada para manter simplicidade e clareza.

---

### 8.3 Sem versionamento explícito

Essa alternativa foi descartada.

Mesmo em uma POC, a ausência de versionamento enfraqueceria a proposta de governança.

Como o case menciona explicitamente versionamento e padrões corporativos da Vivo, a API deve nascer versionada.

---

### 8.4 Versionamento por query parameter

Exemplo:

```http
GET /api/biometria/{cpf}?version=1
```

Essa alternativa foi descartada.

Embora seja simples, mistura identificação de versão com parâmetros de consulta e pode gerar ambiguidade em caches, gateways, documentação e roteamento.

---

## 9. Consequências Positivas

A decisão traz os seguintes benefícios:

- Clareza para consumidores.
- Facilidade de documentação.
- Facilidade de roteamento no gateway.
- Compatibilidade com OpenAPI.
- Facilidade de testes manuais.
- Facilidade de suporte simultâneo a versões.
- Melhor governança de mudanças.
- Redução de risco de quebra de parceiros.
- Aderência ao enunciado do case.
- Simplicidade para apresentação técnica.
- Melhor rastreabilidade por versão nos logs.

---

## 10. Consequências Negativas

A decisão também possui custos:

- A versão aparece na URI.
- Pode haver duplicação de rotas no futuro.
- Pode haver múltiplas versões ativas.
- Exige política de depreciação.
- Exige disciplina para não criar `v2` sem necessidade.
- Pode gerar manutenção paralela temporária.

Esses custos são aceitáveis para APIs corporativas expostas a parceiros.

---

## 11. Trade-offs

A solução escolhe simplicidade e clareza operacional em troca de menor pureza REST.

Versionamento por URI pode ser considerado menos elegante por alguns puristas REST, mas em ambientes corporativos com múltiplos times e parceiros, ele costuma ser pragmático e fácil de governar.

Para o objetivo da POC, essa é a escolha mais adequada.

---

## 12. Estratégia de Depreciação

Quando uma nova versão for criada, a versão anterior não deve ser removida imediatamente.

Deve existir uma política de depreciação.

Exemplo de ciclo:

```text
1. Publicar v2.
2. Manter v1 ativa.
3. Comunicar consumidores.
4. Monitorar uso da v1.
5. Definir prazo de descontinuação.
6. Encerrar v1 após migração.
```

A API pode retornar headers informativos no futuro:

```http
Deprecation: true
Sunset: Wed, 30 Jun 2027 23:59:59 GMT
Link: </api/v2/biometria/{cpf}>; rel="successor-version"
```

Para a POC, essa política será documentada, mas não precisa ser implementada.

---

## 13. Estratégia de Compatibilidade

A versão `v1` deverá permanecer estável.

Regras:

1. Não remover campos públicos.
2. Não renomear campos públicos.
3. Não alterar tipos de campos.
4. Não alterar códigos HTTP sem justificativa.
5. Não alterar formato de erro de forma incompatível.
6. Não alterar path sem nova versão.
7. Não alterar método HTTP sem nova versão.
8. Não alterar escopo obrigatório sem transição.
9. Adições devem ser preferencialmente opcionais.
10. Documentação OpenAPI deve refletir o contrato real.

---

## 14. Estratégia de OpenAPI

Cada versão da API deve possuir documentação OpenAPI compatível.

Para a POC:

```text
docs/api/openapi-v1.yaml
```

ou, se gerado automaticamente:

```http
/v3/api-docs
/swagger-ui.html
```

A documentação deve indicar claramente:

- Versão da API.
- Endpoint.
- Parâmetros.
- Segurança.
- Respostas de sucesso.
- Respostas de erro.
- Exemplos.
- Campos sensíveis.
- Observações sobre dados fictícios.

---

## 15. Estratégia no API Gateway

O gateway deverá rotear chamadas considerando o prefixo versionado.

Exemplo:

```text
/api/v1/biometria/** → biometria-core-api
```

No futuro:

```text
/api/v1/biometria/** → biometria-core-api-v1
/api/v2/biometria/** → biometria-core-api-v2
```

A versão no path facilita roteamento, métricas e logs.

---

## 16. Estratégia no Core API

A Biometria Core API poderá organizar controllers por versão.

Exemplo:

```text
api
  v1
    BiometriaV1Controller
    dto
      BiometriaV1Response
```

Ou, para uma POC pequena:

```text
api
  BiometriaController
```

com mapeamento:

```java
@RequestMapping("/api/v1/biometria")
```

Como a POC tem apenas uma versão, a estrutura pode permanecer simples, desde que o path seja versionado.

---

## 17. Estratégia de Erros Versionados

O formato de erro também faz parte do contrato.

Exemplo conceitual:

```json
{
  "timestamp": "2026-06-30T12:00:00Z",
  "status": 404,
  "error": "BIOMETRIA_NAO_ENCONTRADA",
  "message": "Biometria não encontrada para o CPF informado.",
  "correlationId": "demo-001"
}
```

Mudanças incompatíveis nesse formato exigem nova versão ou transição cuidadosa.

---

## 18. Impacto na Segurança

O versionamento não substitui autenticação/autorização.

Cada versão poderá exigir escopos específicos.

Para v1:

```text
biometria:read
```

No futuro, v2 poderia exigir o mesmo escopo ou um escopo novo:

```text
biometria.v2:read
```

A decisão sobre escopos por versão deve considerar compatibilidade, governança e granularidade.

Para a POC, será usado apenas `biometria:read`.

---

## 19. Impacto na Observabilidade

A versão da API deve aparecer nos logs e métricas.

Campos recomendados:

```text
apiVersion=v1
endpoint=/api/v1/biometria/{cpf}
route=biometria-v1
```

Isso permite responder perguntas como:

- Quantas chamadas usam v1?
- Qual versão tem maior taxa de erro?
- Qual versão tem maior latência?
- Algum parceiro ainda usa versão depreciada?

Na POC, isso pode ser registrado no path e nos logs.

---

## 20. Impacto na Arquitetura C4

### 20.1 C1 — System Context

No C1, o versionamento não precisa aparecer com detalhe.

Pode ser mencionado na descrição da relação:

```text
Parceiro Externo consome API REST versionada da Vivo.
```

### 20.2 C2 — Container Diagram

No C2, a rota versionada pode aparecer no relacionamento:

```text
Parceiro Externo → API Gateway: HTTPS REST /api/v1/biometria/{cpf}
```

### 20.3 C3 — Component Diagram

No C3, o controller pode evidenciar a versão:

```text
BiometriaV1Controller
```

Ou o relacionamento pode indicar:

```text
GET /api/v1/biometria/{cpf}
```

---

## 21. Impacto no README

O README deverá explicar:

- A versão inicial da API.
- O endpoint disponível.
- Como chamar a v1.
- Como interpretar mudanças compatíveis.
- Como futuras versões serão tratadas.
- Onde encontrar OpenAPI.
- Que mudanças incompatíveis exigirão nova versão.

Exemplo:

```bash
curl -X GET http://localhost:8080/api/v1/biometria/12345678909 \
  -H "Authorization: Bearer <token>" \
  -H "X-Correlation-Id: demo-001"
```

---

## 22. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- O endpoint público iniciar com `/api/v1`.
- O gateway rotear chamadas versionadas.
- A OpenAPI indicar versão `v1`.
- O README documentar o versionamento.
- O DAS mencionar a estratégia de versionamento.
- Os diagramas C2/C3 indicarem o endpoint versionado.
- Mudanças incompatíveis forem documentadas como exigindo nova versão.
- O contrato de erro for tratado como parte da versão.
- Os testes cobrirem o endpoint versionado.
- Não existir endpoint público sem versão para biometria.

---

## 23. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-006-versionamento-apis.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Atualizar README com estratégia de versionamento.
- [ ] Criar ou planejar `docs/api/openapi-v1.yaml`.
- [ ] Documentar endpoint `/api/v1/biometria/{cpf}`.
- [ ] Documentar mudanças compatíveis.
- [ ] Documentar mudanças incompatíveis.
- [ ] Documentar política de depreciação.
- [ ] Refletir a versão nos diagramas C2 e C3.

### Código — Gateway

- [ ] Configurar rota `/api/v1/biometria/**`.
- [ ] Garantir que não exista rota pública sem versão.
- [ ] Registrar path versionado nos logs.
- [ ] Preparar configuração para versões futuras.

### Código — Core API

- [ ] Expor controller com path `/api/v1/biometria`.
- [ ] Criar endpoint `GET /{cpf}`.
- [ ] Garantir que DTOs representem contrato v1.
- [ ] Garantir que erros façam parte do contrato v1.
- [ ] Documentar endpoint via OpenAPI/Swagger.
- [ ] Testar endpoint versionado.

### Testes

- [ ] Testar `GET /api/v1/biometria/{cpf}` com sucesso.
- [ ] Testar que path sem versão não é aceito.
- [ ] Testar resposta de erro v1.
- [ ] Testar OpenAPI gerada para v1.
- [ ] Testar roteamento versionado no gateway.
- [ ] Testar logs contendo path versionado.

---

## 24. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Criar endpoint sem versão | Médio | Padronizar controller e gateway |
| Quebrar contrato v1 sem nova versão | Alto | OpenAPI e testes de contrato |
| Criar v2 sem necessidade | Médio | Política clara de mudanças incompatíveis |
| Manter muitas versões ativas | Médio | Política de depreciação |
| Documentação divergente do código | Alto | Gerar OpenAPI e revisar README |
| Erro não tratado como contrato | Médio | Padronizar resposta de erro |

---

## 25. Decisão Final

A API REST da POC adotará versionamento por URI.

A versão inicial será `v1`, exposta pelo endpoint:

```http
GET /api/v1/biometria/{cpf}
```

Mudanças incompatíveis deverão ser introduzidas em uma nova versão de API.

Essa decisão favorece clareza, governança, compatibilidade com parceiros externos e simplicidade operacional.

---

## 26. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Atualizar o DAS consolidando ADR-001 a ADR-006.
2. Criar os diagramas C4:
   - C1 — System Context.
   - C2 — Container.
   - C3 — Component.
3. Criar a estrutura inicial do repositório.
4. Criar os módulos:
   - `biometria-gateway`.
   - `biometria-core-api`.
   - `biometria-legacy-soap`.
5. Criar OpenAPI v1.
6. Iniciar implementação pelo serviço SOAP fake.
