# API Local — kb-governance

Documentação dos endpoints expostos pela aplicação.

---

## 🔹 GET /kb/articles/test-list

Endpoint utilizado para validar a paginação da API do Movidesk.

### Parâmetros
- `page` (default: 0)
- `pageSize` (default: 30)

### Exemplo

GET /kb/articles/test-list?page=0&pageSize=30

### Retorno
```json
{
  "page": 0,
  "pageSize": 30,
  "count": 30,
  "totalSize": 1103,
  "firstId": 33964,
  "lastId": 92800
}

POST /kb/articles/sync-all

Dispara a sincronização completa da KB.

Exemplo
POST /kb/articles/sync-all

Uso

Execução manual

Execução sob demanda


---

# 🧩 PARTE 4 — `docs/database.md` (Banco de Dados)

```md
# Modelo de Dados — kb-governance

Descrição da estrutura de dados utilizada no projeto.

---

## 🗃️ Tabela: kb_article

Tabela responsável por armazenar os artigos da KB.

### Campos

| Campo | Tipo | Descrição |
|------|------|----------|
| id | BIGINT | ID do artigo no Movidesk |
| title | TEXT | Título |
| slug | TEXT | Slug |
| article_status | INTEGER | Status do artigo |
| summary | TEXT | Resumo |
| content_html | TEXT | Conteúdo HTML |
| content_text | TEXT | Conteúdo texto |
| revision_id | BIGINT | Revisão |
| reading_time | TEXT | Tempo de leitura |
| created_date | TIMESTAMP | Data de criação |
| updated_date | TIMESTAMP | Data de atualização |
| fetched_at | TIMESTAMP | Data do sync |
| source_url | TEXT | URL original |
| source_system | TEXT | Origem (movidesk) |

---

## 📌 Decisões de Modelagem

- ID do Movidesk como chave primária
- Conteúdo HTML e texto separados
- Datas com timezone

## Retorno do Menu 

"Menu consisanet"
"Consisanet - Escritório"
"Consisanet - Protocolos"
"Consisanet - Caixa"
"Consisanet - Faturamento"
"Consisanet - Inventários "
"Consisanet - Patrimônio"
"Consisanet - Cereais"
"Consisanet - DARFs"
"Consisanet - Financeiro"
"Consisanet - Fiscal"
"Consisanet - Contabilidade" 
"Consisanet - Faturamento"
"Menu Biojob"
"Menu SGRH "
"Menu notaon"
"Conta Shop"
"Açor"
"Ordena"
"Menu Quinto Eixo"
"Edoc"
"Captura"
"CLOUD/EDI"

Módulos “Utilitários” e “Indústria” do ConsisaNet não possuem manuais e possuem o retorno  "Menu consisanet" ou NULL
