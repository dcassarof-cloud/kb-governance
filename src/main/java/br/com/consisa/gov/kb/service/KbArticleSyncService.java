package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.domain.KbSyncIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import br.com.consisa.gov.kb.domain.KbSystem;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 🔄 Service de sincronização individual de artigos
 *
 * RESPONSABILIDADES (após refatoração):
 * -------------------------------------
 * ✅ Buscar artigo no Movidesk (GET /article/{id})
 * ✅ Tratar erros HTTP (404, 5xx, timeout)
 * ✅ Orquestrar mapeamento, classificação e persistência
 * ✅ Abrir issues quando necessário
 * ✅ Atualizar status de sync
 *
 * NÃO FAZ MAIS (delegado):
 * -------------------------
 * ❌ Parsing de datas → MovideskDateParser
 * ❌ Geração de hash → KbArticleHashService
 * ❌ Classificação → KbArticleClassificationService
 * ❌ Mapeamento DTO → KbArticleMetadataMapper
 * ❌ Sync full → KbFullSyncService
 *
 * QUANDO USAR:
 * ------------
 * - Sync individual de UM artigo
 * - Chamado pelo DELTA sync
 * - Chamado pelo FULL sync (via loop)
 * - Endpoint manual: POST /kb/articles/{id}/sync
 *
 * FLUXO:
 * ------
 * 1. Busca artigo via HTTP (MovideskClient)
 * 2. Trata 404 → abre issue NOT_FOUND
 * 3. Trata erros → abre issue ERROR
 * 4. Mapeia DTO → Entity (MetadataMapper)
 * 5. Classifica (ClassificationService)
 * 6. Detecta conteúdo vazio → abre issue EMPTY_CONTENT
 * 7. Marca sync_status = OK
 * 8. Salva no banco
 * 9. Retorna entidade salva
 */
@Service
public class KbArticleSyncService {

    private static final Logger log = LoggerFactory.getLogger(KbArticleSyncService.class);

    // Status técnicos do sync
    private static final String SYNC_OK = "OK";
    private static final String SYNC_NOT_FOUND = "NOT_FOUND";
    private static final String SYNC_ERROR = "ERROR";

    // Dependencies (injeção)
    private final MovideskClient movideskClient;
    private final KbArticleRepository repository;
    private final KbArticleMetadataMapper metadataMapper;
    private final KbArticleClassificationService classificationService;
    private final KbSyncIssueService issueService;
    private final KbArticleHashService hashService;
    private final KbSystemService systemService;
    private final KbGovernanceIssueService governanceIssueService;

    public KbArticleSyncService(
            MovideskClient movideskClient,
            KbArticleRepository repository,
            KbArticleMetadataMapper metadataMapper,
            KbArticleClassificationService classificationService,
            KbSyncIssueService issueService,
            KbArticleHashService hashService,
            KbSystemService systemService,
            KbGovernanceIssueService governanceIssueService
    ) {
        this.movideskClient = movideskClient;
        this.repository = repository;
        this.metadataMapper = metadataMapper;
        this.classificationService = classificationService;
        this.issueService = issueService;
        this.hashService = hashService;
        this.systemService = systemService;
        this.governanceIssueService = governanceIssueService;
    }

