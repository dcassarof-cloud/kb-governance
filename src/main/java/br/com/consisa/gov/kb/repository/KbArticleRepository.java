package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para acesso à tabela kb_article.
 *
 * Responsabilidade:
 * - Consultas e persistência de KbArticle
 * - Sem regra de negócio (isso fica no Service)
 */
public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    /**
     * Retorna no máximo 200 artigos que ainda não possuem sistema associado
     * (system_id IS NULL), ordenados pelos mais recentemente atualizados.
     *
     * Por que limitar 200?
     * - Evita trazer milhares de registros de uma vez (performance/memória)
     * - Ajuda na curadoria manual via tela (paginação simples)
     *
     * Observação:
     * - Se no futuro precisar de paginação real, trocamos para Pageable.
     */
    List<KbArticle> findTop200BySystemIsNullOrderByUpdatedDateDesc();
}
