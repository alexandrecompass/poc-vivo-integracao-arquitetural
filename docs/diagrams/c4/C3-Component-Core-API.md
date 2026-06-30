# C4 Model — C3 Component Diagram

> **Projeto:** POC Vivo – Integração Arquitetural  
> **Artefato:** C3 – Component Diagram  
> **Container detalhado:** Biometria Core API  
> **Versão:** 1.0  
> **Status:** Draft

---

# 1. Objetivo

O diagrama C3 detalha os principais componentes internos da **Biometria Core API**, evidenciando como o microserviço REST foi organizado para manter separação de responsabilidades, baixo acoplamento e isolamento do legado SOAP.

Este artefato demonstra como a arquitetura aplica:

- Clean Architecture;
- Ports and Adapters;
- Adapter Pattern;
- Anti-Corruption Layer;
- Facade;
- DTO Mapping;
- Observabilidade;
- Tratamento padronizado de erros.

---

# 2. Container Detalhado

O container detalhado neste C3 é:

```text
Biometria Core API
```

Responsabilidade geral:

- Receber requisições REST versionadas.
- Validar CPF.
- Executar o caso de uso de consulta de biometria.
- Isolar a integração SOAP.
- Traduzir dados do legado para contrato REST.
- Registrar logs estruturados.
- Propagar correlation ID.
- Padronizar respostas de erro.

---

# 3. Componentes Internos

## 3.1 BiometriaController

**Camada:** API / Entrada  
**Tipo:** REST Controller  
**Responsabilidade:** Expor o endpoint REST público interno da Biometria Core API.

Endpoint:

```http
GET /api/v1/biometria/{cpf}
```

Responsabilidades:

- Receber requisição HTTP.
- Extrair CPF do path.
- Delegar ao caso de uso.
- Retornar DTO REST.
- Não conter regra de negócio.
- Não conhecer SOAP.
- Não manipular diretamente o legado.

---

## 3.2 BiometriaRequestValidator

**Camada:** API / Validação  
**Tipo:** Validator  
**Responsabilidade:** Validar dados de entrada antes da execução do caso de uso.

Responsabilidades:

- Validar formato do CPF.
- Rejeitar CPF inválido.
- Normalizar CPF, se necessário.
- Evitar chamadas desnecessárias ao legado.

Observação:

A validação pode ser implementada como componente dedicado, Bean Validation, Value Object `Cpf` ou combinação desses recursos.

---

## 3.3 ConsultarBiometriaUseCase

**Camada:** Application  
**Tipo:** Use Case / Application Service  
**Responsabilidade:** Orquestrar a consulta de biometria.

Responsabilidades:

- Receber CPF validado.
- Acionar porta de saída para consulta ao legado.
- Aplicar regras de orquestração.
- Interpretar retorno interno.
- Produzir resposta para o controller.
- Não depender de SOAP, XML ou WSDL.

Esse componente representa o coração da aplicação.

---

## 3.4 BiometriaLegadoPort

**Camada:** Port / Application Boundary  
**Tipo:** Interface de saída  
**Responsabilidade:** Definir o contrato interno de comunicação com a origem de biometria.

Responsabilidades:

- Descrever a operação necessária para a aplicação.
- Desacoplar o use case da implementação SOAP.
- Permitir mocks em testes.
- Permitir troca futura da origem de dados.

Exemplo conceitual:

```java
public interface BiometriaLegadoPort {
    Biometria consultarPorCpf(Cpf cpf);
}
```

---

## 3.5 SoapBiometriaLegadoAdapter

**Camada:** Infrastructure / Adapter  
**Tipo:** Adapter SOAP  
**Responsabilidade:** Implementar a porta de saída usando o serviço SOAP legado.

Responsabilidades:

- Receber chamada da porta.
- Montar request SOAP.
- Chamar SOAP Client.
- Tratar falhas técnicas.
- Invocar mapper.
- Traduzir resposta SOAP para domínio.
- Esconder detalhes SOAP das camadas superiores.

