# C4 Model — C1 System Context

> **Projeto:** POC Vivo – Integração Arquitetural  
> **Artefato:** C1 – System Context  
> **Versão:** 1.0

---

# Objetivo

O diagrama C1 apresenta a solução sob o ponto de vista do negócio, mostrando apenas os atores e sistemas que interagem com ela, sem detalhar tecnologias ou componentes internos.

---

# Escopo

O sistema em foco é a **Plataforma de Integração de Biometria**.

Essa plataforma é responsável por disponibilizar informações de biometria facial provenientes de um sistema legado para parceiros externos através de uma API REST segura.

---

# Atores

## Parceiro Externo

Consumidor autorizado da API de biometria.

### Responsabilidades

- Solicitar token OAuth2.
- Consultar biometria por CPF.
- Consumir contrato REST versionado.

---

# Sistemas Externos

## Keycloak

Responsável por:

- Autenticação OAuth2
- Emissão de JWT
- Gestão de clientes
- Gestão de escopos

Relacionamento:

Parceiro Externo → Keycloak

---

## Plataforma de Integração de Biometria (Sistema em Desenvolvimento)

Responsável por:

- Expor API REST
- Validar acesso
- Integrar com legado SOAP
- Traduzir contratos
- Garantir rastreabilidade
- Registrar observabilidade

Relacionamentos:

Parceiro Externo → Plataforma

Plataforma → Sistema Legado SOAP

Plataforma → Keycloak

---

## Sistema Legado SOAP

Sistema oficial de consulta de biometria.

Responsabilidades:

- Consultar dados de biometria
- Ler dados do Oracle (H2 na POC)
- Responder via SOAP

Relacionamento:

Plataforma → Sistema Legado SOAP

---

# Fluxo Principal

1. Parceiro obtém token no Keycloak.
2. Parceiro chama a Plataforma de Integração.
3. A Plataforma autentica e autoriza.
4. A Plataforma consulta o Sistema Legado SOAP.
5. O Sistema Legado retorna os dados.
6. A Plataforma devolve resposta REST padronizada.

---

# Relações C4

| Origem | Destino | Tecnologia | Objetivo |
|---------|----------|------------|----------|
| Parceiro Externo | Keycloak | HTTPS / OAuth2 | Obter token |
| Parceiro Externo | Plataforma | HTTPS REST | Consultar biometria |
| Plataforma | Keycloak | JWT/JWKS | Validar token |
| Plataforma | Sistema Legado SOAP | SOAP | Consultar biometria |

---

# Sistemas Deliberadamente Omitidos

Não aparecem no C1:

- API Gateway
- Biometria Core API
- SOAP Adapter
- Banco H2
- Oracle
- OpenAPI
- Logs
- Métricas
- Observabilidade

Esses elementos pertencem aos níveis C2 e C3.

---

# Observações Arquiteturais

- O legado permanece como System of Record.
- Parceiros nunca acessam o legado diretamente.
- Todo acesso ocorre por meio da Plataforma de Integração.
- A plataforma desacopla consumidores do contrato SOAP.

---

# Checklist

- [x] Sistema principal identificado
- [x] Atores identificados
- [x] Sistemas externos identificados
- [x] Relações definidas
- [x] Responsabilidades descritas
- [ ] Diagrama Draw.io
- [ ] Diagrama Mermaid
- [ ] Diagrama Structurizr DSL

---

# Próximo Artefato

**C2 — Container Diagram**
