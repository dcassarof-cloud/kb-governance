package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleSearchItemDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskMenuDto;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbSyncIssueType;
import br.com.consisa.gov.kb.domain.KbSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🧭 Service especializado em classificação de artigos
 *
 * RESPONSABILIDADES:
 * ------------------
 * - Classificar artigos via kb_menu_map (Menu Movidesk → Sistema)
 * - Abrir issues quando menu null ou não mapeado
 * - Aplicar fallback para sistema GERAL
 *
 * FLUXO:
 * ------
 * 1. Extrai menu do DTO do Movidesk
 * 2. Busca mapeamento ativo no kb_menu_map
 * 3. Se não encontrar → abre issue + fallback GERAL
 * 4. Salva system_id no artigo
 *
 * ISSUES ABERTAS:
 * ---------------
 * - MENU_NULL: quando menu vem null da API
 * - MENU_NOT_MAPPED: menu existe mas não tem mapeamento
 *
 * EXEMPLO:
 * --------
 * Menu: "Consisanet - Fiscal" (id=13282)
 * → Busca kb_menu_map (source_menu_id=13282, active=true)
 * → Encontra system_id=5 (CONSISANET)
 * → article.system_id = 5
 */
@Service
public class KbArticleClassificationService {

    private static final Logger log = LoggerFactory.getLogger(KbArticleClassificationService.class);

    private static final String SOURCE_SYSTEM = "movidesk";
    private static final String GERAL_CODE = "GERAL";

    private final KbMenuMapService menuMapService;
    private final KbSyncIssueService issueService;
    private final KbSystemService systemService;

    public KbArticleClassificationService(
            KbMenuMapService menuMapService,
            KbSyncIssueService issueService,
            KbSystemService systemService
    ) {
        this.menuMapService = menuMapService;
        this.issueService = issueService;
        this.systemService = systemService;
    }

    /**
     * Classifica artigo usando menu do search item.
     *
     * Usado no FULL SYNC (paginado).
     *
     * @param article artigo já salvo no banco
     * @param searchItem DTO do search (contém menu resumido)
     */
    @Transactional
    public void classifyFromSearchItem(KbArticle article, MovideskArticleSearchItemDto searchItem) {
        if (article == null || searchItem == null) {
            return;
        }

        Long menuId = extractMenuId(searchItem.getMenu());
        String menuName = extractMenuName(searchItem.getMenu());

        KbSystem system = resolveSystem(article.getId(), menuId, menuName);

        // atualiza artigo
        article.setSystem(system);

        if (menuId != null) {
            article.setSourceMenuId(menuId);
        }

        if (menuName != null && !menuName.isBlank()) {
            article.setSourceMenuName(menuName);
        }

        // log de diagnóstico
        logClassification(article.getId(), menuId, menuName, system);
    }

    /**
     * Classifica artigo usando menu do artigo completo.
     *
     * Usado no SYNC INDIVIDUAL (GET /article/{id}).
     *
     * @param article artigo já salvo no banco
     * @param menu menu do artigo completo (pode ser null)
     */
    @Transactional
    public void classifyFromMenu(KbArticle article, MovideskMenuDto menu) {
        if (article == null) {
            return;
        }

        Long menuId = extractMenuId(menu);
        String menuName = extractMenuName(menu);

        KbSystem system = resolveSystem(article.getId(), menuId, menuName);

        // atualiza artigo
        article.setSystem(system);

        if (menuId != null) {
            article.setSourceMenuId(menuId);
        }

        if (menuName != null && !menuName.isBlank()) {
            article.setSourceMenuName(menuName);
        }

        // log de diagnóstico
        logClassification(article.getId(), menuId, menuName, system);
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Resolve sistema baseado no menu.
     *
     * Ordem de decisão:
     * 1. Menu null → MENU_NULL issue + GERAL
     * 2. Menu sem mapeamento → MENU_NOT_MAPPED issue + GERAL
     * 3. Menu mapeado → retorna sistema correto
     */
    private KbSystem resolveSystem(Long articleId, Long menuId, String menuName) {
        KbSystem geral = systemService.getByCodeOrThrow(GERAL_CODE);

        // caso 1: menu null
        if (menuId == null) {
            issueService.open(
                    articleId,
                    KbSyncIssueType.MENU_NULL,
                    "Menu veio null (sem source_menu_id)"
            );
            return geral;
        }

        // caso 2: busca mapeamento ativo
        var mapOpt = menuMapService.findActive(SOURCE_SYSTEM, menuId);

        if (mapOpt.isPresent()) {
            // caso 3: mapeado → retorna sistema
            return mapOpt.get().getSystem();
        }

        // caso 2 continuação: não mapeado
        String msg = String.format(
                "Menu não mapeado. source_menu_id=%d source_menu_name='%s'",
                menuId,
                truncate(menuName, 150)
        );

        issueService.open(articleId, KbSyncIssueType.MENU_NOT_MAPPED, msg);

        return geral;
    }

    /**
     * Extrai ID do menu (null-safe).
     */
    private Long extractMenuId(MovideskMenuDto menu) {
        return (menu == null) ? null : menu.getId();
    }

    /**
     * Extrai nome do menu (null-safe).
     */
    private String extractMenuName(MovideskMenuDto menu) {
        return (menu == null) ? null : menu.getName();
    }

    /**
     * Log de diagnóstico da classificação.
     */
    private void logClassification(Long articleId, Long menuId, String menuName, KbSystem system) {
        Long systemId = (system == null) ? null : system.getId();
        String systemCode = (system == null) ? null : system.getCode();

        boolean isGeral = GERAL_CODE.equals(systemCode);

        if (isGeral) {
            log.warn("🧭 Classificado em GERAL. id={} menuId={} menuName='{}'",
                    articleId, menuId, menuName);
        } else {
            log.info("✅ Classificado. id={} menuId={} menuName='{}' systemId={} systemCode={}",
                    articleId, menuId, menuName, systemId, systemCode);
        }
    }

    /**
     * Trunca string para evitar logs gigantes.
     */
    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
