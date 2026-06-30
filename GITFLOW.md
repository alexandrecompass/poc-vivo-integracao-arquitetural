# GITFLOW — Estratégia de Branches e Commits

> **Projeto:** POC Vivo – Integração Arquitetural
> Convenções obrigatórias para todo código commitado neste repositório.

---

## 1. Branches Principais

| Branch | Propósito | Proteção |
|---|---|---|
| `main` | Código estável, entregável, revisado | Merge apenas via PR |
| `develop` | Integração contínua dos módulos | Merge via PR ou direto (POC) |

---

## 2. Branches de Trabalho

### Nomenclatura

```
<tipo>/<escopo>/<descricao-curta>
```

Separador: `/` entre tipo e escopo, `-` entre palavras da descrição.

### Tipos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade ou módulo |
| `fix` | Correção de bug |
| `docs` | Apenas documentação |
| `test` | Apenas testes |
| `refactor` | Refatoração sem mudança de comportamento |
| `chore` | Configuração, build, CI, dependências |
| `style` | Formatação, checkstyle (sem mudança lógica) |

### Escopos

| Escopo | Módulo |
|---|---|
| `legacy-soap` | biometria-legacy-soap |
| `core-api` | biometria-core-api |
| `gateway` | biometria-gateway |
| `docker` | docker-compose, Keycloak config |
| `docs` | pasta docs/ |
| `root` | pom.xml raiz, .gitignore, AGENTS.md |

### Exemplos de branches

```
feat/legacy-soap/endpoint-soap-biometria
feat/legacy-soap/seed-cpfs-validos
feat/core-api/use-case-consultar-biometria
feat/core-api/endpoint-validar-cpf
feat/core-api/adapter-soap
feat/gateway/rotas-e-filtros
feat/gateway/validacao-jwt-keycloak
feat/docker/docker-compose-completo
test/core-api/testes-unitarios-use-cases
test/legacy-soap/testes-integracao-endpoint-soap
fix/core-api/mascaramento-cpf-log
docs/root/atualizar-readme
chore/root/configurar-pom-raiz
```

---

## 3. Fluxo de Trabalho por Rodada

```
main
 └── develop
       ├── feat/legacy-soap/...    ← Rodada 1
       ├── feat/core-api/...       ← Rodada 2
       └── feat/gateway/...        ← Rodada 3
```

### Rodada 1 — biometria-legacy-soap

```bash
git checkout develop
git checkout -b feat/legacy-soap/implementacao-inicial
# ... implementar ...
git add biometria-legacy-soap/
git commit -m "feat(legacy-soap): ..."
git push origin feat/legacy-soap/implementacao-inicial
# PR: feat/legacy-soap/implementacao-inicial → develop
```

### Rodada 2 — biometria-core-api

```bash
git checkout develop
git pull origin develop
git checkout -b feat/core-api/implementacao-inicial
# ... implementar ...
git add biometria-core-api/
git commit -m "feat(core-api): ..."
git push origin feat/core-api/implementacao-inicial
# PR: feat/core-api/implementacao-inicial → develop
```

### Rodada 3 — biometria-gateway

```bash
git checkout develop
git pull origin develop
git checkout -b feat/gateway/implementacao-inicial
# ... implementar ...
git add biometria-gateway/
git commit -m "feat(gateway): ..."
git push origin feat/gateway/implementacao-inicial
# PR: feat/gateway/implementacao-inicial → develop
```

### Release final

```bash
git checkout main
git merge --no-ff develop -m "release: poc-vivo v1.0.0"
git tag -a v1.0.0 -m "POC Vivo Integração Arquitetural v1.0.0"
git push origin main --tags
```

---

## 4. Formato de Commit (Conventional Commits)

```
<tipo>(<escopo>): <descrição imperativa em minúsculas>

[corpo opcional — o quê e por quê, não como]

[rodapé opcional — referências, breaking changes]
```

### Regras

- Linha de assunto: máximo 72 caracteres
- Tipo e escopo: minúsculos
- Descrição: imperativo, sem ponto final, sem maiúscula inicial
- Corpo: opcional, separado por linha em branco
- `BREAKING CHANGE:` no rodapé quando houver incompatibilidade

### Exemplos corretos

```
feat(legacy-soap): implementar endpoint SOAP de consulta de biometria
```

```
feat(legacy-soap): adicionar seed com CPFs válidos no H2

Popula a tabela BIOMETRIA com 10 CPFs que passam no algoritmo de
validação do dígito verificador. Necessário para testes locais e
demonstração da integração SOAP end-to-end.
```

```
feat(core-api): implementar use case ConsultarBiometriaUseCase

- Porta BiometriaLegadoPort como dependência (inversão de controle)
- Adapter SOAP implementa a porta
- SoapFaultTranslator converte falhas SOAP para exceções internas
- GlobalExceptionHandler mapeia exceções para HTTP padronizado
```

```
feat(core-api): adicionar endpoint público de validação de CPF

GET /api/v1/cpf/{cpf}/validar
Retorna HTTP 200 com valido: true|false.
Endpoint não requer autenticação (permitAll).
```

