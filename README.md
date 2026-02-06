# kb-governance

Projeto de **governança da Knowledge Base (KB)** com integração à **API do Movidesk**, desenvolvido em **Java 21 + Spring Boot**, com persistência em **PostgreSQL**.

O objetivo é **centralizar os artigos da base de conhecimento em um banco local**, garantindo **histórico, rastreabilidade, controle** e servindo como base para **futuras automações**.

---

## Contexto e Motivação

Atualmente, os artigos da Knowledge Base estão armazenados exclusivamente no Movidesk.  
Apesar de funcionais, esse modelo apresenta limitações relacionadas a:

- Governança do conhecimento
- Histórico de versões
- Análises e relatórios
- Integrações futuras
- Automação e uso de IA
- Dependência direta da API externa

Este projeto surge para resolver essas lacunas, criando uma **camada local de controle, governança e evolução do conhecimento**.

---

## Objetivos do Projeto

### Objetivo principal
- Sincronizar os artigos da Knowledge Base do Movidesk para um banco de dados local.

### Objetivos específicos
- Persistir conteúdo HTML e texto
- Armazenar datas de criação e atualização
- Registrar a origem do artigo (source system)
- Manter histórico de sincronização
- Permitir consultas locais sem depender da API externa

---

## Visão Futura

- Sincronização incremental (apenas artigos alterados)
- Classificação por sistema/módulo
- Governança de status interno
- Versionamento de conteúdo
- Scheduler configurável
- Base para automações e uso de IA

---

## Stack Tecnológica

- Java 21
- Spring Boot 4.0.1
- Spring Web (REST)
- Spring Data JPA
- PostgreSQL
- Flyway (migrations)
- RestClient (integração HTTP)
- Jackson (JSON)
- Maven
- Postman (testes manuais)
- IntelliJ IDEA

---

## Requisitos

- Java 21
- Maven

## Comandos principais

```bash
mvn -U clean compile
mvn test
mvn spring-boot:run
```

## Nota de migração (Spring Boot 4)

- `@MockBean` foi removido no Boot 4. Use `@MockitoBean` de
  `org.springframework.test.context.bean.override.mockito.MockitoBean`.

---

> _"Consagre ao Senhor tudo o que você faz, e os seus planos serão bem-sucedidos."_  
> **Provérbios 16:3**
