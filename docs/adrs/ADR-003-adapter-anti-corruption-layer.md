# ADR-003 — Uso de Adapter e Anti-Corruption Layer para Isolar o Legado SOAP

**Projeto:** POC Vivo — Integração Arquitetural  
**Status:** Aceita  
**Data:** 2026-06-30  
**Versão:** 1.0  
**Decisão relacionada:** Isolamento do contrato legado SOAP e proteção do domínio moderno REST

---

## 1. Contexto

A POC Vivo — Integração Arquitetural tem como objetivo disponibilizar informações de biometria facial de clientes para parceiros externos por meio de uma API REST moderna.

Entretanto, a origem desses dados está em um sistema legado com banco Oracle, onde as imagens estão armazenadas em tabela, e o único meio de acesso disponível é um serviço SOAP interno.

Essa situação é comum em ambientes corporativos grandes: sistemas legados continuam sendo fontes oficiais de dados, mas novos canais e parceiros precisam consumir essas informações através de contratos modernos, seguros, observáveis e governados.

O desafio arquitetural não é apenas converter SOAP para REST. O desafio maior é impedir que as características do legado contaminem a arquitetura moderna.

Se o contrato SOAP, os nomes técnicos, os tipos de dados legados, os códigos de erro antigos e as estruturas XML forem usados diretamente dentro da API REST, o novo serviço ficará fortemente acoplado ao legado. Isso dificultará manutenção, testes, evolução e eventual migração futura.

Por isso, esta ADR define o uso dos padrões **Adapter** e **Anti-Corruption Layer** para isolar o legado SOAP.

---

## 2. Problema

O sistema legado possui um contrato próprio, provavelmente baseado em:

- WSDL.
- XML.
- SOAP Envelope.
- SOAP Fault.
- Tipos gerados automaticamente.
- Estruturas orientadas ao modelo antigo.
- Códigos técnicos do legado.
- Campos com nomenclatura interna.
- Possíveis inconsistências ou convenções históricas.

O domínio moderno da API REST, por outro lado, deve oferecer um contrato limpo, simples, estável e orientado ao consumidor.

Se a API REST consumir e propagar diretamente estruturas SOAP, surgem problemas como:

- Acoplamento direto ao contrato legado.
- Exposição de detalhes técnicos antigos.
- Dificuldade de trocar o legado no futuro.
- Dificuldade de testar casos de uso sem SOAP.
- Contrato REST influenciado por XML/WSDL.
- Erros técnicos do legado vazando para consumidores externos.
- Menor clareza na separação entre domínio, aplicação e infraestrutura.
- Manutenção mais cara.
- Evolução mais arriscada.

A solução precisa criar uma fronteira explícita entre o mundo moderno REST e o mundo legado SOAP.

---

## 3. Decisão

A solução adotará uma camada de isolamento baseada nos padrões:

- **Adapter**
- **Anti-Corruption Layer**
- **Ports and Adapters**
- **Facade**
- **DTO Mapping**

O microserviço **Biometria Core API** não deverá depender diretamente de classes SOAP geradas, estruturas XML, envelopes SOAP ou códigos técnicos do legado em suas camadas de domínio e aplicação.

A comunicação com o legado será feita por meio de uma porta de saída interna, por exemplo:

```java
public interface BiometriaLegadoPort {
    BiometriaLegadoResponse consultarPorCpf(String cpf);
}
```

A implementação dessa porta ficará em um adapter de infraestrutura, por exemplo:

```java
public class SoapBiometriaLegadoAdapter implements BiometriaLegadoPort {
    // Chamada ao cliente SOAP
}
```

O adapter será responsável por:

- Montar a requisição SOAP.
- Chamar o serviço SOAP legado.
- Interpretar a resposta SOAP.
- Tratar SOAP Faults.
- Traduzir erros técnicos.
- Mapear dados legados para estruturas internas.
- Esconder detalhes XML/WSDL das camadas superiores.

A camada de aplicação deverá depender apenas da interface interna, nunca da implementação SOAP.

