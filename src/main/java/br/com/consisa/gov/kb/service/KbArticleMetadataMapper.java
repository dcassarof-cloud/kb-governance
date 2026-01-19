package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleDto;
import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 🗺️ Mapper especializado: DTO Movidesk → Entity KbArticle
 *
 * RESPONSABILIDADES:
 * ------------------
 * - Mapear campos básicos (título, slug, status, etc)
 * - Mapear datas usando MovideskDateParser
 * - Gerar content_hash via KbArticleHashService
 * - Construir source_url
 * - Inicializar campos de auditoria
 *
 * NÃO FAZ:
 * --------
 * - Classificação (KbArticleClassificationService)
 * - Persistência (repository)
 * - Validações de negócio
 *
 * EXEMPLO:
 * --------
 * MovideskArticleDto dto = client.getArticleById(123);
 * KbArticle entity = mapper.map(dto);
 * // entity pronto para salvar
 */
@Service
public class KbArticleMetadataMapper {

    private static final String SOURCE_SYSTEM = "movidesk";
    private static final String BASE_URL = "https://consisanet.movidesk.com/kb/pt-br/article/";

    private final MovideskDateParser dateParser;
    private final KbArticleHashService hashService;

    public KbArticleMetadataMapper(
            MovideskDateParser dateParser,
            KbArticleHashService hashService
    ) {
        this.dateParser = dateParser;
        this.hashService = hashService;
    }

    /**
     * Mapeia DTO completo para entidade.
     *
     * ⚠️ Importante:
     * - Usa entidade existente se fornecida (update)
     * - Cria nova se null (insert)
     * - NÃO persiste no banco
     * - NÃO classifica (system_id fica null)
     *
     * @param dto DTO retornado pela API
     * @param existing entidade existente ou null
     * @return entidade mapeada (não persistida)
     */
    public KbArticle map(MovideskArticleDto dto, KbArticle existing) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("DTO inválido ou sem ID");
        }

        KbArticle entity = (existing != null) ? existing : new KbArticle();

        // ===========================
        // Campos básicos
        // ===========================

        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setSlug(dto.getSlug());
        entity.setArticleStatus(dto.getArticleStatus());
        entity.setSummary(dto.getSummary());
        entity.setContentHtml(dto.getContentHtml());
        entity.setContentText(dto.getContentText());
        entity.setRevisionId(dto.getRevisionId());
        entity.setReadingTime(dto.getReadingTime());

        // ===========================
        // Datas (via parser)
        // ===========================

        entity.setCreatedDate(dateParser.parse(dto.getCreatedDate()));
        entity.setUpdatedDate(dateParser.parse(dto.getUpdatedDate()));

        // ===========================
        // Auditoria/origem
        // ===========================

        entity.setSourceSystem(SOURCE_SYSTEM);
        entity.setFetchedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setSourceUrl(buildSourceUrl(dto.getId(), dto.getSlug()));

        // ===========================
        // Menu (raw, sem classificar)
        // ===========================

        if (dto.getMenu() != null) {
            entity.setSourceMenuId(dto.getMenu().getId());
            entity.setSourceMenuName(dto.getMenu().getName());
        }

        // ===========================
        // Content hash (duplicados)
        // ===========================

        String baseForHash = selectContentForHash(
                entity.getContentText(),
                entity.getContentHtml()
        );

        if (baseForHash != null && !baseForHash.isBlank()) {
            entity.setContentHash(hashService.generateContentHash(baseForHash));
        } else {
            entity.setContentHash(null);
        }

        // ===========================
        // Governança mínima (se insert)
        // ===========================

        if (entity.getGovernanceStatus() == null || entity.getGovernanceStatus().isBlank()) {
            entity.setGovernanceStatus("PENDING");
        }

        return entity;
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Constrói URL do artigo no Movidesk.
     *
     * Formato: https://consisanet.movidesk.com/kb/pt-br/article/{id}/{slug}
     *
     * Se slug null/vazio, usa apenas ID.
     */
    private String buildSourceUrl(Long id, String slug) {
        String cleanSlug = (slug == null || slug.isBlank()) ? "" : slug;
        return BASE_URL + id + "/" + cleanSlug;
    }

    /**
     * Seleciona qual conteúdo usar para hash.
     *
     * Prioridade:
     * 1. contentText (se não vazio)
     * 2. contentHtml (fallback)
     *
     * @return conteúdo escolhido ou null se ambos vazios
     */
    private String selectContentForHash(String contentText, String contentHtml) {
        boolean hasText = contentText != null && !contentText.isBlank();
        boolean hasHtml = contentHtml != null && !contentHtml.isBlank();

        if (hasText) {
            return contentText;
        }

        if (hasHtml) {
            return contentHtml;
        }

        return null;
    }
}