---

## 3.6 SoapBiometriaClient

**Camada:** Infrastructure  
**Tipo:** Cliente SOAP  
**Responsabilidade:** Realizar a chamada técnica ao Legacy SOAP Service.

Responsabilidades:

- Serializar request SOAP.
- Enviar requisição HTTP/SOAP.
- Receber resposta SOAP.
- Propagar correlation ID quando possível.
- Respeitar timeout configurado.
- Não conter regra de domínio.

---

## 3.7 SoapBiometriaMapper

**Camada:** Infrastructure / Mapping  
**Tipo:** Mapper  
**Responsabilidade:** Converter estruturas SOAP para modelo interno.

Responsabilidades:

- Converter resposta SOAP em objeto de domínio.
- Normalizar status do legado.
- Omitir campos técnicos.
- Converter datas.
- Interpretar códigos de retorno.
- Evitar vazamento de estruturas SOAP.

---

## 3.8 SoapFaultTranslator

**Camada:** Infrastructure / Error Translation  
**Tipo:** Tradutor de erros  
**Responsabilidade:** Traduzir SOAP Faults e erros técnicos para exceções internas.

Responsabilidades:

- Tratar SOAP Fault.
- Tratar timeout.
- Tratar indisponibilidade.
- Tratar resposta inválida.
- Converter falhas em exceções da aplicação.

Exemplos:

| Falha | Exceção interna |
|---|---|
| CPF não encontrado | BiometriaNaoEncontradaException |
| Timeout SOAP | LegadoTimeoutException |
| SOAP indisponível | LegadoIndisponivelException |
| Resposta inválida | RespostaLegadoInvalidaException |

---

## 3.9 Biometria Domain Model

**Camada:** Domain  
**Tipo:** Modelo de domínio  
**Responsabilidade:** Representar a biometria no modelo interno da aplicação.

Possíveis objetos:

- `Biometria`
- `Cpf`
- `BiometriaStatus`
- `OrigemBiometria`

Responsabilidades:

- Representar conceitos internos.
- Evitar dependência de DTO REST.
- Evitar dependência de SOAP.
- Manter linguagem clara do domínio.

---

## 3.10 BiometriaResponseMapper

**Camada:** API / Mapping  
**Tipo:** Mapper REST  
**Responsabilidade:** Converter modelo interno para DTO REST v1.

Responsabilidades:

- Construir resposta da API.
- Mascarar CPF quando aplicável.
- Incluir origem.
- Incluir data da consulta.
- Evitar exposição de campos internos.

---

## 3.11 GlobalExceptionHandler

**Camada:** API / Error Handling  
**Tipo:** Exception Handler  
**Responsabilidade:** Padronizar respostas de erro REST.

Responsabilidades:

- Converter exceções internas em HTTP status.
- Retornar erro padronizado.
- Incluir correlation ID.
- Evitar exposição de stack trace.
- Evitar exposição de detalhes do legado.

Exemplos:

| Exceção | HTTP |
|---|---:|
| CpfInvalidoException | 400 |
| BiometriaNaoEncontradaException | 404 |
| LegadoIndisponivelException | 502 |
| LegadoTimeoutException | 504 |
| Exception | 500 |

---

## 3.12 CorrelationIdFilter

**Camada:** Infrastructure / Observability  
**Tipo:** Filter  
**Responsabilidade:** Garantir correlation ID em todas as requisições.

Responsabilidades:

- Ler `X-Correlation-Id`.
- Gerar correlation ID se ausente.
- Inserir valor no MDC/log context.
- Propagar para chamadas externas.
- Retornar correlation ID na resposta.

---

## 3.13 RequestLoggingFilter

**Camada:** Infrastructure / Observability  
**Tipo:** Filter  
**Responsabilidade:** Registrar logs estruturados de entrada e saída.

Responsabilidades:

- Logar início da requisição.
- Logar fim da requisição.
- Medir duração.
- Registrar status HTTP.
- Mascarar CPF.
- Não registrar payload sensível.

---

## 3.14 BiometriaMetrics

**Camada:** Infrastructure / Observability  
**Tipo:** Metrics Component  
**Responsabilidade:** Registrar métricas relevantes.

Métricas sugeridas:

- Total de consultas.
- Total de sucesso.
- Total de biometria não encontrada.
- Total de falhas do legado.
- Latência da chamada SOAP.
- Latência total da API.

Na POC, pode ser implementado parcialmente com Actuator/Micrometer.

---

# 4. Relações entre Componentes

| Origem | Destino | Relação |
|---|---|---|
| API Gateway | BiometriaController | Encaminha HTTP REST |
| BiometriaController | BiometriaRequestValidator | Valida entrada |
| BiometriaController | ConsultarBiometriaUseCase | Executa caso de uso |
| ConsultarBiometriaUseCase | BiometriaLegadoPort | Consulta origem de biometria |
| SoapBiometriaLegadoAdapter | BiometriaLegadoPort | Implementa porta |
| SoapBiometriaLegadoAdapter | SoapBiometriaClient | Chama SOAP |
| SoapBiometriaLegadoAdapter | SoapBiometriaMapper | Mapeia resposta |
| SoapBiometriaLegadoAdapter | SoapFaultTranslator | Traduz falhas |
| SoapBiometriaClient | Legacy SOAP Service | SOAP HTTP |
| ConsultarBiometriaUseCase | Biometria Domain Model | Usa modelo interno |
| BiometriaController | BiometriaResponseMapper | Converte domínio para REST |
| GlobalExceptionHandler | Exceções internas | Converte para HTTP |
| CorrelationIdFilter | Todos os componentes | Garante rastreabilidade |
| RequestLoggingFilter | Fluxo HTTP | Registra logs estruturados |
| BiometriaMetrics | Fluxo de execução | Registra métricas |

---

# 5. Fluxo Principal

```text
1. API Gateway encaminha GET /api/v1/biometria/{cpf}.
2. CorrelationIdFilter captura ou cria correlation ID.
3. RequestLoggingFilter registra início da requisição.
4. BiometriaController recebe o CPF.
5. BiometriaRequestValidator valida o CPF.
6. Controller chama ConsultarBiometriaUseCase.
7. UseCase chama BiometriaLegadoPort.
8. SoapBiometriaLegadoAdapter implementa a porta.
9. Adapter chama SoapBiometriaClient.
10. SoapClient chama Legacy SOAP Service.
11. Legacy SOAP retorna resposta.
12. SoapBiometriaMapper converte SOAP para domínio.
13. UseCase retorna modelo interno.
14. BiometriaResponseMapper converte para DTO REST v1.
15. Controller retorna HTTP 200.
16. RequestLoggingFilter registra status e duração.
```

---

# 6. Fluxo de Erro

```text
1. Legacy SOAP retorna fault ou falha técnica.
2. SoapBiometriaClient recebe erro.
3. SoapFaultTranslator converte erro técnico em exceção interna.
4. UseCase propaga exceção.
5. GlobalExceptionHandler converte exceção em resposta REST padronizada.
6. Logs registram erro técnico com correlation ID.
7. Consumidor recebe status adequado, sem detalhes internos do legado.
```

---

# 7. Princípios Aplicados

## Separação de responsabilidades

Cada componente tem uma função clara.

## Inversão de dependência

O use case depende de uma porta, não do adapter SOAP.

## Isolamento do legado

SOAP fica restrito à infraestrutura.

## Contrato REST limpo

DTOs REST não reutilizam classes SOAP.

## Observabilidade transversal

Filtros e métricas envolvem o fluxo sem misturar regra de negócio.

## Segurança de dados

Logs não devem conter CPF completo, JWT completo ou imagem/Base64.

---

# 8. Regras Arquiteturais