---

## 4. Justificativa

O padrão Adapter permite que a aplicação moderna consuma o legado por meio de uma interface própria, alinhada ao domínio da solução.

O padrão Anti-Corruption Layer impede que conceitos, nomes, estruturas e limitações do legado contaminem o modelo moderno.

Essa decisão é especialmente importante porque biometria facial é um dado sensível. A solução precisa ter controle claro sobre quais dados são expostos, como são transformados e quais informações são omitidas.

Além disso, essa separação favorece evolução futura. Se no futuro o legado SOAP for substituído por:

- Um microserviço moderno.
- Uma API REST interna.
- Um evento assíncrono.
- Um banco dedicado.
- Um serviço gRPC.
- Uma solução cloud-native.

A camada de aplicação poderá permanecer praticamente igual, alterando apenas a implementação do adapter.

---

## 5. Conceitos Aplicados

### 5.1 Adapter

O Adapter converte uma interface externa incompatível em uma interface esperada pela aplicação.

No case:

```text
Aplicação moderna espera:
consultarBiometriaPorCpf(cpf)

Legado oferece:
SOAP XML Request/Response via WSDL
```

O adapter traduz essas duas realidades.

---

### 5.2 Anti-Corruption Layer

A Anti-Corruption Layer, conceito muito utilizado em Domain-Driven Design, cria uma camada de proteção entre contextos diferentes.

No case, existem dois contextos:

```text
Contexto moderno:
REST, JSON, OAuth2, OpenAPI, logs estruturados

Contexto legado:
SOAP, XML, Oracle, contratos antigos
```

A Anti-Corruption Layer impede que o contexto legado corrompa o modelo moderno.

---

### 5.3 Ports and Adapters

A aplicação define portas, e a infraestrutura implementa adapters.

```text
Application Use Case
        ↓
BiometriaLegadoPort
        ↓
SoapBiometriaLegadoAdapter
        ↓
Legacy SOAP Service
```

A regra principal é: a aplicação conhece a porta, mas não conhece o adapter concreto.

---

### 5.4 Facade

A API REST funciona como fachada para consumidores externos.

Os parceiros enxergam uma interface simples:

```http
GET /api/v1/biometria/{cpf}
```

Mas internamente existe:

```text
REST → Use Case → Port → SOAP Adapter → SOAP Client → Legacy SOAP → H2/Oracle
```

---

## 6. Alternativas Consideradas

### 6.1 Chamar o cliente SOAP diretamente no Controller REST

Essa alternativa foi descartada.

Embora seja rápida para uma POC simples, ela mistura responsabilidades e gera forte acoplamento.

Problemas:

- Controller passa a conhecer SOAP.
- Difícil testar lógica de aplicação.
- Difícil evoluir para outra origem de dados.
- Erros SOAP podem vazar para o contrato REST.
- Viola Clean Architecture.
- Aumenta complexidade da camada HTTP.

O controller deve apenas receber a requisição REST e delegar ao caso de uso.

---

### 6.2 Chamar o cliente SOAP diretamente no Use Case

Essa alternativa também foi descartada.

O use case representa a orquestração da aplicação e deve depender de abstrações, não de detalhes de infraestrutura.

Se o use case conhecer classes geradas por SOAP, XML ou WSDL, a aplicação fica acoplada ao legado.

A decisão correta é fazer o use case depender de uma porta interna.

---

### 6.3 Usar classes SOAP como DTOs REST

Essa alternativa foi descartada com força.

Ela cria vazamento direto de modelo legado para consumidores externos.

Problemas:

- Contrato REST fica poluído por nomenclatura SOAP.
- Alterações no WSDL podem quebrar a API REST.
- Consumidores externos passam a depender do legado.
- Exposição de campos desnecessários.
- Dificuldade de aplicar versionamento limpo.
- Risco de exposição indevida de dados sensíveis.

O contrato REST deve ter DTOs próprios.

---

### 6.4 Implementar transformação no API Gateway

