# POC Vivo - Integração Arquitetural

## Passo 1 — Fundação do Workspace e Escopo Arquitetural

## 1. Objetivo deste passo

Criar a base inicial da POC para o case técnico de Arquiteto de Integração da Vivo, mantendo o escopo alinhado ao desafio proposto:

- Expor dados de biometria facial de clientes por API REST.
- Integrar a API REST moderna com um serviço legado SOAP interno.
- Simular o legado Oracle utilizando H2.
- Aplicar padrões arquiteturais de integração, segurança, observabilidade e governança.
- Preparar a base para documentação arquitetural C4 Model nos níveis C1, C2 e C3.

Este passo ainda não deve implementar regras complexas nem diagramas finais. O foco é estabelecer a estrutura correta do projeto e as primeiras decisões arquiteturais.

---

## 2. Decisão inicial de arquitetura

A POC será composta por três aplicações principais:

```text
poc-vivo-integracao-arquitetural/

├── biometria-gateway/
├── biometria-core-api/
├── biometria-legacy-soap/
├── docs/
└── README.md
```

### 2.1 biometria-gateway

Responsável por representar a camada de entrada corporativa para parceiros externos.

Responsabilidades:

- Roteamento para a API de biometria.
- Validação de token JWT/OAuth2, preferencialmente via Keycloak.
- Propagação de correlationId.
- Logging básico de entrada e saída.
- Centralização futura de políticas de API, rate limit e versionamento.

Observação: o gateway não é tecnicamente obrigatório para a API REST funcionar, mas é arquiteturalmente recomendado no contexto da Vivo porque representa governança, segurança e exposição controlada para parceiros externos.

---

### 2.2 biometria-core-api

Responsável pela API REST moderna exposta ao ecossistema corporativo.

Endpoint principal:

```http
GET /api/v1/biometria/{cpf}
```

Responsabilidades:

- Receber requisições REST.
- Validar formato do CPF.
- Orquestrar a consulta de biometria.
- Chamar o serviço legado SOAP por meio de um adapter.
- Traduzir resposta SOAP para contrato REST.
- Aplicar tratamento de exceções padronizado.
- Produzir logs estruturados.
- Expor documentação OpenAPI/Swagger.

Padrões aplicados:

- Facade.
- Adapter.
- Anti-Corruption Layer.
- Clean Architecture.
- DTO Mapping.

---

### 2.3 biometria-legacy-soap

Responsável por simular o sistema legado interno.

Responsabilidades:

- Expor um serviço SOAP fictício.
- Simular acesso a dados armazenados em Oracle utilizando H2.
- Retornar dados fictícios de biometria facial.
- Representar o acoplamento legado entre SOAP e banco relacional.

Observação: o desafio menciona Oracle com imagens em tabela. Na POC, o H2 será usado para simular esse comportamento sem dependência externa pesada.

---

## 3. Sobre a integração com Receita Federal

A integração com a Receita Federal ficará fora da primeira versão da POC.

Justificativa:

- O desafio central é integração entre API REST moderna e legado SOAP.
- Inserir uma integração externa adicional pode parecer fuga de escopo.
- Validação real de CPF envolve restrições legais, privacidade, disponibilidade de API e governança de dados.
- Para a POC, basta validar o formato do CPF e simular a existência do cliente no legado.

A validação em órgão externo pode ser mencionada na documentação como evolução futura, caso exista necessidade de enriquecimento cadastral ou validação antifraude.

---

## 4. Estrutura inicial de documentação

Criar a seguinte estrutura dentro de `docs/`:

```text
docs/

├── architecture/
│   ├── DAS.md
│   ├── c4-c1-context.md
│   ├── c4-c2-container.md
│   ├── c4-c3-component.md
│   └── decisions/
│       ├── ADR-001-api-gateway.md
│       ├── ADR-002-soap-adapter.md
│       └── ADR-003-h2-as-oracle-simulation.md
│
├── openapi/
│   └── biometria-api-v1.yaml
│
└── diagrams/
    ├── c1-context.mmd
    ├── c2-container.mmd
    └── c3-component.mmd
```

---

## 5. Primeiro backlog técnico

### Fundação

- Criar repositório ou workspace local `poc-vivo-integracao-arquitetural`.
- Criar os três módulos/aplicações:
  - `biometria-gateway`
  - `biometria-core-api`
  - `biometria-legacy-soap`
- Criar diretório `docs` com a estrutura arquitetural inicial.
- Criar `README.md` raiz explicando a proposta da POC.

### Core API

- Criar endpoint REST:

```http
GET /api/v1/biometria/{cpf}
```

- Criar camadas iniciais:

```text
controller
application/usecase
domain
ports
adapters/soap
config
exception
observability
```

### Legacy SOAP

- Criar serviço SOAP fictício para consulta de biometria por CPF.
- Criar base H2 com dados fictícios.
- Simular imagem facial como Base64 ou texto placeholder.

### Gateway

- Criar gateway com rota para `biometria-core-api`.
- Preparar configuração para OAuth2 Resource Server/JWT.
- Preparar integração futura com Keycloak.

---

## 6. Decisões arquiteturais iniciais

### Decisão 1 — Utilizar API Gateway

O gateway será usado como ponto único de entrada para parceiros externos, centralizando autenticação, autorização, roteamento, logging, correlationId e futuras políticas de governança.

### Decisão 2 — Não expor o legado diretamente

O serviço SOAP legado não será exposto para parceiros externos. Ele será acessado apenas pelo `biometria-core-api`, por meio de um adapter.

### Decisão 3 — Aplicar Anti-Corruption Layer

A API moderna não deve herdar diretamente o contrato SOAP legado. O adapter será responsável por traduzir o modelo legado para um modelo REST limpo.

### Decisão 4 — Simular Oracle com H2

A POC usará H2 para reduzir complexidade operacional, mantendo a representação conceitual de persistência relacional do legado.

### Decisão 5 — Manter Receita Federal fora do escopo inicial

A validação externa de CPF será tratada como evolução futura, não como parte da POC principal.

---

## 7. Frase de defesa arquitetural

> O gateway não é tecnicamente necessário para expor a API REST, mas é arquiteturalmente recomendado porque centraliza segurança, governança, roteamento, versionamento e políticas de acesso para parceiros externos. O microserviço core permanece desacoplado e responsável apenas pela lógica de biometria e integração com o legado SOAP.

---

## 8. Próximo passo

Após criar o workspace, o próximo passo será definir o desenho C4 inicial em texto antes de gerar os diagramas:

1. C1 — System Context.
2. C2 — Containers.
3. C3 — Components do `biometria-core-api`.

Somente depois disso os diagramas devem ser gerados em Mermaid, Draw.io ou Structurizr DSL.
