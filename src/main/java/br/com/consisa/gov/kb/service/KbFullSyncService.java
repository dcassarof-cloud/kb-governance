package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleSearchItemDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskArticleSearchResponse;
import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🔄 Service especializado em FULL SYNC (paginado)
 *
 * RESPONSABILIDADES:
 * ------------------
 * - Varrer TODOS os artigos via search (paginado)
 * - Para cada artigo:
 *   1. Baixar via GET /article/{id}
 *   2. Mapear DTO → Entity
 *   3. Classificar (menu → sistema)
 *   4. Salvar no banco
 *
 * FLUXO:
 * ------
 * 1. page = 0
 * 2. Busca search(page, pageSize)
 * 3. Para cada item:
 *    - sync(item.id)
 *    - classify(article, item)
 * 4. page++
 * 5. Repete até não ter mais itens
 *
 * QUANDO USAR:
 * ------------
 * - Primeira sincronização
 * - Reprocessamento completo
 * - Depois de seed de kb_menu_map
 *
 * QUANDO NÃO USAR:
 * ----------------
 * - Sincronização incremental (use DELTA)
 * - Horário comercial (muitas requisições)
 *
 * DIFERENÇA DO DELTA:
 * -------------------
 * - FULL: varre tudo, sempre baixa GET /article/{id}
 * - DELTA: só baixa se detectar mudança (revisionId/updatedDate)
 */
@Service
public class KbFullSyncService {

    private static final Logger log = LoggerFactory.getLogger(KbFullSyncService.class);

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGES = 1000; // safety: ~50k artigos

    private final MovideskClient movideskClient;
    private final KbArticleRepository repository;
    private final KbArticleSyncService syncService;
    private final KbArticleClassificationService classificationService;

    public KbFullSyncService(
            MovideskClient movideskClient,
            KbArticleRepository repository,
            KbArticleSyncService syncService,
            KbArticleClassificationService classificationService
    ) {
        this.movideskClient = movideskClient;
        this.repository = repository;
        this.syncService = syncService;
        this.classificationService = classificationService;
    }

    /**
     * Executa FULL SYNC com pageSize padrão (50).
     *
     * Este é o método principal do FULL SYNC.
     */
    @Transactional
    public void syncAll() {
        syncAll(DEFAULT_PAGE_SIZE);
    }

    /**
     * Executa FULL SYNC com pageSize customizado.
     *
     * ⚠️ Atenção:
     * - pageSize muito grande → timeout
     * - pageSize muito pequeno → muitas requisições
     * - recomendado: 30-100
     *
     * @param pageSize quantidade de itens por página
     */
    @Transactional
    public void syncAll(int pageSize) {
        int safePageSize = Math.max(10, Math.min(pageSize, 200));

        log.info("🚀 FULL SYNC iniciado. pageSize={}", safePageSize);

        int page = 0;
        Integer totalSize = null;
        int totalSynced = 0;

        while (page < MAX_PAGES) {
            try {
                MovideskArticleSearchResponse resp = movideskClient.searchArticles(page, safePageSize);

                if (totalSize == null) {
                    totalSize = resp.getTotalSize();
                }

                var items = resp.getItems();

                log.info("📄 FULL SYNC page={} totalSize={} items={}",
                        page, totalSize, (items == null ? 0 : items.size()));

                if (items == null || items.isEmpty()) {
                    log.info("🏁 FULL SYNC: sem mais itens na página {}. Encerrando.", page);
                    break;
                }

                // processa cada item
                for (MovideskArticleSearchItemDto item : items) {
                    if (processItem(item)) {
                        totalSynced++;
                    }
                }

                page++;

                // checa se acabou
                if (totalSize != null && page * safePageSize >= totalSize) {
                    log.info("🏁 FULL SYNC: todas as páginas processadas.");
                    break;
                }

            } catch (Exception ex) {
                log.error("❌ FULL SYNC: falha na página {}. Encerrando. motivo={}",
                        page, ex.toString(), ex);
                break;
            }
        }

        log.info("🏁 FULL SYNC finalizado. totalSynced={}", totalSynced);
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Processa um item do search.
     *
     * Fluxo:
     * 1. Sync individual (GET /article/{id})
     * 2. Classifica usando menu do search
     * 3. Salva
     *
     * @return true se sucesso, false se erro/skip
     */
    private boolean processItem(MovideskArticleSearchItemDto item) {
        if (item == null || item.getId() == null) {
            return false;
        }

        Long id = item.getId();

        try {
            // 1) sync individual (baixa artigo completo)
            KbArticle article = syncService.sync(id);

            if (article == null) {
                log.warn("⚠️ FULL SYNC: sync retornou null. id={}", id);
                return false;
            }

            // 2) classifica usando menu do search
            classificationService.classifyFromSearchItem(article, item);

            // 3) salva (classificação aplicada)
            repository.save(article);

            return true;

        } catch (Exception e) {
            log.error("❌ FULL SYNC: erro ao processar item. id={} motivo={}",
                    id, e.toString(), e);
            return false;
        }
    }
}