Essa alternativa foi descartada na ADR-002 e reforçada aqui.

O gateway não deve conhecer contrato SOAP nem fazer transformação complexa de payload.

Transformação de modelo pertence à camada de integração da Biometria Core API.

---

### 6.5 Criar um serviço separado apenas para tradução SOAP/REST

Essa alternativa foi considerada, mas não será adotada inicialmente.

Em cenários muito grandes, poderia existir um serviço especializado em integração com o legado. Porém, para esta POC, criar mais um microserviço poderia aumentar a complexidade sem ganho proporcional.

A responsabilidade de adaptação SOAP ficará dentro da Biometria Core API, em camada isolada de infraestrutura.

---

## 7. Consequências Positivas

A decisão traz os seguintes benefícios:

- Isolamento do legado SOAP.
- Proteção do domínio moderno.
- Contrato REST mais limpo.
- Menor acoplamento com WSDL/XML.
- Maior testabilidade.
- Facilidade de simular o legado em testes.
- Facilidade de substituir o legado no futuro.
- Separação clara de responsabilidades.
- Melhor aderência à Clean Architecture.
- Menor risco de vazamento de dados internos.
- Tratamento padronizado de erros.
- Evolução independente do contrato externo.
- Código mais organizado e sustentável.

---

## 8. Consequências Negativas

A decisão também adiciona complexidade:

- Mais classes e interfaces.
- Necessidade de mappers.
- Necessidade de tratar conversões.
- Curva inicial maior.
- Possibilidade de duplicação controlada de modelos.
- Necessidade de manter testes de mapeamento.

Essa complexidade é considerada aceitável porque reduz riscos maiores de acoplamento e manutenção.

---

## 9. Trade-offs

A decisão troca simplicidade inicial por sustentabilidade arquitetural.

Uma chamada direta SOAP no controller seria mais rápida, mas deixaria o projeto com aparência de integração improvisada.

O uso de Adapter e Anti-Corruption Layer demonstra maturidade, separação de responsabilidades e visão de evolução futura.

Para um case de Arquiteto de Integração, essa decisão é especialmente forte porque mostra que a solução não é apenas funcional, mas arquiteturalmente defensável.

---

## 10. Modelo de Dependência Esperado

A direção das dependências deve seguir este princípio:

```text
Controller → Application → Port ← Adapter → SOAP Client → Legacy SOAP
```

A camada de aplicação não deve depender da infraestrutura.

O adapter depende da porta, não o contrário.

Exemplo conceitual:

```text
domain
  Biometria

application
  ConsultarBiometriaUseCase

ports
  BiometriaLegadoPort

adapters.soap
  SoapBiometriaLegadoAdapter
  SoapBiometriaMapper
  SoapClientConfig
```

---

## 11. Estrutura Recomendada de Pacotes

Sugestão para a Biometria Core API:

```text
br.com.vivo.poc.biometria
  api
    BiometriaController
    dto
      BiometriaResponse
      ErrorResponse

  application
    ConsultarBiometriaUseCase
    exception
      BiometriaNaoEncontradaException
      LegadoIndisponivelException

  domain
    Biometria
    Cpf
    BiometriaStatus

  port
    BiometriaLegadoPort

  adapter
    soap
      SoapBiometriaLegadoAdapter
      SoapBiometriaMapper
      SoapBiometriaClient
      SoapFaultTranslator

  infrastructure
    config
      SoapClientConfig
      ObservabilityConfig

    error
      GlobalExceptionHandler

    logging
      CorrelationIdFilter
```

Essa estrutura pode ser ajustada conforme a implementação, mas deve preservar a intenção arquitetural.

---

## 12. Contrato REST Esperado

Endpoint inicial:

```http
GET /api/v1/biometria/{cpf}
```

Exemplo de resposta de sucesso:

```json
{
  "cpf": "***.***.***-09",
  "biometriaDisponivel": true,
  "imagemBase64": "valor-ficticio-base64",
  "origem": "LEGADO_SOAP",
  "dataConsulta": "2026-06-30T10:30:00Z"
}
```

