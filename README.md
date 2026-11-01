
# Microsserviço de Pedidos

Microsserviço desenvolvido em Java e Spring Boot como
parte do projeto acadêmico Gateway de API Seguro para
Microserviços.

## Objetivo

Disponibilizar um endpoint REST de consulta de pedidos,
integrado ao Gateway Seguro.

O Gateway realiza a autenticação JWT, a autorização
RBAC e o controle de requisições antes de encaminhar
as chamadas ao microsserviço.

## Arquitetura

O projeto utiliza duas aplicações independentes:

- Gateway Seguro: HTTPS, porta 8443.
- Microsserviço de Pedidos: HTTP, porta 8081.

O microsserviço escuta somente no endereço 127.0.0.1,
restringindo o acesso à interface local.

## Endpoint

GET /api/pedidos

O endpoint retorna uma lista de pedidos fictícios,
utilizados para demonstrar a integração.

## Controle de acesso

O microsserviço exige o cabeçalho:

    X-Gateway-Authenticated: true

Na ausência desse cabeçalho ou quando seu valor
é diferente de true, o acesso é negado com HTTP 403.

Esse cabeçalho é apenas um indicador de encaminhamento.
Ele pode ser falsificado por um processo com acesso
à porta do microsserviço.

Em produção, seriam necessários controles adicionais,
como isolamento de rede e autenticação mútua TLS (mTLS).

## Pré-requisitos

- Java compatível com a versão configurada no pom.xml.
- Maven Wrapper incluído no projeto.

## Executar os testes

No PowerShell, na raiz do projeto:

    .\mvnw.cmd test

Resultado obtido:

    Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS

## Executar o microsserviço

No PowerShell, na raiz do projeto:

    .\mvnw.cmd spring-boot:run

O microsserviço ficará disponível em:

    http://127.0.0.1:8081

## Testes manuais

### Acesso sem cabeçalho

    curl.exe -i http://127.0.0.1:8081/api/pedidos

Resultado esperado: HTTP 403.

### Acesso com cabeçalho

    curl.exe -i -H "X-Gateway-Authenticated: true" http://127.0.0.1:8081/api/pedidos

Resultado esperado: HTTP 200, com a lista de pedidos.

Esse segundo teste demonstra uma limitação conhecida
do protótipo: o cabeçalho pode ser falsificado.

### Acesso pelo Gateway Seguro

Com as duas aplicações em execução e um JWT válido
com a permissão USER:

    $token = Read-Host "Cole o JWT valido"

    curl.exe -k -i -H "Authorization: Bearer $token" https://localhost:8443/api/pedidos

Resultado esperado: HTTP 200.

A opção -k é utilizada somente para o certificado
HTTPS autoassinado do ambiente local.

## Considerações finais

O microsserviço demonstra o encaminhamento de
requisições autenticadas pelo Gateway.

A implementação é um protótipo acadêmico e não
deve ser considerada uma solução completa de
autenticação entre serviços para produção.
