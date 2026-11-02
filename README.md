# Microsserviço de Pedidos

Microsserviço desenvolvido em Java e Spring Boot como parte do projeto final de um curso de capacitação profissional, com foco em segurança de APIs e arquitetura de microsserviços.

## Objetivo

Disponibilizar um endpoint REST para consulta de pedidos, integrado ao Gateway Seguro.

O Gateway realiza a autenticação JWT, a autorização baseada em papéis (RBAC) e o controle de requisições (*rate limiting*) antes de encaminhar as chamadas ao microsserviço.

O microsserviço realiza uma validação adicional da credencial interna, garantindo que a requisição encaminhada possua uma assinatura HMAC-SHA256 válida.

## Arquitetura

O projeto utiliza duas aplicações independentes:

- **Gateway Seguro:** HTTPS, porta 8443.
- **Microsserviço de Pedidos:** HTTP, porta 8081.

O microsserviço está configurado para escutar somente no endereço `127.0.0.1`, restringindo o acesso à interface local.

O fluxo de comunicação é:

```text
Cliente
   |
   | HTTPS + JWT
   v
Gateway Seguro (8443)
   |
   | Validação JWT
   | Autorização RBAC
   | Rate Limiting
   | Geração de credencial HMAC-SHA256
   v
Microsserviço de Pedidos (8081)
   |
   | Validação HMAC-SHA256
   | Validação de timestamp
   | Validação de método HTTP e rota
   | Proteção contra replay
   v
Resposta JSON
```

## Endpoint

`GET /api/pedidos`

O endpoint retorna uma lista de pedidos fictícios, utilizados para demonstrar a integração entre os serviços.

## Controle de acesso

O microsserviço exige dois cabeçalhos:

- `X-Gateway-Authenticated: true`
- `X-Gateway-Token: <credencial HMAC>`

O cabeçalho `X-Gateway-Authenticated` não é suficiente para autorizar o acesso. A credencial criptográfica também precisa ser válida.

### Autenticação interna com HMAC-SHA256

O Gateway gera uma credencial com três componentes:

```text
timestamp.nonce.assinatura
```

**Timestamp:** representa o instante de geração da credencial, em segundos desde a época Unix.

**Nonce:** valor aleatório de 128 bits, gerado para identificar cada credencial.

**Assinatura:** resultado do algoritmo HMAC-SHA256 utilizando uma chave secreta compartilhada entre o Gateway e o microsserviço.

A mensagem autenticada pela assinatura contém:

```text
timestamp
nonce
metodo_http
rota
```

Os componentes são concatenados utilizando uma quebra de linha (`\n`) como separador.

Para a operação implementada, o método e a rota são:

```text
GET
/api/pedidos
```

Essa abordagem vincula a credencial à operação para a qual foi gerada.

### Validação da credencial

Ao receber uma requisição, o microsserviço verifica:

1. Se o cabeçalho `X-Gateway-Authenticated` possui o valor `true`.
2. Se a credencial `X-Gateway-Token` está presente.
3. Se a credencial contém os três componentes esperados.
4. Se o nonce possui 128 bits.
5. Se o timestamp está dentro do prazo de validade de 30 segundos e não representa um instante futuro.
6. Se a assinatura HMAC-SHA256 é válida.
7. Se a assinatura corresponde ao método HTTP e à rota esperados.
8. Se a credencial ainda não foi utilizada.

Requisições que não atendem a essas verificações recebem **HTTP 403 — Forbidden**.

### Proteção contra ataques de repetição (replay attacks)

O microsserviço mantém um registro em memória dos identificadores de credenciais utilizadas.

Após a validação criptográfica, o identificador é registrado utilizando `ConcurrentHashMap.putIfAbsent()`.

Essa operação permite que apenas a primeira utilização de uma mesma credencial seja aceita, inclusive diante de tentativas simultâneas.

Uma segunda utilização é rejeitada, mesmo que a assinatura ainda esteja dentro do prazo de validade.

Os registros expirados são removidos periodicamente por uma rotina agendada com Spring Scheduling, evitando seu acúmulo indefinido durante a operação normal.

**Limitação:** o armazenamento é local à instância do microsserviço. Em uma arquitetura distribuída com múltiplas instâncias, seria necessário utilizar um armazenamento compartilhado, como Redis, para garantir a proteção contra replay entre todas elas.

## Configuração da chave secreta

O Gateway e o microsserviço devem utilizar a mesma variável de ambiente:

`GATEWAY_INTERNAL_SECRET`

Seu valor deve ser uma chave aleatória de pelo menos 256 bits, codificada em Base64.

