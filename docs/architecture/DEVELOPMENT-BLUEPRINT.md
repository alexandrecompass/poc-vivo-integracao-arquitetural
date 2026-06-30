# DEVELOPMENT-BLUEPRINT

> **Projeto:** POC Vivo – Integração Arquitetural  
> **Documento:** Development Blueprint  
> **Versão:** 1.0  
> **Status:** Aprovado para Implementação

---

# 1. Objetivo

Este documento estabelece o contrato entre a arquitetura da solução e sua implementação.

Enquanto o Documento de Arquitetura da Solução (DAS) descreve **o que** será construído e as ADRs explicam **por que** determinadas decisões foram tomadas, este Blueprint define **como** a solução deverá ser implementada.

Seu objetivo é garantir que qualquer desenvolvedor implemente a arquitetura preservando as decisões arquiteturais, evitando interpretações individuais que comprometam a consistência do projeto.

Este documento deverá ser considerado normativo durante toda a implementação da POC.

---

# 2. Ordem de Precedência

Em caso de conflito entre documentos, deve ser seguida a seguinte hierarquia:

1. Documento de Arquitetura da Solução (DAS)
2. ADRs (Architecture Decision Records)
3. Development Blueprint
4. Diagramas C4
5. OpenAPI
6. README
7. Código-fonte

Nenhuma implementação deverá contradizer uma ADR sem que uma nova ADR seja criada.

---

# 3. Objetivos de Implementação

A implementação deverá priorizar:

- Clareza.
- Legibilidade.
- Baixo acoplamento.
- Alta coesão.
- Facilidade de testes.
- Facilidade de evolução.
- Isolamento do legado.
- Observabilidade.
- Segurança.
- Simplicidade.

O objetivo da POC não é construir uma plataforma completa, mas demonstrar decisões arquiteturais sólidas.

---

# 4. Estrutura do Repositório

```text
poc-vivo-integracao-arquitetural/
├── docs/
├── docker/
├── scripts/
├── biometria-gateway/
├── biometria-core-api/
├── biometria-legacy-soap/
├── pom.xml
└── README.md
```

Cada módulo deverá possuir responsabilidade única.

---

# 5. Estrutura Maven

Projeto multimódulo.

Módulos:

- biometria-gateway
- biometria-core-api
- biometria-legacy-soap

O pom.xml raiz concentrará:

- gerenciamento de versões;
- plugins comuns;
- JaCoCo;
- Checkstyle/Spotless (se adotados);
- BOMs.

---

# 6. Responsabilidades dos Módulos

## API Gateway

Responsável apenas por:

- autenticação;
- autorização;
- roteamento;
- correlation ID;
- logging de borda.

Nunca deverá conter regra de negócio.

---

## Biometria Core API

Responsável por:

- caso de uso;
- integração com legado;
- contrato REST;
- tratamento de erros;
- observabilidade.

Todo conhecimento funcional deve residir neste módulo.

---

## Legacy SOAP

Responsável por simular o sistema legado.

Não deverá conter código compartilhado com o Core.

---

# 7. Organização de Pacotes

A estrutura recomendada é:

```text
api/
application/
domain/
port/
adapter/
infrastructure/
```

Não criar classes diretamente no pacote raiz.

Cada pacote deve representar uma responsabilidade arquitetural.

---

# 8. Regras de Clean Architecture

- Controllers não possuem regra de negócio.
- Use Cases não conhecem HTTP.
- Use Cases não conhecem SOAP.
- Domain não conhece Spring.
- Adapter conhece infraestrutura.
- Infrastructure depende da aplicação, nunca o contrário.

---

# 9. Convenções de Nome

Controllers:
- *Controller

Casos de uso:
- *UseCase

Ports:
- *Port

Adapters:
- *Adapter

Clients:
- *Client

Mappers:
- *Mapper

Exception Handlers:
- *ExceptionHandler

DTOs:
- *Request
- *Response

Filtros:
- *Filter

---

# 10. Estratégia REST

Todos os endpoints públicos:

```text
/api/v1/*
```

Não criar endpoints sem versionamento.

OpenAPI deverá ser mantida sincronizada com a implementação.

---

# 11. Estratégia SOAP

Toda integração SOAP deverá passar por:

UseCase
→ Port
→ Adapter
→ SOAP Client

Nunca chamar SOAP diretamente do Controller.

---

# 12. Tratamento de Erros

Toda exceção deverá ser traduzida para um erro REST padronizado.

Nunca retornar:

- stack trace;
- SOAP Fault;
- mensagens técnicas do Oracle;
- mensagens internas do Spring.

---

# 13. Logging

Todos os logs deverão ser estruturados.

Campos mínimos:

- correlationId
- service
- event
- status
- duration

Nunca registrar:

- JWT completo;
- imagem em Base64;
- CPF completo;
- client secret.

---

# 14. Observabilidade

Cada módulo deverá possuir:

- Actuator;
- Health Check;
- Correlation ID;
- Logs estruturados.

Micrometer deverá ser utilizado para métricas.

---

# 15. Segurança

Autenticação:

OAuth2 + JWT.

Autorização:

Escopo:

```text
biometria:read
```

Gateway valida token.

Core permanece protegido atrás do Gateway.

---

# 16. Testes

Cada módulo deverá possuir:

## Unitários

- Use Cases
- Mappers
- Validators

## Integração

- Controllers
- SOAP Adapter
- Gateway

Objetivo mínimo:

- Cobertura significativa das regras de negócio.
- Testes de cenários felizes e de erro.

---

# 17. Qualidade

Antes de concluir qualquer implementação:

- Build verde.
- Testes verdes.
- Sem warnings relevantes.
- Código formatado.
- Sem duplicação desnecessária.
- Sem TODOs esquecidos.

---

# 18. Git

Fluxo sugerido:

- branch por funcionalidade;
- commits pequenos;
- mensagens descritivas;
- merge apenas após validação.

---

# 19. Definition of Done

Uma funcionalidade só será considerada concluída quando possuir:

- código implementado;
- testes;
- documentação atualizada;
- OpenAPI atualizada;
- logs implementados;
- tratamento de erros;
- cobertura adequada;
- aderência às ADRs.

---

# 20. Checklist de Implementação

## Arquitetura

- [ ] Estrutura multimódulo criada.
- [ ] Pacotes conforme blueprint.
- [ ] Clean Architecture respeitada.
- [ ] Ports & Adapters implementados.

## Segurança

- [ ] Keycloak configurado.
- [ ] JWT validado.
- [ ] Escopos configurados.

## Integração

- [ ] SOAP fake implementado.
- [ ] Adapter implementado.
- [ ] Mapper implementado.

## Observabilidade

- [ ] Correlation ID.
- [ ] Logs estruturados.
- [ ] Actuator.
- [ ] Métricas.

## Qualidade

- [ ] Testes unitários.
- [ ] Testes de integração.
- [ ] Build verde.

---

# 21. Roadmap de Implementação

1. Estrutura Maven.
2. Docker Compose.
3. Keycloak.
4. Legacy SOAP.
5. Core API.
6. Gateway.
7. OpenAPI.
8. Observabilidade.
9. Testes.
10. Refinamento.

---

# 22. Considerações Finais

Este Blueprint representa o elo entre a arquitetura e o código.

Sua finalidade é assegurar que todas as decisões registradas no DAS, ADRs e Diagramas C4 sejam refletidas fielmente na implementação.

Alterações arquiteturais relevantes deverão ser formalizadas por meio de novas ADRs antes de qualquer mudança no código, preservando a rastreabilidade das decisões e a consistência da solução ao longo de sua evolução.
