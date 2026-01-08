# Arquitetura do Projeto KB Governance

Este documento descreve a arquitetura, responsabilidades e fluxo das classes do projeto **Kb Governance**, responsável por integrar e governar artigos da base de conhecimento do Movidesk.


## 🧱 Visão Geral

Arquitetura em camadas:

Controller  
↓  
Service  
↓  
Client (Movidesk API)

Persistência:

Service  
↓  
Repository (JPA)  
↓  
PostgreSQL

---

## 📦 Responsabilidade das Camadas

### Controller
- Exposição de endpoints REST
- Disparo de sincronizações

### Service
- Regras de negócio
- Orquestração de sync
- Paginação
- Tratamento de erros

### Client
- Comunicação HTTP com Movidesk
- Busca paginada
- Busca detalhada por ID

### Repository
- Persistência dos dados

---

## 🔄 Fluxo de Sincronização

1. Busca lista paginada de artigos
2. Para cada item:
    - Busca artigo completo
    - Converte dados
    - Salva no banco
3. Repete até o fim das páginas


## 1) KbGovernanceApplication

### O que é
Classe principal do projeto e **ponto de entrada do Spring Boot**.

### Responsabilidade
- Iniciar a aplicação (`main`)
- Subir o servidor embutido
- Carregar o contexto do Spring
- Escanear componentes anotados com:
    - `@Service`
    - `@Repository`
    - `@RestController`
    - `@Configuration`

### Observação
Nenhuma regra de negócio deve existir aqui.  
Essa classe apenas **liga a aplicação**.

---

## 2) RestClientConfig

### O que é
Classe de configuração (`@Configuration`) responsável por criar e configurar o cliente HTTP usado para acessar a API do Movidesk.

### Responsabilidades
- Criar um `RestClient` (Spring 6) ou `WebClient`
- Definir a `baseUrl` do Movidesk
- Configurar headers padrão:
    - `Accept: application/json`
    - `Content-Type: application/json`
    - Token de autenticação
- Configurar timeout de conexão e leitura
- Configurar logging de requisições e respostas

### Por que existe
Centralizar configurações HTTP e evitar duplicação de código dentro do `MovideskClient`.

### Visão Consisa 2026
- Timeouts obrigatórios
- Retry com backoff
- Circuit breaker
- Logs estruturados (sem vazar token)
- Rate limit ou fila para evitar bloqueios do Movidesk

---

## 3) MovideskClient

### O que é
Camada **gateway** responsável por realizar a comunicação HTTP com a API do Movidesk.

### Responsabilidades
- Montar endpoints e parâmetros (`take`, `skip`, filtros)
- Enviar requisições HTTP
- Converter JSON em DTOs Java
- Tratar erros HTTP:
    - `401 / 403` → autenticação/permissão
    - `429` → rate limit
    - `5xx` → instabilidade externa

### Regra importante
Nenhuma regra de negócio deve existir aqui.  
Essa classe apenas **fala HTTP**.

### Analogia
Funciona como um **motoboy**: busca os dados fora e entrega para o sistema.

---

## 4) DTOs do Movidesk (`client.movidesk`)

DTO (Data Transfer Object) representa o formato do JSON retornado pela API do Movidesk.

---

### 4.1) MovideskArticleDto

#### O que é
DTO que representa um artigo completo retornado pelo Movidesk.

#### Campos comuns
- `id`
- `subject`
- `content` / `html`
- `createdDate`
- `updatedDate`
- `category`
- `tags`
- `status`

#### Uso
Recebido pelo `MovideskClient` e convertido em `KbArticle` no service.

---

### 4.2) MovideskArticleSearchItemDto

#### O que é
DTO resumido usado em listagens e buscas.

#### Campos comuns
- `id`
- `subject`
- `snippet`
- `score`
- `category`

#### Uso
Utilizado para paginação e telas de consulta.

---

### 4.3) MovideskArticleSearchResponse

#### O que é
Envelope da resposta de busca.

#### Conteúdo
- `items`: lista de `MovideskArticleSearchItemDto`
- `total`: total de registros
- Dados de paginação (`skip`, `take`, `hasMore`)

#### Uso
Facilita paginação e controle de resultados.

---

### 4.4) MovideskMenuDto

#### O que é
DTO que representa a estrutura de menu/categorias/sistemas do Movidesk.

#### Uso
Mapear menu → `KbSystem`, permitindo classificar artigos por módulo (Fiscal, Financeiro, etc).

---

## 5) Camada Domain

Camada responsável pelas **entidades persistidas no banco de dados**.

---

### 5.1) KbArticle

#### O que é
Entidade que representa um artigo governado internamente.

#### Responsabilidade
- Armazenar dados relevantes para governança
- Servir como fonte de verdade interna

#### Campos comuns
- `id`
- `movideskId`
- `title`
- `contentHtml`
- `systemId`
- `lastSyncAt`
- `sourceUpdatedAt`
- `status`
- `hash`

#### Visão Consisa 2026
- Versionamento de conteúdo
- Auditoria de alterações

---

### 5.2) KbSystem

#### O que é
Entidade que representa sistemas ou módulos (ConsisaNet Fiscal, Financeiro, etc).

#### Responsabilidade
- Catalogar sistemas
- Permitir classificação e filtro de artigos

#### Campos comuns
- `id`
- `name`
- `slug`
- `source`
- `parentId`

---

## 6) Camada Repository

Camada de acesso ao banco via Spring Data JPA.

---

### 6.1) KbArticleRepository

#### O que é
Interface `JpaRepository<KbArticle, Long>`.

#### Responsabilidade
- Persistir e consultar artigos

#### Exemplos de métodos
- `findByMovideskId(Long movideskId)`
- `findBySystemId(Long systemId, Pageable pageable)`

---

### 6.2) KbSystemRepository

#### O que é
Interface `JpaRepository<KbSystem, Long>`.

#### Responsabilidade
- CRUD de sistemas
- Consultas por nome ou slug
- Montagem de hierarquia

---

## 7) Camada Service

Camada onde reside a **regra de negócio**.

---

### KbArticleSyncService

#### O que é
Serviço responsável por sincronizar artigos do Movidesk com o banco interno.

#### Fluxo de sincronização
1. Recebe o `articleId`
2. Busca o artigo via `MovideskClient`
3. Converte DTO → `KbArticle`
4. Verifica existência no banco
5. Executa upsert (insert ou update)
6. Atualiza metadados de sincronização
7. Retorna o artigo salvo

#### Não deve conter
- Código de controller
- SQL manual
- Lógica de HTTP

#### Visão Consisa 2026
- Sync em lote com paginação
- Jobs agendados
- Idempotência
- Comparação por hash
- Logs e auditoria

---

## 8) Camada Controller

---

### KbArticleController

#### O que é
API REST do sistema.

#### Responsabilidade
- Receber requisições HTTP
- Validar parâmetros
- Chamar serviços
- Retornar respostas HTTP adequadas

#### Exemplos de endpoints
- `POST /kb/articles/{id}/sync`
- `GET /kb/articles/test-list?take=25&skip=0`

#### Boa prática
Controllers não devem conhecer detalhes do Movidesk.  
Eles expõem endpoints do **domínio interno**.

---

## Fluxo Geral da Aplicação

- `KbArticleController` → `KbArticleSyncService`
- `KbArticleSyncService` → `MovideskClient`
- `MovideskClient` → `RestClient` (configurado em `RestClientConfig`)
- `KbArticleSyncService` → `KbArticleRepository`
- `KbArticle` → relacionamento com `KbSystem`
- DTOs do Movidesk existem apenas para transporte de dados

---
