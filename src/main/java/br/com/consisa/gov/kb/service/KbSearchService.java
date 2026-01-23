package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔍 SERVICE DE BUSCA FULL-TEXT INTELIGENTE
 *
 * TECNOLOGIA:
 * -----------
 * - PostgreSQL Full-Text Search (FTS)
 * - ts_vector + ts_query para indexação
 * - Dicionário 'portuguese' para stemming
 * - GIN index para performance
 * - ts_rank para ranking por relevância
 *
 * RECURSOS:
 * ---------
 * ✅ Busca em título + conteúdo
 * ✅ Ranking por relevância (ts_rank)
 * ✅ Boost para artigos de alta qualidade
 * ✅ Filtros por sistema
 * ✅ Highlighting de termos (ts_headline)
 * ✅ Busca em artigos IA-ready
 *
 * QUANDO USAR:
 * ------------
 * - API de busca pública
 * - Sugestões de artigos relacionados
 * - Base para futura integração RAG/IA
 * - Autocomplete de pesquisa
 */
@Service
public class KbSearchService {

    private static final Logger log = LoggerFactory.getLogger(KbSearchService.class);

    private final EntityManager entityManager;
    private final KbArticleRepository articleRepo;

    public KbSearchService(EntityManager entityManager, KbArticleRepository articleRepo) {
        this.entityManager = entityManager;
        this.articleRepo = articleRepo;
    }

    // ======================
    // BUSCA PRINCIPAL
    // ======================

