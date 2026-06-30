# Architecture Overview

> **Projeto:** POC Vivo – Integração Arquitetural  
> **Documento:** Visão Geral da Arquitetura  
> **Versão:** 1.0  
> **Status:** Draft

---

# 1. Propósito

Este documento apresenta uma visão executiva da arquitetura da POC antes do detalhamento técnico presente no DAS, ADRs e diagramas C4.

Seu objetivo é responder rapidamente:

- Qual problema a solução resolve?
- Como a arquitetura está organizada?
- Quais componentes participam?
- Quais princípios arquiteturais foram adotados?
- Como a solução poderá evoluir futuramente?

---

# 2. Visão Executiva

A solução moderniza um sistema legado que disponibiliza informações de biometria facial exclusivamente por SOAP, tornando essas informações acessíveis por uma API REST moderna protegida por OAuth2/JWT.

A arquitetura foi desenhada para preservar o legado, reduzir acoplamento e permitir evolução gradual sem interromper consumidores existentes.

---

# 3. Objetivos Arquiteturais

- Modernizar a exposição do legado.
- Isolar o contrato SOAP.
- Expor APIs REST governadas.
- Centralizar segurança.
- Garantir rastreabilidade ponta a ponta.
- Facilitar evolução para microsserviços.
- Manter o legado como System of Record.

---

# 4. Princípios Arquiteturais

- API First
- REST First
- Security by Design
- Clean Architecture
- Ports and Adapters
- Separation of Concerns
- Loose Coupling
- High Cohesion
- Observability First
- Evolutionary Architecture

---

# 5. Componentes

## Parceiro Externo

Consumidor autorizado da API.

## API Gateway

Responsável por:

- OAuth2/JWT
- Roteamento
- Correlation ID
- Logging de borda
- Políticas futuras (Rate Limit, Quotas)

## Biometria Core API

Responsável por:

- Casos de uso
- Validação
- Tradução REST ⇄ SOAP
- Regras de integração
- Tratamento de exceções

## Legacy SOAP

Simula o sistema legado.

## Banco H2

Representa o Oracle da POC.

## Keycloak

Provedor OAuth2/OpenID Connect.

---

# 6. Fluxo Principal

Parceiro Externo

↓

Keycloak (obtenção do token)

↓

API Gateway

↓

Biometria Core API

↓

SOAP Adapter

↓

Legacy SOAP

↓

H2

---

# 7. Decisões Arquiteturais

| ADR | Tema |
|------|------|
| ADR-001 | REST sobre SOAP |
| ADR-002 | API Gateway |
| ADR-003 | Adapter + Anti-Corruption Layer |
| ADR-004 | OAuth2/JWT + Keycloak |
| ADR-005 | Observabilidade |
| ADR-006 | Versionamento |

---

# 8. Requisitos Não Funcionais

- Segurança
- Disponibilidade
- Escalabilidade
- Observabilidade
- Governança
- Versionamento
- Baixo acoplamento
- Testabilidade

---

# 9. Estratégia de Segurança

- OAuth2
- JWT
- Client Credentials
- Escopos
- Gateway como ponto de entrada
- Logs sem dados sensíveis

---

# 10. Estratégia de Observabilidade

- Logs estruturados
- Correlation ID
- Actuator
- Micrometer
- Preparação para OpenTelemetry

---

# 11. Estratégia de Evolução

A arquitetura permite substituir o legado SOAP futuramente por:

- API REST interna
- Microserviço dedicado
- Cache
- Eventos
- Read Model
- Oracle real
- Kubernetes
- Service Mesh

Sem alterar o contrato público da API.

---

# 12. Estrutura da Documentação

```text
docs/
├── architecture/
│   ├── ARCHITECTURE-OVERVIEW.md
│   └── DAS.md
├── adr/
├── diagrams/
│   ├── c4/
│   ├── sequence/
│   └── deployment/
├── api/
│   └── openapi-v1.yaml
├── decisions/
└── README.md
```

---

# 13. Roadmap Arquitetural

- [x] Architecture Overview
- [x] DAS inicial
- [x] ADR-001 a ADR-006
- [ ] C1 – System Context
- [ ] C2 – Container
- [ ] C3 – Component
- [ ] Diagramas de Sequência
- [ ] OpenAPI
- [ ] Implementação
- [ ] Testes
- [ ] Documentação Final

---

# 14. Referências

- Documento de Arquitetura da Solução (DAS)
- ADR-001 a ADR-006
- Diagramas C4
- OpenAPI
- README