1. Controller não chama SOAP diretamente.
2. Controller não contém regra de negócio.
3. Use case não depende de WSDL/XML.
4. Use case depende apenas da porta.
5. Adapter SOAP implementa a porta.
6. Mapper SOAP fica isolado no adapter.
7. DTO REST não reutiliza DTO SOAP.
8. Erro SOAP não vaza para resposta REST.
9. CPF deve ser validado antes da chamada ao legado.
10. Logs devem conter correlation ID.
11. Logs não devem conter biometria/Base64.
12. Logs não devem conter CPF completo.

---

# 9. Estrutura de Pacotes Recomendada

```text
br.com.vivo.poc.biometria
├── api
│   ├── BiometriaController
│   ├── mapper
│   │   └── BiometriaResponseMapper
│   └── dto
│       ├── BiometriaResponse
│       └── ErrorResponse
│
├── application
│   ├── ConsultarBiometriaUseCase
│   └── exception
│       ├── CpfInvalidoException
│       ├── BiometriaNaoEncontradaException
│       ├── LegadoIndisponivelException
│       └── LegadoTimeoutException
│
├── domain
│   ├── Biometria
│   ├── Cpf
│   ├── BiometriaStatus
│   └── OrigemBiometria
│
├── port
│   └── BiometriaLegadoPort
│
├── adapter
│   └── soap
│       ├── SoapBiometriaLegadoAdapter
│       ├── SoapBiometriaClient
│       ├── SoapBiometriaMapper
│       └── SoapFaultTranslator
│
└── infrastructure
    ├── config
    │   ├── SoapClientConfig
    │   └── ObservabilityConfig
    ├── error
    │   └── GlobalExceptionHandler
    ├── logging
    │   ├── CorrelationIdFilter
    │   ├── RequestLoggingFilter
    │   └── CpfMasker
    └── metrics
        └── BiometriaMetrics
```

---

# 10. Decisões Relacionadas

| ADR | Relação com C3 |
|---|---|
| ADR-001 | Controller REST atua como fachada para o legado |
| ADR-003 | Porta, Adapter e Anti-Corruption Layer |
| ADR-005 | Filtros, logs, métricas e correlation ID |
| ADR-006 | Controller expõe `/api/v1` |

---

# 11. Elementos Deliberadamente Omitidos

Não detalhamos neste C3:

- Componentes internos do API Gateway.
- Componentes internos do Keycloak.
- Componentes internos do Legacy SOAP.
- Estrutura interna do H2.
- Implementação real do Oracle.
- Infraestrutura Docker.
- Dashboards de observabilidade.

Esses elementos pertencem a outros diagramas ou documentos.

---

# 12. Checklist do Artefato

## Documentação

- [x] Componentes internos definidos.
- [x] Responsabilidades descritas.
- [x] Relações entre componentes descritas.
- [x] Fluxo principal descrito.
- [x] Fluxo de erro descrito.
- [x] Regras arquiteturais descritas.
- [x] Estrutura de pacotes sugerida.
- [ ] Gerar Mermaid C3.
- [ ] Gerar Structurizr DSL C3.
- [ ] Gerar Draw.io C3.

## Implementação futura

- [ ] Criar controller REST.
- [ ] Criar validator/value object de CPF.
- [ ] Criar use case.
- [ ] Criar porta de saída.
- [ ] Criar adapter SOAP.
- [ ] Criar client SOAP.
- [ ] Criar mapper SOAP.
- [ ] Criar translator de SOAP Fault.
- [ ] Criar mapper REST.
- [ ] Criar exception handler.
- [ ] Criar filtros de correlation/logging.
- [ ] Criar métricas.
- [ ] Criar testes unitários.
- [ ] Criar testes de integração.

---

# 13. Próximo Artefato

**C3 — Component Diagram em Mermaid**

Arquivo sugerido:

```text
docs/diagrams/c4/C3-Component-Core-API.mmd
```
