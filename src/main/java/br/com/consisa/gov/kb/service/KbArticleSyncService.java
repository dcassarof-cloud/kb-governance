package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskArticleSearchItemDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbSyncIssueType;
import br.com.consisa.gov.kb.domain.KbSystem;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * Serviço responsável por sincronizar artigos da KB do Movidesk
 * para o banco local, mantendo:
 * - conteúdo (HTML / texto)
 * - metadados (status, slug, revisionId, datas)
 * - auditoria da fonte (sourceUrl, sourceMenuId, sourceMenuName)
 * - classificação por sistema (KbSystem)
 *
 * Governança via issues (kb_sync_issue):
 * - syncAll() nunca pode cair por causa de 1 artigo
 * - 404 vira issue NOT_FOUND (não fatal)
 * - menu null vira issue MENU_NULL (não tenta adivinhar)
 * - menu não mapeado vira issue MENU_NOT_MAPPED
 * - conteúdo vazio vira issue EMPTY_CONTENT (métrica)
 * - qualquer exceção vira issue ERROR
 *
 * Regra importante:
 * - NÃO criar "stub" em kb_article só pra registrar erro.
 * - Se kb_article existir, atualiza syncStatus/syncErrorMessage; se não existir, só cria issue.
 */
@Service
public class    KbArticleSyncService {

    private static final Logger log = LoggerFactory.getLogger(KbArticleSyncService.class);

    // Status locais (em kb_article) - usado só quando o artigo já existe localmente
    private static final String SYNC_OK = "OK";
    private static final String SYNC_NOT_FOUND = "NOT_FOUND";
    private static final String SYNC_ERROR = "ERROR";

    // Governança mínima (para não quebrar constraint nullable=false em inserts reais)
    private static final String GOV_PENDING = "PENDING";

    private final MovideskClient movideskClient;
    private final KbArticleRepository repository;
    private final KbSystemRepository systemRepository;
    private final KbMenuMapService menuMapService;
    private final KbSyncIssueService issueService;

    public KbArticleSyncService(
            MovideskClient movideskClient,
            KbArticleRepository repository,
            KbSystemRepository systemRepository,
            KbMenuMapService menuMapService,
            KbSyncIssueService issueService
    ) {
        this.movideskClient = movideskClient;
        this.repository = repository;
        this.systemRepository = systemRepository;
        this.menuMapService = menuMapService;
        this.issueService = issueService;
    }

    /* =========================================================
       DATE PARSER (Movidesk é inconsistente com offset)
       ========================================================= */

    private static final DateTimeFormatter MOVI_DATE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter();

    private static OffsetDateTime parseMovi(String s) {
        if (s == null || s.isBlank()) return null;

        boolean hasOffset =
                s.endsWith("Z") ||
                        s.contains("+") ||
                        s.matches(".*-\\d\\d:\\d\\d$");

        if (!hasOffset) {
            LocalDateTime ldt = LocalDateTime.parse(s, MOVI_DATE);
            return ldt.atOffset(ZoneOffset.UTC);
        }

        return OffsetDateTime.parse(s, MOVI_DATE);
    }

    /* =========================================================
       ISSUE HELPERS (sem criar stub)
       ========================================================= */

    @Transactional
    protected void openNotFoundIssue(long articleId, String msg) {
        issueService.open(articleId, KbSyncIssueType.NOT_FOUND, msg);

        repository.findById(articleId).ifPresent(a -> {
            a.setSyncStatus(SYNC_NOT_FOUND);
            a.setSyncErrorMessage(truncate(msg, 400));
            repository.save(a);
        });
    }

    @Transactional
    protected void openErrorIssue(long articleId, String msg) {
        issueService.open(articleId, KbSyncIssueType.ERROR, msg);

        repository.findById(articleId).ifPresent(a -> {
            a.setSyncStatus(SYNC_ERROR);
            a.setSyncErrorMessage(truncate(msg, 400));
            repository.save(a);
        });
    }

    /* =========================================================
       SYNC DE ARTIGO INDIVIDUAL (GET /article/{id})
       ========================================================= */

    @Transactional
    public KbArticle sync(long articleId) {
        MovideskArticleDto dto;

        try {
            dto = movideskClient.getArticleById(articleId);

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("⚠️ Movidesk 404 (Article was not found). id={}", articleId);
            openNotFoundIssue(articleId, "Movidesk 404: Article was not found");
            return null;

        } catch (Exception ex) {
            log.error("❌ Erro ao buscar artigo no Movidesk. id={} motivo={}", articleId, ex.toString());
            openErrorIssue(articleId, truncate(ex.getMessage(), 400));
            return null;
        }

        if (dto.getId() == null) {
            openErrorIssue(articleId, "Movidesk retornou dto.id null");
            throw new IllegalStateException("Movidesk retornou artigo sem id (null) para articleId=" + articleId);
        }

        KbArticle entity = repository.findById(dto.getId()).orElseGet(KbArticle::new);

        // metadados
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setSlug(dto.getSlug());
        entity.setArticleStatus(dto.getArticleStatus());
        entity.setSummary(dto.getSummary());
        entity.setContentHtml(dto.getContentHtml());
        entity.setContentText(dto.getContentText());
        entity.setRevisionId(dto.getRevisionId());
        entity.setReadingTime(dto.getReadingTime());

        // datas
        entity.setCreatedDate(parseMovi(dto.getCreatedDate()));
        entity.setUpdatedDate(parseMovi(dto.getUpdatedDate()));

        // origem/auditoria
        entity.setSourceSystem("movidesk");
        entity.setFetchedAt(OffsetDateTime.now(ZoneOffset.UTC));

        String slug = (dto.getSlug() == null) ? "" : dto.getSlug();
        entity.setSourceUrl(
                "https://consisanet.movidesk.com/kb/pt-br/article/" + dto.getId() + "/" + slug
        );

        // menu (endpoint completo)
        if (dto.getMenu() != null) {
            entity.setSourceMenuId(dto.getMenu().getId());
            entity.setSourceMenuName(dto.getMenu().getName());
        }

        // governança mínima
        if (entity.getGovernanceStatus() == null || entity.getGovernanceStatus().isBlank()) {
            entity.setGovernanceStatus(GOV_PENDING);
        }

        // status técnico OK
        entity.setSyncStatus(SYNC_OK);
        entity.setSyncErrorMessage(null);

        // métrica: conteúdo vazio
        boolean emptyText = (entity.getContentText() == null || entity.getContentText().trim().isEmpty());
        boolean emptyHtml = (entity.getContentHtml() == null || entity.getContentHtml().trim().isEmpty());
        if (emptyText && emptyHtml) {
            issueService.open(entity.getId(), KbSyncIssueType.EMPTY_CONTENT,
                    "Artigo com conteúdo vazio (HTML e TEXT)");
            log.warn("📭 Issue EMPTY_CONTENT id={} title='{}'", entity.getId(), entity.getTitle());
        }

        return repository.save(entity);
    }