    /**
     * 🔍 Busca inteligente com ranking por relevância
     *
     * @param query termo de busca
     * @param limit máximo de resultados (1-100)
     * @return lista ordenada por relevância
     */
    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            log.warn("⚠️ Query vazia, retornando lista vazia");
            return List.of();
        }

        String sanitized = sanitizeQuery(query);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        log.info("🔍 Buscando: '{}' (limite: {})", sanitized, safeLimit);

        String sql = """
            SELECT
                a.id,
                a.title,
                a.summary,
                a.source_url,
                s.code AS system_code,
                s.name AS system_name,
                a.governance_status,
                ts_rank(
                    to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, a.content_html, '')),
                    plainto_tsquery('portuguese', :query)
                ) AS relevance_score,
                CASE
                    WHEN a.governance_status = 'APPROVED' THEN 1.5
                    WHEN a.sync_status = 'OK' AND a.system_id IS NOT NULL THEN 1.2
                    ELSE 1.0
                END AS quality_boost,
                ts_headline(
                    'portuguese',
                    COALESCE(SUBSTRING(a.content_text, 1, 1000), SUBSTRING(a.content_html, 1, 1000), ''),
                    plainto_tsquery('portuguese', :query),
                    'MaxWords=50, MinWords=20, MaxFragments=1'
                ) AS snippet
            FROM kb_article a
            LEFT JOIN kb_system s ON s.id = a.system_id
            WHERE a.article_status = 1
              AND to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, a.content_html, ''))
                  @@ plainto_tsquery('portuguese', :query)
            ORDER BY (relevance_score * quality_boost) DESC, a.updated_date DESC
            LIMIT :limit
        """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", sanitized);
        nativeQuery.setParameter("limit", safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        List<SearchResult> results = rows.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());

        log.info("✅ Encontrados {} resultados para '{}'", results.size(), sanitized);

        return results;
    }

    /**
     * 🔍 Busca com filtro de sistema
     */
    @Transactional(readOnly = true)
    public List<SearchResult> searchInSystem(String query, String systemCode, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String sanitized = sanitizeQuery(query);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        log.info("🔍 Buscando '{}' no sistema {} (limite: {})", sanitized, systemCode, safeLimit);

        String sql = """
            SELECT
                a.id,
                a.title,
                a.summary,
                a.source_url,
                s.code AS system_code,
                s.name AS system_name,
                a.governance_status,
                ts_rank(
                    to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, '')),
                    plainto_tsquery('portuguese', :query)
                ) AS relevance_score,
                1.0 AS quality_boost,
                ts_headline(
                    'portuguese',
                    COALESCE(SUBSTRING(a.content_text, 1, 1000), ''),
                    plainto_tsquery('portuguese', :query),
                    'MaxWords=50, MinWords=20'
                ) AS snippet
            FROM kb_article a
            JOIN kb_system s ON s.id = a.system_id
            WHERE a.article_status = 1
              AND s.code = :systemCode
              AND to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, ''))
                  @@ plainto_tsquery('portuguese', :query)
            ORDER BY relevance_score DESC, a.updated_date DESC
            LIMIT :limit
        """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", sanitized);
        nativeQuery.setParameter("systemCode", systemCode);
        nativeQuery.setParameter("limit", safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        return rows.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 Busca APENAS em artigos IA-ready (alta qualidade)
     *
     * Útil para RAG/IA - queremos apenas conteúdo de qualidade.
     */
    @Transactional(readOnly = true)
    public List<SearchResult> searchIaReady(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String sanitized = sanitizeQuery(query);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        log.info("🔍 Buscando IA-ready: '{}' (limite: {})", sanitized, safeLimit);

        String sql = """
            WITH ia_ready AS (
                SELECT article_id
                FROM kb_article_governance_report
                WHERE is_empty = false
                  AND is_duplicate_same_system = false
                  AND lacks_min_structure = false
            )
            SELECT
                a.id,
                a.title,
                a.summary,
                a.source_url,
                s.code AS system_code,
                s.name AS system_name,
                a.governance_status,
                ts_rank(
                    to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, '')),
                    plainto_tsquery('portuguese', :query)
                ) AS relevance_score,
                2.0 AS quality_boost,
                ts_headline(
                    'portuguese',
                    COALESCE(SUBSTRING(a.content_text, 1, 1000), ''),
                    plainto_tsquery('portuguese', :query),
                    'MaxWords=50'
                ) AS snippet
            FROM kb_article a
            JOIN ia_ready ir ON ir.article_id = a.id
            LEFT JOIN kb_system s ON s.id = a.system_id
            WHERE to_tsvector('portuguese', a.title || ' ' || COALESCE(a.content_text, ''))
                  @@ plainto_tsquery('portuguese', :query)
            ORDER BY relevance_score DESC
            LIMIT :limit
        """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", sanitized);
        nativeQuery.setParameter("limit", safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        return rows.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 Sugestões de artigos relacionados
     *
     * Baseado no título do artigo atual.
     */
    @Transactional(readOnly = true)
    public List<SearchResult> findSimilar(Long articleId, int limit) {
        var article = articleRepo.findById(articleId).orElse(null);
        if (article == null) {
            log.warn("⚠️ Artigo {} não encontrado", articleId);
            return List.of();
        }

        // Usa título como query
        String searchTerms = article.getTitle();

        var results = search(searchTerms, limit + 1); // +1 porque pode retornar o próprio

        // Remove o artigo atual dos resultados
        return results.stream()
                .filter(r -> !r.articleId().equals(articleId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ======================
    // HELPERS
    // ======================

    /**
     * Sanitiza query para evitar SQL injection
     */
    private String sanitizeQuery(String query) {
        if (query == null) return "";

        return query
                .trim()
                .replaceAll("[&|!<>():]", " ")  // Remove operadores FTS
                .replaceAll("\\s+", " ")         // Normaliza espaços
                .substring(0, Math.min(query.length(), 200));  // Limita tamanho
    }

    /**
     * Mapeia Object[] para SearchResult
     */
    private SearchResult mapToSearchResult(Object[] row) {
        return new SearchResult(
                toLong(row[0]),       // id
                toString(row[1]),     // title
                toString(row[2]),     // summary
                toString(row[3]),     // source_url
                toString(row[4]),     // system_code
                toString(row[5]),     // system_name
                toString(row[6]),     // governance_status
                toDouble(row[7]),     // relevance_score
                toDouble(row[8]),     // quality_boost
                toString(row[9])      // snippet
        );
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof java.math.BigInteger) return ((java.math.BigInteger) obj).longValue();
        return null;
    }

    private String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        return 0.0;
    }

    // ======================
    // RECORD (DTO)
    // ======================

    /**
     * 📄 Resultado de busca
     */
    public record SearchResult(
            Long articleId,
            String title,
            String summary,
            String sourceUrl,
            String systemCode,
            String systemName,
            String governanceStatus,
            Double relevanceScore,
            Double qualityBoost,
            String snippet
    ) {
        /**
         * Score final (relevância × boost)
         */
        public Double totalScore() {
            return relevanceScore * qualityBoost;
        }
    }
}