    /**
     * Sincroniza UM artigo do Movidesk.
     *
     * ⚠️ Transacional: rollback se erro ao salvar.
     *
     * @param articleId ID do artigo no Movidesk
     * @return entidade salva ou null se não encontrado
     *
     * @throws RuntimeException se erro crítico (não HTTP)
     */
    @Transactional
    public KbArticle sync(long articleId) {
        // ===========================
        // 1) Busca artigo via HTTP
        // ===========================

        MovideskArticleDto dto;

        try {
            log.debug("🔄 Sync artigo id={}", articleId);
            dto = movideskClient.getArticleById(articleId);

        } catch (HttpClientErrorException.NotFound ex) {
            // 404: artigo foi deletado ou nunca existiu
            log.warn("⚠️ Movidesk 404 (Article was not found). id={}", articleId);
            handleNotFound(articleId);
            return null;

        } catch (Exception ex) {
            // 5xx, timeout, DNS, etc
            log.error("❌ Erro ao buscar artigo. id={} motivo={}", articleId, ex.toString());
            handleError(articleId, ex);
            return null;
        }

        // ===========================
        // 2) Validação básica
        // ===========================

        if (dto.getId() == null) {
            String msg = "Movidesk retornou dto.id null";
            log.error("❌ {}", msg);
            handleError(articleId, new IllegalStateException(msg));
            throw new IllegalStateException(msg + " para articleId=" + articleId);
        }

        // ===========================
        // 3) Mapeia DTO → Entity
        // ===========================

        KbArticle existing = repository.findById(dto.getId()).orElse(null);
        KbArticle entity = metadataMapper.map(dto, existing);

        // ===========================
        // 4) Classifica (menu → sistema)
        // ===========================

        classificationService.classifyFromMenu(entity, dto.getMenu());

        // ===========================
        // 5) Detecta conteúdo vazio
        // ===========================

        checkEmptyContent(entity);

        // ===========================
        // 6) Marca sync OK
        // ===========================

        entity.setSyncStatus(SYNC_OK);
        entity.setSyncErrorMessage(null);

        // marca como "visto" (usado no DELTA)
        entity.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setSyncState("SYNCED");

        // ===========================
        // 7) Salva no banco
        // ===========================

        KbArticle saved = repository.save(entity);

        governanceIssueService.open(
                saved.getId(),
                KbGovernanceIssueType.REVIEW_REQUIRED,
                GovernanceSeverity.INFO,
                "Revisão obrigatória pendente para este manual.",
                null
        );

        log.info("✅ Artigo sincronizado. id={} title='{}'", saved.getId(), saved.getTitle());

        return saved;
    }

    /**
     * Atribui manualmente um sistema a um artigo.
     *
     * Usado para correções manuais via API.
     *
     * @param articleId ID do artigo
     * @param systemCode código do sistema (ex: NOTAON, SGRH)
     */
    @Transactional
    public void assignSystem(long articleId, String systemCode) {
        KbArticle article = repository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Artigo não encontrado: " + articleId
                ));

        KbSystem system = systemService.getByCodeOrThrow(systemCode);

        article.setSystem(system);
        repository.save(article);

        log.info("🔧 Sistema atribuído manualmente. articleId={} systemCode={}",
                articleId, systemCode);
    }

    /**
     * Lista artigos sem classificação (system_id null).
     *
     * Útil para diagnóstico e correção manual.
     *
     * @return até 200 artigos não classificados
     */
    @Transactional(readOnly = true)
    public List<KbArticle> listUnclassified() {
        return repository.findTop200BySystemIsNullOrderByUpdatedDateDesc();
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Trata artigo não encontrado (404).
     *
     * - Abre issue NOT_FOUND
     * - Marca sync_status = NOT_FOUND
     * - Marca sync_state = MISSING (para delta não tentar de novo)
     */
    private void handleNotFound(long articleId) {
        String msg = "Movidesk 404: Article was not found";

        issueService.open(articleId, KbSyncIssueType.NOT_FOUND, msg);

        repository.findById(articleId).ifPresent(article -> {
            article.setSyncStatus(SYNC_NOT_FOUND);
            article.setSyncErrorMessage(truncate(msg, 400));
            article.setSyncState("MISSING");
            article.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.save(article);
        });
    }

    /**
     * Trata erro ao buscar artigo (5xx, timeout, etc).
     *
     * - Abre issue ERROR
     * - Marca sync_status = ERROR
     */
    private void handleError(long articleId, Exception ex) {
        String msg = truncate(ex.getMessage(), 400);

        issueService.open(articleId, KbSyncIssueType.ERROR, msg);

        repository.findById(articleId).ifPresent(article -> {
            article.setSyncStatus(SYNC_ERROR);
            article.setSyncErrorMessage(msg);
            repository.save(article);
        });
    }

    /**
     * Detecta conteúdo vazio e abre issue.
     *
     * Considera vazio quando:
     * - contentText vazio E contentHtml vazio
     */
    private void checkEmptyContent(KbArticle entity) {
        int textLen = hashService.cleanLength(entity.getContentText());
        int htmlLen = hashService.cleanLength(entity.getContentHtml());

        boolean emptyBoth = (textLen == 0 && htmlLen == 0);

        if (emptyBoth) {
            issueService.open(
                    entity.getId(),
                    KbSyncIssueType.EMPTY_CONTENT,
                    "Artigo com conteúdo vazio (HTML e TEXT)"
            );

            log.warn("📭 Issue EMPTY_CONTENT. id={} title='{}'",
                    entity.getId(), entity.getTitle());
        }
    }

    /**
     * Trunca string para evitar overflow no banco.
     */
    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