    /* =========================================================
       SYNC GERAL + CLASSIFICAÇÃO (kb_menu_map)
       ========================================================= */

    public void syncAll() {
        int page = 0;
        int pageSize = 30;
        Integer totalSize = null;

        KbSystem geral = getSystemOrThrow("GERAL");
        final String sourceSystem = "movidesk";

        log.info("🚀 syncAll iniciado. pageSize={}", pageSize);

        while (true) {
            try {
                var resp = movideskClient.searchArticles(page, pageSize);
                var items = resp.getItems();

                if (totalSize == null) totalSize = resp.getTotalSize();

                log.info("📄 page={} totalSize={} items={}",
                        page,
                        totalSize,
                        (items == null ? 0 : items.size())
                );

                if (items == null || items.isEmpty()) break;

                for (MovideskArticleSearchItemDto item : items) {
                    Long id = (item == null) ? null : item.getId();
                    if (id == null) continue;

                    try {
                        // 1) sincroniza conteúdo completo
                        KbArticle saved = sync(id);
                        if (saved == null) continue;

                        // 2) menu vindo do SEARCH
                        Long menuId = safeMenuId(item);
                        String menuName = safeMenuName(item);

                        // 3) resolve sistema via kb_menu_map
                        KbSystem system;

                        if (menuId == null) {
                            issueService.open(id, KbSyncIssueType.MENU_NULL,
                                    "Menu veio null no search do Movidesk");
                            system = geral;
                        } else {
                            var mapOpt = menuMapService.findActive(sourceSystem, menuId);
                            if (mapOpt.isPresent()) {
                                system = mapOpt.get().getSystem();
                            } else {
                                issueService.open(id, KbSyncIssueType.MENU_NOT_MAPPED,
                                        "Menu não mapeado. source_menu_id=" + menuId +
                                                " source_menu_name='" + truncate(menuName, 150) + "'");
                                system = geral;
                            }
                        }

                        // 4) persiste classificação + auditoria
                        saved.setSystem(system);
                        if (menuId != null) saved.setSourceMenuId(menuId);
                        if (menuName != null && !menuName.isBlank()) saved.setSourceMenuName(menuName);

                        if (saved.getGovernanceStatus() == null || saved.getGovernanceStatus().isBlank()) {
                            saved.setGovernanceStatus(GOV_PENDING);
                        }

                        repository.save(saved);

                        String systemCode = (system == null ? "NULL" : system.getCode());

                        if ("GERAL".equals(systemCode)) {
                            log.warn("🧭 Caiu em GERAL. id={} menuId={} menuRaw='{}'",
                                    id, menuId, menuName);
                        }

                        log.info("✅ classificado id={} menuId={} menu='{}' system={}",
                                id, menuId, menuName, systemCode
                        );
                    } catch (Exception e) {
                        log.error("❌ erro ao sincronizar/classificar id={}", id, e);
                        openErrorIssue(id, truncate(e.getMessage(), 400));
                    }
                }

                page++;
                if (totalSize != null && page * pageSize >= totalSize) break;

            } catch (Exception ex) {
                log.error("❌ Falha no searchArticles page={}. Encerrando syncAll. motivo={}",
                        page, ex.toString(), ex);
                break;
            }
        }

        log.info("🏁 syncAll finalizado.");
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private Long safeMenuId(MovideskArticleSearchItemDto item) {
        if (item == null || item.getMenu() == null) return null;
        return item.getMenu().getId();
    }

    private String safeMenuName(MovideskArticleSearchItemDto item) {
        if (item == null || item.getMenu() == null) return null;
        return item.getMenu().getName();
    }

    private KbSystem getSystemOrThrow(String code) {
        return systemRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Sistema não encontrado no banco: " + code));
    }

    /* =========================================================
       OPERAÇÕES AUXILIARES
       ========================================================= */

    @Transactional
    public void assignSystem(long articleId, String systemCode) {
        KbArticle article = repository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Artigo não encontrado: " + articleId));

        KbSystem system = systemRepository.findByCode(systemCode)
                .orElseThrow(() -> new IllegalArgumentException("Sistema não encontrado: " + systemCode));

        article.setSystem(system);
        repository.save(article);
    }

    @Transactional(readOnly = true)
    public List<KbArticle> listUnclassified() {
        return repository.findTop200BySystemIsNullOrderByUpdatedDateDesc();
    }
}
