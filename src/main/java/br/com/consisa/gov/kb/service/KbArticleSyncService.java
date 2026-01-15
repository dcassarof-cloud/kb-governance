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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * Serviço responsável por sincronizar artigos da KB do Movidesk para o banco local.
 */
@Service
public class KbArticleSyncService {

    private static final Logger log = LoggerFactory.getLogger(KbArticleSyncService.class);

    // Status locais (em kb_article)
    private static final String SYNC_OK = "OK";
    private static final String SYNC_NOT_FOUND = "NOT_FOUND";
    private static final String SYNC_ERROR = "ERROR";

    // Governança mínima (para inserts)
    private static final String GOV_PENDING = "PENDING";

    // Fonte oficial do menu map
    private static final String SOURCE_SYSTEM = "movidesk";

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

            // marca como "morto" pra sair do ciclo do delta
            a.setSyncState("MISSING");
            a.setLastSeenAt(OffsetDateTime.now(ZoneOffset.UTC));

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
       SYNC INDIVIDUAL (GET /article/{id})
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

        // update se existe, insert se não existe
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
        entity.setSourceSystem(SOURCE_SYSTEM);
        entity.setFetchedAt(OffsetDateTime.now(ZoneOffset.UTC));

        String slug = (dto.getSlug() == null) ? "" : dto.getSlug();
        entity.setSourceUrl("https://consisanet.movidesk.com/kb/pt-br/article/" + dto.getId() + "/" + slug);

        // menu
        if (dto.getMenu() != null) {
            entity.setSourceMenuId(dto.getMenu().getId());
            entity.setSourceMenuName(dto.getMenu().getName());
        }

        // ✅ content_hash: base para detector de duplicados
        String baseForHash = (entity.getContentText() != null && !entity.getContentText().isBlank())
                ? entity.getContentText()
                : entity.getContentHtml();

        if (baseForHash != null && !baseForHash.isBlank()) {
            entity.setContentHash(sha256(normalizeForHash(baseForHash)));
        } else {
            entity.setContentHash(null);
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

    /**
     * FULL SYNC: varre todos os artigos do Movidesk via search (paginado)
     * e baixa cada artigo via getArticleById (sync(id)).
     */
    @Transactional
    public void syncAllFull() {
        int page = 0;
        int pageSize = 50;
        Integer totalSize = null;

        KbSystem geral = getSystemOrThrow("GERAL");
        Long geralId = geral.getId();

        log.info("🚀 syncAllFull iniciado. pageSize={}", pageSize);

        while (true) {
            try {
                var resp = movideskClient.searchArticles(page, pageSize);
                var items = resp.getItems();

                if (totalSize == null) totalSize = resp.getTotalSize();

                log.info("📄 page={} totalSize={} items={}", page, totalSize, (items == null ? 0 : items.size()));

                if (items == null || items.isEmpty()) break;

                for (MovideskArticleSearchItemDto item : items) {
                    Long id = (item == null) ? null : item.getId();
                    if (id == null) continue;

                    try {
                        KbArticle saved = sync(id);
                        if (saved == null) continue;

                        // usa menu do SEARCH para classificar no FULL
                        classifyUsingMenuMap(saved, item, geral, geralId);

                    } catch (Exception e) {
                        log.error("❌ erro ao sincronizar/classificar id={}", id, e);
                        openErrorIssue(id, truncate(e.getMessage(), 400));
                    }
                }

                page++;
                if (totalSize != null && page * pageSize >= totalSize) break;

            } catch (Exception ex) {
                log.error("❌ Falha no searchArticles page={}. Encerrando FULL. motivo={}", page, ex.toString(), ex);
                break;
            }
        }

        log.info("🏁 syncAllFull finalizado.");
    }

    /* =========================================================
       CLASSIFICAÇÃO (kb_menu_map)
       ========================================================= */

    private void classifyUsingMenuMap(KbArticle saved,
                                      MovideskArticleSearchItemDto item,
                                      KbSystem geral,
                                      Long geralId) {

        Long id = saved.getId();

        Long menuId = safeMenuId(item);
        String menuName = safeMenuName(item);

        KbSystem system = resolveSystemFromMenu(menuId, menuName, id, geral);

        saved.setSystem(system);
        if (menuId != null) saved.setSourceMenuId(menuId);
        if (menuName != null && !menuName.isBlank()) saved.setSourceMenuName(menuName);

        if (saved.getGovernanceStatus() == null || saved.getGovernanceStatus().isBlank()) {
            saved.setGovernanceStatus(GOV_PENDING);
        }

        repository.save(saved);

        Long systemId = (system == null ? null : system.getId());
        boolean caiuEmGeral = (systemId == null) || (geralId != null && geralId.equals(systemId));

        if (caiuEmGeral) {
            log.warn("🧭 Caiu em GERAL. id={} menuId={} menuRaw='{}'", id, menuId, menuName);
        }

        log.info("✅ classificado id={} menuId={} menu='{}' systemId={}", id, menuId, menuName, systemId);
    }

    private KbSystem resolveSystemFromMenu(Long menuId, String menuName, Long articleId, KbSystem geral) {
        if (menuId == null) {
            issueService.open(articleId, KbSyncIssueType.MENU_NULL,
                    "Menu veio null (sem source_menu_id)");
            return geral;
        }

        var mapOpt = menuMapService.findActive(SOURCE_SYSTEM, menuId);
        if (mapOpt.isPresent()) {
            return mapOpt.get().getSystem();
        }

        issueService.open(articleId, KbSyncIssueType.MENU_NOT_MAPPED,
                "Menu não mapeado. source_menu_id=" + menuId +
                        " source_menu_name='" + truncate(menuName, 150) + "'");
        return geral;
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

    // ============================
    // ✅ HASH HELPERS (NO FINAL!)
    // ============================

    private static String normalizeForHash(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar SHA-256", e);
        }
    }
}
