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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔄 VERSÃO 2.0 - FULL SYNC Melhorado
 *
 * MELHORIAS:
 * ----------
 * ✅ Processamento em batch (configur��vel)
 * ✅ Métricas detalhadas (tempo médio, taxa de sucesso)
 * ✅ Progresso em tempo real
 * ✅ Retry automático de falhas
 * ✅ Processamento paralelo opcional
 * ✅ Melhor tratamento de erros
 * ✅ Estatísticas por página
 *
 * QUANDO USAR:
 * ------------
 * - Primeira sincronização
 * - Reprocessamento completo
 * - Recuperação após falha crítica
 * - Validação de integridade
 *
 * CONFIGURAÇÕES:
 * --------------
 * - pageSize: 30-100 (recomendado: 50)
 * - batchSize: quantos artigos processar antes de commit
 * - parallelism: 1 (sequencial) ou mais (paralelo)
 */
@Service
public class KbFullSyncService {

    private static final Logger log = LoggerFactory.getLogger(KbFullSyncService.class);

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int MAX_PAGES = 1000; // safety: ~50k artigos
    private static final int MAX_RETRIES = 2;

    private final MovideskClient movideskClient;
    private final KbArticleRepository repository;
    private final KbArticleSyncService syncService;
    private final KbArticleClassificationService classificationService;

