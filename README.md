# kb-governance

Projeto de **governança da Knowledge Base (KB)** com integração à API do Movidesk, desenvolvido em **Java 21 + Spring Boot**, com persistência em **PostgreSQL**.

O objetivo é centralizar os artigos da base de conhecimento em um banco local, garantindo **histórico, rastreabilidade, controle e base para futuras automações**.

---

## 📌 Contexto e Motivação

Atualmente, os artigos da Knowledge Base estão armazenados exclusivamente no Movidesk.  
Apesar de funcionais, esse modelo traz limitações para:

- Governança do conhecimento
- Histórico de versões
- Análises e relatórios
- Integrações futuras
- Automação e uso de IA
- Redução de dependência direta da API externa

Este projeto nasce para **resolver essas lacunas**, criando uma camada local de controle e evolução do conhecimento.

---

##  Objetivos do Projeto

### Objetivo principal
- Sincronizar os artigos da Knowledge Base do Movidesk para um banco local.

### Objetivos específicos
- Persistir conteúdo HTML e texto
- Armazenar datas de criação e atualização
- Registrar origem do artigo (source system)
- Manter histórico de sincronização
- Permitir consultas locais sem depender da API externa

### Visão futura
- Sync incremental (somente artigos alterados)
- Classificação por sistema/módulo
- Governança de status interno
- Versionamento de conteúdo
- Scheduler configurável
- Base para automações e IA

---

## Stack Tecnológica

- **Java 21**
- **Spring Boot**
- Spring Web (REST)
- Spring Data JPA
- **PostgreSQL**
- **Flyway** (migrations)
- **RestClient** (integração HTTP)
- **Jackson** (JSON)
- **Maven**
- **Postman** (testes manuais)
- **IntelliJ IDEA**

---

> "Consagre ao Senhor tudo o que voçê faz e os seus planos serão bem sucedidos."
> |Provébios 16:3|