Observação: em ambiente real, a decisão de retornar imagem Base64 diretamente deve ser cuidadosamente avaliada. Para a POC, pode ser usada com dados fictícios, mas a documentação deve destacar que biometria facial é dado sensível.

---

## 13. Contrato SOAP Fictício Esperado

Exemplo conceitual da operação SOAP:

```text
consultarBiometriaPorCpf(cpf)
```

Resposta fictícia:

```text
cpf
status
imagemBase64
dataCadastro
codigoRetorno
mensagemRetorno
```

Esses campos não devem vazar diretamente para o contrato REST.

O adapter deve converter a resposta para o modelo interno.

---

## 14. Estratégia de Mapeamento

A solução deverá possuir mapeamento explícito entre:

```text
SOAP Response → Modelo Interno → REST Response
```

O mapeamento não deve ser espalhado pelo código.

Deve existir um mapper dedicado, por exemplo:

```java
class SoapBiometriaMapper {
    Biometria toDomain(SoapBiometriaResponse response) {
        // conversão
    }
}
```

Esse mapper será responsável por:

- Converter status legado.
- Normalizar mensagens.
- Omitir campos internos.
- Converter datas.
- Validar presença de dados obrigatórios.
- Interpretar códigos de retorno.
- Proteger o domínio contra inconsistências do legado.

---

## 15. Estratégia de Tratamento de Erros

O adapter deve traduzir erros SOAP em exceções internas.

Exemplos:

| Erro SOAP/Legado | Exceção Interna | HTTP Final |
|---|---|---|
| CPF não encontrado | BiometriaNaoEncontradaException | 404 |
| SOAP Fault técnico | LegadoIndisponivelException | 502 |
| Timeout | LegadoTimeoutException | 504 |
| Resposta inválida | RespostaLegadoInvalidaException | 502 |
| CPF inválido | CpfInvalidoException | 400 |

O controller não deve conhecer SOAP Fault diretamente.

O Global Exception Handler deve converter exceções internas em respostas REST padronizadas.

---

## 16. Impacto na Segurança

A Anti-Corruption Layer também contribui para segurança.

Ela permite controlar exatamente quais campos do legado serão expostos.

Campos internos, técnicos ou sensíveis não necessários devem ser omitidos.

Exemplos de informações que não devem ser expostas:

- ID interno do legado.
- Nome de tabela.
- Código técnico Oracle.
- Mensagens internas de exceção.
- Stack traces.
- Dados biométricos além do necessário.
- Metadados sensíveis.
- Detalhes do WSDL.

Além disso, o adapter deve evitar logar payloads SOAP completos.

---

## 17. Impacto na Observabilidade

O adapter é um ponto crítico de observabilidade.

Ele deve registrar eventos técnicos como:

- Início da chamada ao legado.
- Fim da chamada ao legado.
- Tempo de resposta do SOAP.
- Status técnico da chamada.
- Tipo de erro, se houver.
- Correlation ID.
- CPF mascarado.
- Nome lógico do serviço chamado.

Não deve registrar:

- XML SOAP completo com dados sensíveis.
- Imagem facial.
- Token JWT.
- CPF completo.
- Stack trace em resposta ao consumidor.

---

## 18. Impacto na Resiliência

A existência de uma porta e adapter facilita a aplicação futura de mecanismos de resiliência.

O adapter pode ser envolvido por:

- Timeout.
- Retry controlado.
- Circuit breaker.
- Fallback técnico.
- Métricas de latência.
- Bulkhead.

Para a POC, recomenda-se ao menos documentar:

- Timeout para chamada SOAP.
- Tratamento de indisponibilidade.
- Tradução para 502/504.
- Logs claros para diagnóstico.

Retry deve ser usado com cuidado. Como a operação inicial é uma consulta, retry limitado pode ser aceitável, mas ainda assim deve ser controlado para não sobrecarregar o legado.

---