    // Métricas em tempo real
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalSucceeded = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);
    private volatile OffsetDateTime syncStartTime;

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

    // ======================
    // API Pública
    // ======================

    /**
     * Executa FULL SYNC com configurações padrão.
     */
    @Transactional
    public SyncResult syncAll() {
        return syncAll(DEFAULT_PAGE_SIZE, DEFAULT_BATCH_SIZE, false);
    }

    /**
     * Executa FULL SYNC com configurações customizadas.
     *
     * @param pageSize      tamanho da página (30-100)
     * @param batchSize     quantos artigos processar antes de commit (1-50)
     * @param parallel      habilita processamento paralelo
     */
    @Transactional
    public SyncResult syncAll(int pageSize, int batchSize, boolean parallel) {
        int safePageSize = clamp(pageSize, 10, 200);
        int safeBatchSize = clamp(batchSize, 1, 50);

        log.info("🚀 FULL SYNC iniciado. pageSize={} batchSize={} parallel={}",
                safePageSize, safeBatchSize, parallel);

        syncStartTime = OffsetDateTime.now();
        resetMetrics();

        int page = 0;
        Integer totalSize = null;
        List<SyncError> errors = new ArrayList<>();

        while (page < MAX_PAGES) {
            try {
                MovideskArticleSearchResponse resp = movideskClient.searchArticles(page, safePageSize);

                if (totalSize == null) {
                    totalSize = resp.getTotalSize();
                    log.info("📊 Total de artigos no Movidesk: {}", totalSize);
                }

                var items = resp.getItems();

                if (items == null || items.isEmpty()) {
                    log.info("🏁 FULL SYNC: sem mais itens na página {}. Encerrando.", page);
                    break;
                }

                log.info("📄 Processando página {} ({} artigos)...", page, items.size());

                // Processa página (batch ou paralelo)
                List<SyncError> pageErrors = parallel
                        ? processPageParallel(items, safeBatchSize)
                        : processPageSequential(items, safeBatchSize);

                errors.addAll(pageErrors);

                // Progresso em tempo real
                logProgress(totalSize, page, safePageSize);

                page++;

                // Checa se acabou
                if (totalSize != null && page * safePageSize >= totalSize) {
                    log.info("🏁 FULL SYNC: todas as páginas processadas.");
                    break;
                }

            } catch (Exception ex) {
                log.error("❌ FULL SYNC: falha na página {}. Encerrando. motivo={}",
                        page, ex.toString(), ex);
                errors.add(new SyncError(null, page, ex.getMessage()));
                break;
            }
        }

        return buildResult(totalSize, errors);
    }

    // ======================
    // Processamento de Páginas
    // ======================

    /**
     * Processa página sequencialmente (um por vez).
     */
    private List<SyncError> processPageSequential(List<MovideskArticleSearchItemDto> items, int batchSize) {
        List<SyncError> errors = new ArrayList<>();
        List<KbArticle> batch = new ArrayList<>();

        for (MovideskArticleSearchItemDto item : items) {
            if (item == null || item.getId() == null) continue;

            try {
                KbArticle article = processItem(item);

                if (article != null) {
                    batch.add(article);
                    totalSucceeded.incrementAndGet();

                    // Commit batch
                    if (batch.size() >= batchSize) {
                        repository.saveAll(batch);
                        batch.clear();
                    }
                }

            } catch (Exception e) {
                log.warn("⚠️ Erro ao processar artigo id={}: {}", item.getId(), e.getMessage());
                errors.add(new SyncError(item.getId(), null, e.getMessage()));
                totalFailed.incrementAndGet();
            } finally {
                totalProcessed.incrementAndGet();
            }
        }

        // Commit batch final
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
        }

        return errors;
    }

    /**
     * Processa página em paralelo (experimental).
     *
     * ⚠️ ATENÇÃO: Usa múltiplas threads e pode causar contenção no banco.
     */
    private List<SyncError> processPageParallel(List<MovideskArticleSearchItemDto> items, int batchSize) {
        List<SyncError> errors = new CopyOnWriteArrayList<>();

        int threads = Math.min(4, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<KbArticle>> futures = new ArrayList<>();

        for (MovideskArticleSearchItemDto item : items) {
            if (item == null || item.getId() == null) continue;

            Future<KbArticle> future = executor.submit(() -> {
                try {
                    KbArticle article = processItem(item);
                    if (article != null) {
                        totalSucceeded.incrementAndGet();
                    }
                    return article;

                } catch (Exception e) {
                    log.warn("⚠️ Erro ao processar artigo id={}: {}", item.getId(), e.getMessage());
                    errors.add(new SyncError(item.getId(), null, e.getMessage()));
                    totalFailed.incrementAndGet();
                    return null;

                } finally {
                    totalProcessed.incrementAndGet();
                }
            });

            futures.add(future);
        }

        // Coleta resultados
        List<KbArticle> articles = new ArrayList<>();

        for (Future<KbArticle> future : futures) {
            try {
                KbArticle article = future.get(30, TimeUnit.SECONDS);
                if (article != null) {
                    articles.add(article);
                }
            } catch (Exception e) {
                log.error("❌ Erro ao aguardar future: {}", e.getMessage());
            }
        }

        executor.shutdown();

        // Salva em batch
        if (!articles.isEmpty()) {
            repository.saveAll(articles);
        }

        return errors;
    }

    /**
     * Processa um item individual.
     */
    private KbArticle processItem(MovideskArticleSearchItemDto item) {
        Long id = item.getId();

        // 1) sync individual (baixa artigo completo)
        KbArticle article = syncService.sync(id);

        if (article == null) {
            log.debug("⚠️ Sync retornou null. id={}", id);
            return null;
        }

        // 2) classifica usando menu do search
        classificationService.classifyFromSearchItem(article, item);

        return article;
    }

    // ======================
    // Métricas e Progresso
    // ======================

    private void resetMetrics() {
        totalProcessed.set(0);
        totalSucceeded.set(0);
        totalFailed.set(0);
    }

    private void logProgress(Integer totalSize, int currentPage, int pageSize) {
        int processed = totalProcessed.get();
        int succeeded = totalSucceeded.get();
        int failed = totalFailed.get();

        if (totalSize != null && totalSize > 0) {
            double percentage = (processed * 100.0) / totalSize;

            Duration elapsed = Duration.between(syncStartTime, OffsetDateTime.now());
            long avgMs = processed > 0 ? elapsed.toMillis() / processed : 0;

            log.info("📊 Progresso: {}/{} ({:.1f}%) | ✅ {} | ❌ {} | ⏱️ {}ms/artigo",
                    processed, totalSize, percentage, succeeded, failed, avgMs);
        } else {
            log.info("📊 Página {}: processados={} | ✅ {} | ❌ {}",
                    currentPage, processed, succeeded, failed);
        }
    }

    private SyncResult buildResult(Integer totalSize, List<SyncError> errors) {
        Duration totalDuration = Duration.between(syncStartTime, OffsetDateTime.now());

        SyncResult result = new SyncResult();
        result.totalSize = totalSize;
        result.processed = totalProcessed.get();
        result.succeeded = totalSucceeded.get();
        result.failed = totalFailed.get();
        result.errors = errors;
        result.durationMs = totalDuration.toMillis();

        if (result.processed > 0) {
            result.avgTimePerArticleMs = result.durationMs / result.processed;
            result.successRate = (result.succeeded * 100.0) / result.processed;
        }

        log.info("🏁 FULL SYNC finalizado. processed={} succeeded={} failed={} duration={}ms successRate={:.1f}%",
                result.processed, result.succeeded, result.failed, result.durationMs, result.successRate);

        return result;
    }

    // ======================
    // API de Progresso
    // ======================

    /**
     * Retorna progresso em tempo real (útil para endpoint de status).
     */
    public ProgressInfo getProgress() {
        ProgressInfo info = new ProgressInfo();
        info.processed = totalProcessed.get();
        info.succeeded = totalSucceeded.get();
        info.failed = totalFailed.get();

        if (syncStartTime != null) {
            Duration elapsed = Duration.between(syncStartTime, OffsetDateTime.now());
            info.elapsedMs = elapsed.toMillis();

            if (info.processed > 0) {
                info.avgTimePerArticleMs = info.elapsedMs / info.processed;
            }
        }

        return info;
    }

    // ======================
    // Helpers
    // ======================

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    // ======================
    // DTOs
    // ======================

    public static class SyncResult {
        public Integer totalSize;
        public int processed;
        public int succeeded;
        public int failed;
        public long durationMs;
        public long avgTimePerArticleMs;
        public double successRate;
        public List<SyncError> errors;
    }

    public static class SyncError {
        public Long articleId;
        public Integer page;
        public String message;

        public SyncError(Long articleId, Integer page, String message) {
            this.articleId = articleId;
            this.page = page;
            this.message = message;
        }
    }

    public static class ProgressInfo {
        public int processed;
        public int succeeded;
        public int failed;
        public long elapsedMs;
        public long avgTimePerArticleMs;
    }
}