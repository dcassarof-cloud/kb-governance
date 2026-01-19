# Modelo de Dados — kb-governance

Este documento descreve o modelo de dados do projeto **kb-governance**, responsável por armazenar e governar os artigos da Knowledge Base (KB) sincronizados a partir do Movidesk.

---

## 🗃️ Tabela: kb_article

Tabela principal do sistema, responsável por armazenar os artigos da KB com conteúdo completo e metadados.

### Estrutura da tabela

| Campo | Tipo | Descrição |
|------|------|----------|
| id | BIGINT (PK) | ID do artigo no Movidesk |
| title | TEXT | Título do artigo |
| slug | TEXT | Slug do artigo |
| article_status | INTEGER | Status do artigo no Movidesk |
| summary | TEXT | Resumo do artigo |
| content_html | TEXT | Conteúdo HTML |
| content_text | TEXT | Conteúdo em texto puro |
| revision_id | BIGINT | Identificador da revisão |
| reading_time | TEXT | Tempo estimado de leitura |
| created_date | TIMESTAMP WITH TIME ZONE | Data de criação no Movidesk |
| updated_date | TIMESTAMP WITH TIME ZONE | Data da última atualização |
| fetched_at | TIMESTAMP WITH TIME ZONE | Data/hora da sincronização |
| source_url | TEXT | URL original do artigo |
| source_system | TEXT | Sistema de origem (ex: movidesk) |

---

## 🔑 Chave Primária

- O campo `id` utiliza o **ID do artigo no Movidesk**
- Não há geração automática de ID (`@GeneratedValue`)
- Garante unicidade e evita duplicação de artigos

---

## 📌 Regras de Persistência

- Cada sincronização:
    - insere um novo artigo **ou**
    - atualiza o existente com base no `id`
- O campo `fetched_at` indica quando o artigo foi sincronizado pela última vez
- Os campos `created_date` e `updated_date` refletem os valores originais do Movidesk

---

## 🧠 Decisões de Modelagem

- Conteúdo HTML e texto são armazenados separadamente
- Datas utilizam `OffsetDateTime` para preservar timezone
- O banco local é tratado como **fonte de governança**, não apenas cache
- A origem do artigo é explicitamente registrada (`source_system`)

---

## 🔮 Evoluções Planejadas no Modelo

### 1️⃣ Classificação por sistema/módulo
Criação de uma tabela adicional para relacionar artigos a sistemas internos.

Exemplo:
- Quinto Eixo
- SGRH
- NotaOn

Tabela futura:
- `kb_system`
- Relacionamento: `kb_article.system_id`

---

### 2️⃣ Governança e versionamento
- Histórico de alterações de conteúdo
- Status interno de governança
- Auditoria de mudanças

---

### 3️⃣ Otimizações futuras
- Índices por `updated_date`
- Índices por sistema/módulo
- Consultas full-text no conteúdo

---

## 📎 Observação Final

Este modelo foi projetado para suportar:
- crescimento da base de conhecimento
- consultas rápidas
- automações futuras
- uso de inteligência artificial sobre o conteúdo

O banco de dados deixa de ser apenas persistência e passa a ser **base estratégica de conhecimento**.