No arquivo `application.properties`, a configuração é:

```properties
gateway.internal.secret=${GATEWAY_INTERNAL_SECRET}
```

A chave secreta real não deve ser armazenada no código-fonte nem publicada no GitHub.

## Pré-requisitos

- Java compatível com a versão configurada no `pom.xml`.
- Maven Wrapper incluído no projeto.
- Variável de ambiente `GATEWAY_INTERNAL_SECRET` configurada.
- Gateway Seguro em execução para os testes de integração.

## Executar os testes

No PowerShell, na raiz do projeto:

```powershell
.\mvnw.cmd clean test
```

Resultado obtido em 02/11/2026:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os testes automatizados incluem:

- Inicialização do contexto Spring Boot.
- Aceitação de credencial HMAC válida.
- Rejeição de assinatura adulterada.
- Rejeição de credencial expirada.
- Rejeição de timestamp futuro.
- Rejeição de credencial ausente.
- Rejeição de credencial reutilizada.
- Rejeição de credencial vinculada a outro método HTTP.
- Rejeição de credencial vinculada a outra rota.
- Verificação de que uma tentativa inválida não consome o nonce de uma credencial legítima.
- Rejeição de credencial no formato antigo.

## Executar o microsserviço

No PowerShell, na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

O microsserviço ficará disponível em:

```text
http://127.0.0.1:8081
```

## Testes manuais

### Acesso direto sem cabeçalhos

```powershell
curl.exe -i http://127.0.0.1:8081/api/pedidos
```

**Resultado obtido:** HTTP 403.

```json
{
  "erro": "ACESSO_NEGADO",
  "mensagem": "Credencial interna ausente ou invalida"
}
```

### Tentativa de falsificação da autenticação interna

```powershell
curl.exe -i `
  -H "X-Gateway-Authenticated: true" `
  -H "X-Gateway-Token: token.falso.assinatura" `
  http://127.0.0.1:8081/api/pedidos
```

**Resultado obtido:** HTTP 403.

Esse teste demonstra que o simples envio do indicador de autenticação e de uma credencial falsa não é suficiente para acessar o microsserviço.

### Acesso pelo Gateway Seguro

Com as duas aplicações em execução e um JWT válido com a permissão `USER`:

```powershell
$token = Read-Host "Cole o JWT valido"

curl.exe -k -i `
  -H "Authorization: Bearer $token" `
  https://localhost:8443/api/pedidos
```

**Resultado obtido:** HTTP 200, com a lista de pedidos.

A opção `-k` é utilizada exclusivamente no ambiente local para aceitar o certificado HTTPS autoassinado.

## Considerações de segurança

A implementação incorpora mecanismos de proteção em diferentes etapas da comunicação:

- Autenticação JWT e autorização RBAC no Gateway.
- Controle de requisições no Gateway.
- Autenticação interna com HMAC-SHA256.
- Credenciais com validade máxima de 30 segundos.
- Nonce aleatório de 128 bits.
- Assinatura vinculada ao método HTTP e à rota.
- Proteção contra reutilização de credenciais.
- Comparação de assinaturas com `MessageDigest.isEqual()`.
- Remoção periódica de registros de credenciais expiradas.

### Limitações e melhorias para produção

Apesar dos mecanismos implementados, o projeto possui limitações:

- A comunicação interna utiliza HTTP na interface local, sem criptografia de transporte nesse trecho.
- A proteção contra replay utiliza armazenamento em memória, restrito a uma instância.
- A assinatura está vinculada ao método HTTP e à rota, mas não ao conteúdo do corpo de uma requisição.
- A segurança da autenticação interna depende do sigilo da chave compartilhada.
- O armazenamento em memória não possui limite máximo explícito de entradas, embora exista limpeza periódica.

Para ambientes de produção, recomenda-se TLS ou mTLS entre os serviços, isolamento de rede, gerenciamento seguro e rotação de chaves, armazenamento compartilhado de nonces e mecanismos de controle de capacidade.

## Considerações finais

O Microsserviço de Pedidos demonstra a integração segura entre aplicações Spring Boot, combinando autenticação externa por JWT com autenticação interna baseada em HMAC-SHA256.

A implementação foi validada por **11 testes automatizados aprovados**, além de testes manuais de comunicação com o Gateway e de bloqueio de acessos não autorizados.

O projeto integra a atividade final de um curso de capacitação profissional e constitui uma implementação demonstrativa de segurança entre microsserviços, não uma solução pronta para implantação em produção sem adaptações adicionais.