## 19. Impacto no Versionamento

O Adapter permite que mudanças no contrato SOAP não necessariamente impactem a API REST.

Se o WSDL mudar, o impacto poderá ficar restrito à camada de adapter e mapper.

Se a API REST precisar mudar, isso será tratado por versionamento externo:

```text
/api/v1/biometria/{cpf}
/api/v2/biometria/{cpf}
```

A Anti-Corruption Layer ajuda a preservar a estabilidade do contrato público.

---

## 20. Impacto na Evolução Futura

Essa decisão prepara a arquitetura para migração gradual.

Cenários futuros possíveis:

### 20.1 Substituir SOAP por REST interno

```text
BiometriaLegadoPort
  ← RestBiometriaLegadoAdapter
```

### 20.2 Substituir SOAP por microserviço pleno

```text
BiometriaLegadoPort
  ← BiometriaServiceAdapter
```

### 20.3 Substituir consulta síncrona por cache ou réplica

```text
BiometriaLegadoPort
  ← CachedBiometriaAdapter
```

### 20.4 Substituir origem por evento/materialized view

```text
BiometriaLegadoPort
  ← BiometriaReadModelAdapter
```

Em todos os casos, a camada de aplicação poderá continuar utilizando a mesma porta.

---

## 21. Impacto nos Diagramas C4

### 21.1 C1 — System Context

No C1, essa decisão aparece indiretamente. O parceiro externo consome a solução moderna, e o legado permanece como sistema interno.

### 21.2 C2 — Container Diagram

No C2, a Biometria Core API aparece como container intermediário entre gateway e legado SOAP.

A relação deve deixar claro:

```text
Biometria Core API → Legacy SOAP Service
```

com protocolo SOAP interno.

### 21.3 C3 — Component Diagram

No C3, essa decisão aparece de forma explícita.

Componentes esperados dentro da Biometria Core API:

- BiometriaController.
- ConsultarBiometriaUseCase.
- BiometriaLegadoPort.
- SoapBiometriaLegadoAdapter.
- SoapBiometriaMapper.
- GlobalExceptionHandler.
- CorrelationIdFilter.

O C3 deve evidenciar que o use case depende da porta e não do client SOAP diretamente.

---

## 22. Regras de Implementação

A implementação deve seguir as regras abaixo:

1. Nenhuma classe SOAP deve ser usada no controller.
2. Nenhuma classe SOAP deve ser usada diretamente no use case.
3. Nenhum SOAP Fault deve vazar para a resposta REST.
4. Nenhum XML SOAP deve ser retornado ao consumidor.
5. O contrato REST deve ter DTOs próprios.
6. O domínio deve ter objetos próprios.
7. O adapter deve concentrar a chamada SOAP.
8. O mapper deve concentrar a tradução SOAP → domínio.
9. O handler global deve concentrar a tradução exceção → HTTP.
10. Logs devem mascarar CPF e não registrar biometria.

---

## 23. Exemplo de Fluxo

Fluxo esperado:

```text
1. Parceiro chama GET /api/v1/biometria/{cpf}
2. Gateway valida JWT.
3. Gateway propaga X-Correlation-Id.
4. Core recebe requisição.
5. Controller valida formato básico.
6. Use case recebe CPF.
7. Use case chama BiometriaLegadoPort.
8. SoapBiometriaLegadoAdapter monta requisição SOAP.
9. Legacy SOAP retorna dados fictícios.
10. Mapper converte resposta SOAP para modelo interno.
11. Use case monta resposta.
12. Controller retorna JSON padronizado.
13. Logs registram fluxo sem dados sensíveis.
```

---

## 24. Critérios de Aceite da Decisão

A decisão será considerada corretamente implementada quando:

- Existir uma porta interna para consulta ao legado.
- Existir um adapter SOAP implementando essa porta.
- O controller não conhecer classes SOAP.
- O use case não conhecer classes SOAP.
- DTOs REST forem independentes do contrato SOAP.
- Existir mapper dedicado SOAP → domínio.
- Erros SOAP forem traduzidos para exceções internas.
- Exceções internas forem traduzidas para HTTP padronizado.
- Logs do adapter forem estruturados e seguros.
- O C3 mostrar claramente a separação entre use case, port e adapter.
- Testes unitários conseguirem mockar a porta sem subir SOAP.

---

## 25. Checklist da Implementação

### Documentação

- [ ] Criar `docs/adr/ADR-003-adapter-anti-corruption-layer.md`.
- [ ] Referenciar esta ADR no `docs/architecture/DAS.md`.
- [ ] Atualizar README com a decisão de isolamento do legado.
- [ ] Refletir a porta e o adapter no diagrama C3.
- [ ] Documentar os padrões Adapter e Anti-Corruption Layer.
- [ ] Documentar a estratégia de tradução de erros.

### Código — Core API

- [ ] Criar pacote `domain`.
- [ ] Criar entidade/modelo `Biometria`.
- [ ] Criar value object `Cpf`, se fizer sentido.
- [ ] Criar pacote `application`.
- [ ] Criar `ConsultarBiometriaUseCase`.
- [ ] Criar pacote `port`.
- [ ] Criar interface `BiometriaLegadoPort`.
- [ ] Criar pacote `adapter.soap`.
- [ ] Criar `SoapBiometriaLegadoAdapter`.
- [ ] Criar `SoapBiometriaMapper`.
- [ ] Criar `SoapFaultTranslator`.
- [ ] Criar DTOs REST próprios.
- [ ] Criar handler global de exceções.
- [ ] Garantir que classes SOAP não vazem para controller/use case.

### Código — Legacy SOAP

- [ ] Criar contrato SOAP fictício.
- [ ] Criar resposta com dados fictícios.
- [ ] Criar cenários de CPF encontrado.
- [ ] Criar cenário de CPF não encontrado.
- [ ] Criar cenário de falha técnica.
- [ ] Criar H2 simulando tabela Oracle.

### Testes

- [ ] Testar use case com porta mockada.
- [ ] Testar mapper SOAP → domínio.
- [ ] Testar tradução de erro SOAP.
- [ ] Testar controller sem dependência SOAP.
- [ ] Testar CPF não encontrado.
- [ ] Testar falha do legado.
- [ ] Testar resposta inválida do legado.
- [ ] Testar que o contrato REST não contém campos técnicos SOAP.

---

## 26. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Vazamento de modelo SOAP para REST | Alto | DTOs REST próprios e mapper dedicado |
| Use case acoplado ao SOAP | Alto | Dependência apenas da porta |
| Excesso de lógica no adapter | Médio | Separar mapper e translator |
| Mapper incompleto | Médio | Testes unitários de mapeamento |
| Logs com payload SOAP sensível | Alto | Política de logging seguro |
| Erros técnicos expostos ao consumidor | Alto | Global Exception Handler |
| Crescimento desorganizado de pacotes | Médio | Estrutura por responsabilidade |

---

## 27. Decisão Final

A solução adotará Adapter e Anti-Corruption Layer para proteger o domínio moderno da Biometria Core API contra detalhes do sistema legado SOAP.

A comunicação com o legado será realizada por uma porta interna e uma implementação SOAP isolada em infraestrutura.

Essa decisão reduz acoplamento, melhora testabilidade, evita vazamento de detalhes técnicos e prepara a arquitetura para evolução futura sem ruptura do contrato REST.

---

## 28. Próximos Passos

Após esta ADR, os próximos passos recomendados são:

1. Criar ADR-004 sobre autenticação e autorização com OAuth2/JWT e Keycloak.
2. Atualizar o DAS com as decisões ADR-001, ADR-002 e ADR-003.
3. Criar os diagramas C1, C2 e C3.
4. Criar a estrutura inicial dos módulos.
5. Implementar primeiro o Legacy SOAP fake.
6. Implementar depois a Biometria Core API com porta e adapter.
7. Implementar por fim o Gateway e segurança.