```
feat(gateway): configurar validação JWT com Keycloak e filtros

- JWKS URL: http://localhost:8081/realms/vivo-poc/protocol/openid-connect/certs
- CorrelationIdGatewayFilter: gera UUID se X-Correlation-Id ausente
- LoggingGatewayFilter: loga método, path, status e duração
```

```
chore(root): configurar pom.xml raiz com módulos e dependências gerenciadas
```

```
test(core-api): adicionar testes unitários de domínio e use cases

- CpfTest: 10 CPFs válidos do seed + 5 inválidos
- ConsultarBiometriaUseCaseTest: mock de BiometriaLegadoPort
- ValidarCpfUseCaseTest
- SoapFaultTranslatorTest
```

```
docs(root): adicionar README com instruções de execução local
```

### Exemplos errados (não usar)

```
# Errado: maiúscula, sem escopo, sem verbo imperativo
Adicionando endpoint SOAP

# Errado: escopo genérico demais
fix(vivo): corrigindo bug

# Errado: linha de assunto muito longa
feat(core-api): implementar o endpoint REST de consulta de biometria facial por CPF com integração SOAP e mascaramento

# Errado: sem tipo
seed com cpfs validos
```

---

## 5. Commits por Módulo — Sequência Esperada

### biometria-legacy-soap

```
chore(legacy-soap): configurar pom.xml com spring-ws e h2
feat(legacy-soap): criar entidade Biometria e repositório JPA
feat(legacy-soap): implementar BiometriaService com consulta por CPF
feat(legacy-soap): implementar endpoint SOAP ConsultarBiometria
feat(legacy-soap): adicionar seed com 10 CPFs válidos no H2
feat(legacy-soap): configurar H2 console e application.yml
test(legacy-soap): adicionar testes unitários e de integração SOAP
```

### biometria-core-api

```
chore(core-api): configurar pom.xml com spring-boot, spring-ws-client, springdoc
feat(core-api): criar value object Cpf com validação de dígito verificador
feat(core-api): criar modelo de domínio Biometria e enums
feat(core-api): implementar BiometriaLegadoPort e SoapBiometriaLegadoAdapter
feat(core-api): implementar SoapBiometriaClient com WebServiceTemplate
feat(core-api): implementar SoapBiometriaMapper e SoapFaultTranslator
feat(core-api): implementar ConsultarBiometriaUseCase
feat(core-api): implementar ValidarCpfUseCase
feat(core-api): implementar BiometriaController com GET /api/v1/biometria/{cpf}
feat(core-api): implementar CpfValidadorController com GET /api/v1/cpf/{cpf}/validar
feat(core-api): implementar GlobalExceptionHandler
feat(core-api): implementar CorrelationIdFilter e RequestLoggingFilter
feat(core-api): configurar SecurityConfig como OAuth2 Resource Server
feat(core-api): configurar logback-spring.xml com JSON estruturado
test(core-api): adicionar testes unitários de domínio, use cases e adapters
test(core-api): adicionar testes de integração dos controllers
```

### biometria-gateway

```
chore(gateway): configurar pom.xml com spring-cloud-gateway e oauth2-client
feat(gateway): configurar rotas para biometria-core-api
feat(gateway): implementar CorrelationIdGatewayFilter
feat(gateway): implementar LoggingGatewayFilter
feat(gateway): configurar SecurityConfig com validação JWT Keycloak
feat(gateway): configurar application.yml com JWKS e rotas
test(gateway): adicionar testes de filtros e rotas
```

### Infraestrutura e finalização

```
chore(docker): adicionar docker-compose com keycloak, gateway, core-api e legacy-soap
chore(docker): adicionar realm-export.json do Keycloak com client pré-configurado
chore(root): configurar pom.xml raiz multi-módulo
docs(root): adicionar README com arquitetura, pré-requisitos e como executar
chore(root): adicionar scripts/test-api.sh para smoke test
```

---

## 6. Tags de Release

Formato: `v<major>.<minor>.<patch>`

| Tag | Quando criar |
|---|---|
| `v0.1.0` | Após `biometria-legacy-soap` funcional |
| `v0.2.0` | Após `biometria-core-api` integrado com SOAP |
| `v0.3.0` | Após `biometria-gateway` integrado com Keycloak |
| `v1.0.0` | Stack completa, testada, documentada |

---

## 7. Regras de Merge

- PRs para `develop`: squash merge se a branch tiver commits de WIP, merge commit se os commits forem limpos
- PRs para `main`: **sempre merge commit** (`--no-ff`) para preservar histórico
- Nunca fazer force push em `develop` ou `main`
- Branches de feature são deletadas após o merge

---

## 8. Branch de Diagramas

Após a stack completa e integrada, gerar os diagramas C4 em uma branch dedicada:

```
docs/diagrams/gerar-diagramas-draw-io
```

Commit esperado:

```
docs(diagrams): gerar diagramas C4 em Draw.io a partir dos .mmd e .dsl
```
