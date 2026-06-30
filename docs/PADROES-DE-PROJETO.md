# Padrões de Projeto — POC Vivo Biometria Facial

Este documento é um guia de referência rápida para apoiar a apresentação e revisão técnica da solução. Para cada padrão estão descritos: **o que é**, **onde está no código** e **por que foi escolhido**.

---

## Sumário

1. [Arquiteturais](#1-padrões-arquiteturais)
   - [Clean Architecture / Ports & Adapters](#11-clean-architecture--ports--adapters-hexagonal)
   - [API Gateway](#12-api-gateway)
   - [Strangler Fig](#13-strangler-fig)
2. [GoF — Estruturais](#2-padrões-gof--estruturais)
   - [Adapter](#21-adapter)
   - [Facade](#22-facade)
   - [Decorator](#23-decorator)
3. [GoF — Comportamentais](#3-padrões-gof--comportamentais)
   - [Chain of Responsibility (Filter Chain)](#31-chain-of-responsibility--filter-chain)
   - [Template Method](#32-template-method)
   - [Strategy (implícito)](#33-strategy-implícito-via-polimorfismo)
4. [DDD](#4-padrões-ddd-domain-driven-design)
   - [Value Object](#41-value-object)
   - [Anti-Corruption Layer (ACL)](#42-anti-corruption-layer-acl)
5. [Segurança](#5-padrões-de-segurança)
   - [Defense in Depth](#51-defense-in-depth)
   - [Interceptor](#52-interceptor)
6. [Observabilidade](#6-padrões-de-observabilidade)
   - [Correlation ID / Trace Context Propagation](#61-correlation-id--trace-context-propagation)
   - [Health Indicator](#62-health-indicator)

---

## 1. Padrões Arquiteturais

### 1.1 Clean Architecture / Ports & Adapters (Hexagonal)

**O que é:**
Organiza o código em camadas concêntricas onde o **domínio** (regras de negócio) fica no centro, isolado de frameworks, banco de dados e protocolos externos. A comunicação entre camadas acontece exclusivamente através de **interfaces** (Ports). Implementações concretas (Adapters) ficam na camada mais externa.

**Onde está no código (`biometria-core-api`):**
```
domain/          ← regras puras: Cpf (VO), Biometria, BiometriaStatus
port/            ← contratos: BiometriaLegadoPort (interface)
application/     ← use cases: ConsultarBiometriaUseCase, ValidarCpfUseCase
adapter/soap/    ← implementação: SoapBiometriaLegadoAdapter (impl da Port)
api/             ← entrada REST: BiometriaController, CpfValidadorController
infrastructure/  ← cross-cutting: security, logging, metrics, config
```

**Por que foi escolhido:**
Permite trocar o legado SOAP por qualquer outra tecnologia (REST, gRPC, banco direto) sem alterar uma linha de código do domínio ou dos use cases. É o alicerce da estratégia de migração Strangler Fig.

---

### 1.2 API Gateway

**O que é:**
Um único ponto de entrada que centraliza: autenticação JWT, roteamento, injeção de headers internos de segurança, logging e métricas. Os serviços internos nunca ficam expostos diretamente ao mundo externo.

**Onde está no código (`biometria-gateway`):**
```
GatewaySecurityConfig.java   ← valida JWT (OAuth2 Resource Server)
GatewayRoutesConfig.java     ← define rotas e injeta X-Gateway-Secret
CorrelationIdGatewayFilter   ← gera/propaga X-Correlation-Id
GatewayLoggingFilter         ← log estruturado de entrada/saída
GatewayMetricsFilter         ← métricas Prometheus por rota
```

**Por que foi escolhido:**
Garante que `biometria-core-api` só seja acessível através do Gateway (via X-Gateway-Secret), evitando bypass da autenticação. Simplifica o onboarding de novos parceiros — basta emitir um novo client no Keycloak.

---

### 1.3 Strangler Fig

**O que é:**
Padrão de migração que consiste em "estrangular" gradualmente um sistema legado envolvendo-o com novas funcionalidades. O legado continua operando normalmente enquanto as novas rotas são adicionadas em paralelo. Com o tempo, o legado é desligado sem big-bang.

**Onde está no código:**
O Gateway roteia chamadas para o `biometria-core-api` (REST moderno) que internamente ainda chama o `biometria-legacy-soap`. O SOAP representa o sistema legado que será gradualmente substituído.

**Fases no projeto** (`docs/apresentacao/DATI-2.0-POC-Vivo-Biometria.docx` — seção 3.3):
- **Fase 1 (0–3 meses):** Gateway na frente, legado SOAP intocado
- **Fase 2 (3–12 meses):** Novo microserviço assume domínios por domínio
- **Fase 3 (12–24 meses):** SOAP descomissionado

**Por que foi escolhido:**
Elimina o risco de uma migração em big-bang. Produção continua estável enquanto a modernização acontece de forma incremental e reversível.

---

## 2. Padrões GoF — Estruturais

### 2.1 Adapter

**O que é (GoF):**
Converte a interface de uma classe em outra que os clientes esperam. Permite que classes com interfaces incompatíveis trabalhem juntas.

**Onde está no código:**
```java
// Port (interface que o domínio conhece):
BiometriaLegadoPort.java → consultarPorCpf(Cpf cpf): Biometria

// Adapter (implementação que fala SOAP):
SoapBiometriaLegadoAdapter.java
  → implements BiometriaLegadoPort
  → converte Cpf (domínio) → XML SOAP → Biometria (domínio)
```

**Por que foi escolhido:**
O domínio não sabe que existe SOAP. Se amanhã o legado virar REST ou GraphQL, basta trocar o Adapter — zero impacto no UseCase ou no Controller.

---

### 2.2 Facade

**O que é (GoF):**
Fornece uma interface simplificada para um subsistema complexo, escondendo a complexidade interna.

**Onde está no código:**
```java
SoapBiometriaClient.java
  → encapsula WebServiceTemplate (Spring-WS)
  → gestão de timeout, marshalling, WSDL, endpoint URL
  → SoapBiometriaLegadoAdapter usa este client sem saber dos detalhes
```

**Por que foi escolhido:**
Isola a complexidade de configuração do Spring-WS (marshallers JAXB, interceptors, timeouts). O Adapter é simples e legível; toda a "canalização" SOAP fica encapsulada no Client.

---

### 2.3 Decorator

**O que é (GoF):**
Adiciona responsabilidades a um objeto dinamicamente, envolvendo-o com comportamentos extras sem alterar sua interface.

**Onde está no código:**
```java
BiometriaMetrics.java

// registrarApiLatencia envolve qualquer Supplier<T> com medição de tempo:
public <T> T registrarApiLatencia(Supplier<T> fn) {
    return apiLatencia.record(fn);  // ← decora a função com Timer
}

// Uso no Controller:
BiometriaResponse response = metrics.registrarApiLatencia(
    () -> responseMapper.toResponse(useCase.executar(cpf))
);
```

**Por que foi escolhido:**
Separa a lógica de negócio da preocupação de métricas. A lógica do UseCase não sabe que está sendo cronometrada. O Decorator injeta o comportamento transversal (cross-cutting concern) de forma transparente.

---

## 3. Padrões GoF — Comportamentais

### 3.1 Chain of Responsibility — Filter Chain

**O que é (GoF):**
Passa uma requisição por uma cadeia de handlers. Cada handler decide processar a requisição, modificá-la ou passá-la adiante.

**Onde está no código:**

Gateway (Spring Cloud Gateway — WebFlux):
```
CorrelationIdGatewayFilter  → gera/propaga X-Correlation-Id  (Ordered.HIGHEST_PRECEDENCE + 1)
GatewayLoggingFilter        → log de entrada e saída          (Ordered.LOWEST_PRECEDENCE - 2)
GatewayMetricsFilter        → registra métricas por rota      (Ordered.LOWEST_PRECEDENCE - 1)
GatewaySecurityConfig       → valida JWT (via Spring Security)
```

Core API (Spring MVC — Servlet):
```
GatewaySecretFilter     → valida X-Gateway-Secret (antes do BearerTokenFilter)
CorrelationIdFilter     → propaga correlationId para MDC
RequestLoggingFilter    → log estruturado de todas as requisições
```

Legacy SOAP (Spring-WS):
```
InternalSecretInterceptor → valida X-Core-Secret no SOAP header
```

**Por que foi escolhido:**
Cada responsabilidade de segurança e observabilidade fica em um handler isolado e reutilizável, sem poluir a lógica de negócio nos Controllers e Use Cases.

---

### 3.2 Template Method

**O que é (GoF):**
Define o esqueleto de um algoritmo em uma classe base, deixando que as subclasses preencham etapas específicas.

**Onde está no código:**
```java
// Spring usa OncePerRequestFilter como classe base:
public abstract class OncePerRequestFilter extends GenericFilterBean {
    protected abstract void doFilterInternal(...) throws ...;  // ← Template Method
    protected boolean shouldNotFilter(...) { return false; }   // ← hook opcional
}

// Implementações no projeto:
GatewaySecretFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(request) {
        return !request.getRequestURI().startsWith("/api/v1/biometria/");
    }
    @Override
    protected void doFilterInternal(...) { /* valida X-Gateway-Secret */ }
}
```

**Por que foi escolhido:**
O Spring já fornece o Template (OncePerRequestFilter) com o ciclo de vida correto (executa exatamente uma vez por request mesmo em dispatch incluso). O projeto apenas preenche a lógica específica, sem reimplementar o mecanismo de filtragem.

---

### 3.3 Strategy (implícito via polimorfismo)

**O que é (GoF):**
Define uma família de algoritmos intercambiáveis encapsulados em classes. O cliente não sabe qual algoritmo concreto está sendo executado.

**Onde está no código:**
```java
// SoapFaultTranslator — estratégia de tradução de erros:
public RuntimeException traduzir(SoapFaultClientException ex) {
    if (faultString.contains("BIOMETRIA_NAO_ENCONTRADA"))
        return new BiometriaNaoEncontradaException(...);
    return new LegadoIndisponivelException(...);
}

// A "estratégia" do port permite futuras implementações alternativas:
BiometriaLegadoPort legadoPort;  // ← pode ser SOAP, REST, mock, cache...
// ConsultarBiometriaUseCase não muda independente da implementação
```

**Por que foi escolhido:**
Facilita testes (basta injetar um mock de `BiometriaLegadoPort` nos testes de UseCase) e permite substituir o legado SOAP por outra implementação sem alterar o código cliente.

---

## 4. Padrões DDD (Domain-Driven Design)

### 4.1 Value Object

**O que é:**
Um objeto cujo valor define a identidade — não há ID técnico. É **imutável**, valida as próprias regras de negócio na construção, e a igualdade é por valor (não por referência).

**Onde está no código:**
```java
// domain/Cpf.java — Value Object canônico:
public final class Cpf {               // ← final: não pode ser herdado
    private final String valor;        // ← imutável

    private Cpf(String valor) { ... }  // ← construtor privado: só via factory method

    public static Cpf of(String raw) {
        // valida regra de negócio (dígito verificador) ANTES de construir
        if (!isValido(sanitize(raw))) throw new CpfInvalidoException(raw);
        return new Cpf(sanitize(raw));
    }

    @Override
    public String toString() {
        return "*********" + valor.substring(9); // ← máscara LGPD embutida
    }
    // equals/hashCode por valor (não por referência)
}
```

**Por que foi escolhido:**
Elimina CPF inválido em qualquer ponto do sistema — se um `Cpf` existe, é válido. A máscara LGPD no `toString()` garante que o CPF nunca aparece completo em logs, independente de quem o usa.

---

### 4.2 Anti-Corruption Layer (ACL)

**O que é:**
Camada de tradução entre o modelo de domínio e um modelo externo (legado, terceiro, sistema diferente). Evita que os conceitos e contratos externos "contaminem" o domínio da aplicação.

**Onde está no código:**
```java
// adapter/soap/SoapBiometriaMapper.java
// Traduz: ConsultarBiometriaResponse (JAXB/SOAP) → Biometria (domínio)
public Biometria todominio(ConsultarBiometriaResponse response) { ... }

// adapter/soap/SoapFaultTranslator.java
// Traduz: SoapFaultClientException (Spring-WS) → exceções do domínio
public RuntimeException traduzir(SoapFaultClientException ex) { ... }

// adapter/soap/SoapBiometriaLegadoAdapter.java
// Orquestra a ACL: usa Client → Mapper → FaultTranslator
// O domínio nunca vê JAXB, WebServiceTemplate ou SoapFault
```

**Por que foi escolhido:**
O contrato SOAP do legado tem campos específicos do sistema antigo (`status_cod`, `dt_ult_verif_TS`, etc.). A ACL transforma esses conceitos para o vocabulário do domínio (`BiometriaStatus`, `Biometria`). Uma mudança no legado impacta apenas a ACL, não o domínio.

---

## 5. Padrões de Segurança

### 5.1 Defense in Depth (Defesa em Profundidade)

**O que é:**
Múltiplas camadas independentes de segurança. Se uma camada for comprometida, as demais ainda protegem o sistema.

**Onde está no código:**

```
Camada 1 — Internet → Gateway (porta 8080)
  JWT RS256 (Keycloak) + escopo biometria:read obrigatório
  Qualquer request sem token válido retorna 401 antes de tocar o Core API

Camada 2 — Gateway → Core API (porta 8082)
  Header X-Gateway-Secret injetado pelo Gateway, validado pelo GatewaySecretFilter
  Core API rejeita com 403 qualquer request sem este header — mesmo com JWT válido

Camada 3 — Core API → Legacy SOAP (porta 8083)
  Header SOAP X-Core-Secret injetado pelo SoapBiometriaClient
  InternalSecretInterceptor bloqueia chamadas sem este header
  SOAP nunca exposto externamente (não há rota no Gateway)
```

**Por que foi escolhido:**
Impede ataques de bypass: mesmo que alguém descubra a porta 8082 internamente, não consegue acessar sem o X-Gateway-Secret. O SOAP legado fica completamente isolado da internet.

---

### 5.2 Interceptor

**O que é:**
Ponto de extensão que captura e processa requisições/respostas antes e depois da execução principal, sem modificar o código central.

**Onde está no código:**
```java
// biometria-legacy-soap:
InternalSecretInterceptor implements EndpointInterceptor {
    @Override
    public boolean handleRequest(MessageContext ctx, Object endpoint) {
        // extrai X-Core-Secret do SOAP header
        // rejeita com SOAP Fault se ausente ou inválido
        // retorna false para interromper a cadeia
    }
}

// WsConfig.java — registra o interceptor em todos os endpoints SOAP:
@Override
public void addInterceptors(List<EndpointInterceptor> interceptors) {
    interceptors.add(internalSecretInterceptor);
}
```

**Por que foi escolhido:**
O `BiometriaEndpoint` (lógica de negócio SOAP) não contém nenhum código de segurança. O Interceptor é declarativo e aplicado transversalmente a todos os endpoints, garantindo que nenhum endpoint fique desprotegido por esquecimento.

---

## 6. Padrões de Observabilidade

### 6.1 Correlation ID / Trace Context Propagation

**O que é:**
Um identificador único (`X-Correlation-Id`) gerado na borda do sistema (Gateway) e propagado em todos os headers HTTP e nos logs de cada serviço. Permite reconstruir o fluxo completo de uma requisição distribuída.

**Onde está no código:**
```
CorrelationIdGatewayFilter    → gera UUID se ausente, injeta no header e no MDC
  ↓ X-Correlation-Id →
CorrelationIdFilter (Core)    → lê o header, injeta no MDC (correlationId)
  ↓ X-Correlation-Id →
SoapBiometriaClient           → propaga para o header SOAP (via WebServiceTemplate)

Todos os logs JSON incluem: "correlationId": "uuid-aqui"
```

Complementado por B3 Tracing (Micrometer + Zipkin) que propaga `traceId` e `spanId` automaticamente.

**Por que foi escolhido:**
Em uma arquitetura distribuída com 3 serviços, identificar qual requisição causou um erro requer que todos os logs falem o mesmo "idioma". O Correlation ID permite filtrar no Loki/Grafana todos os logs de uma única chamada do parceiro.

---

### 6.2 Health Indicator

**O que é:**
Componente que verifica a saúde de uma dependência externa e reporta o resultado para o Spring Actuator (`/actuator/health`).

**Onde está no código:**
```java
// infrastructure/health/LegadoSoapHealthIndicator.java
public class LegadoSoapHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // faz chamada de diagnóstico ao SOAP
            return Health.up()
                .withDetail("legado-soap", "acessivel")
                .withDetail("url", soapUrl)
                .build();
        } catch (Exception ex) {
            return Health.down()
                .withDetail("legado-soap", "inacessivel")
                .withException(ex)
                .build();
        }
    }
}
```

O Grafana consulta `/management/core-api/health` via Gateway e exibe os painéis de status UP/DOWN em tempo real.

**Por que foi escolhido:**
O Gateway expõe a saúde do Core API (que inclui o indicador do SOAP) através da rota `/management/core-api/**`. O SRE vê em um único endpoint se toda a cadeia está saudável, sem precisar acessar cada serviço individualmente.

---

## Mapa Visual — Onde cada padrão aparece

```
┌─────────────────────────────────────────────────────────────────────┐
│  INTERNET / PARCEIRO                                                │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTPS + JWT
                    ╔════════▼═══════════╗
                    ║  biometria-gateway  ║  ← API Gateway
                    ║  :8080              ║  ← Chain of Responsibility
                    ║                     ║     (Filter Chain)
                    ╚═══════╦═════════════╝
                            │ X-Gateway-Secret  ← Defense in Depth
                    ╔═══════▼═════════════════════════════════════╗
                    ║  biometria-core-api :8082                   ║
                    ║                                             ║
                    ║  Controller (api)                           ║
                    ║      ↓  Decorator (BiometriaMetrics)        ║
                    ║  UseCase (application)                      ║
                    ║      ↓  Strategy (BiometriaLegadoPort)      ║
                    ║  Port ←→ Adapter (SoapBiometriaLegadoAdapter)║
                    ║              ↓ Facade (SoapBiometriaClient) ║
                    ║              ↓ ACL (Mapper + FaultTranslator)║
                    ╚══════════════╦══════════════════════════════╝
                                   │ SOAP + X-Core-Secret  ← Defense in Depth
                    ╔══════════════▼══════════════╗
                    ║  biometria-legacy-soap :8083 ║  ← Interceptor
                    ║  (simulação legado — H2)     ║
                    ╚══════════════════════════════╝

  Cross-cutting (todos os serviços):
  ├── Value Object: Cpf (validação + máscara LGPD)
  ├── Template Method: OncePerRequestFilter
  ├── Correlation ID: X-Correlation-Id → Loki → Zipkin
  ├── Health Indicator: LegadoSoapHealthIndicator → Grafana
  └── Strangler Fig: strategy de migração SOAP → REST
```

---

*Referência: Design Patterns (GoF) — Gamma, Helm, Johnson, Vlissides · Clean Architecture — Robert C. Martin · DDD — Eric Evans · Enterprise Integration Patterns — Hohpe & Woolf*
