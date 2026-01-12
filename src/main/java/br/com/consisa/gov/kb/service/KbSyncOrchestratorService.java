package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbSyncConfigRepository;
import br.com.consisa.gov.kb.repository.KbSyncRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class KbSyncOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(KbSyncOrchestratorService.class);

    private final KbSyncConfigRepository configRepo;
    private final KbSyncRunRepository runRepo;
    private final KbArticleRepository articleRepo;
    private final KbArticleSyncService articleSyncService;

    public KbSyncOrchestratorService(
            KbSyncConfigRepository configRepo,
            KbSyncRunRepository runRepo,
            KbArticleRepository articleRepo,
            KbArticleSyncService articleSyncService
    ) {
        this.configRepo = configRepo;
        this.runRepo = runRepo;
        this.articleRepo = articleRepo;
        this.articleSyncService = articleSyncService;
    }

    @Transactional(readOnly = true)
    public KbSyncConfig getConfig() {
        return configRepo.findById(1L).orElseGet(KbSyncConfig::new);
    }

    @Transactional
    public KbSyncConfig updateConfig(KbSyncConfig incoming) {
        KbSyncConfig cfg = configRepo.findById(1L).orElseGet(KbSyncConfig::new);

        cfg.setEnabled(incoming.isEnabled());
        cfg.setMode(incoming.getMode() == null ? SyncMode.DELTA_WINDOW : incoming.getMode());

        // guard rails
        int interval = Math.max(1, incoming.getIntervalMinutes());
        int daysBack = Math.max(0, incoming.getDaysBack());

        cfg.setIntervalMinutes(interval);
        cfg.setDaysBack(daysBack);

        return configRepo.save(cfg);
    }

    @Transactional(readOnly = true)
    public KbSyncRun latestRun() {
        return runRepo.findTop1ByOrderByStartedAtDesc().orElse(null);
    }

    /**
     * Roda um sync “manual” (endpoint) ou “automático” (scheduler).
     */
    @Transactional
    public KbSyncRun runNow(SyncMode mode, Integer daysBack) {
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);

        KbSyncRun run = new KbSyncRun();
        run.setStartedAt(started);
        run.setMode(mode);
        run.setDaysBack(daysBack);
        run.setStatus(SyncRunStatus.RUNNING);
        run = runRepo.save(run);

        try {
            ResultCounts counts = (mode == SyncMode.FULL)
                    ? runFull(countsInit())
                    : runDeltaWindow(daysBack == null ? 2 : daysBack, countsInit());

            OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);
            run.setFinishedAt(finished);
            run.setDurationMs(Duration.between(started, finished).toMillis());
            run.setStatus(SyncRunStatus.SUCCESS);

            run.setSyncedCount(counts.synced);
            run.setUpdatedCount(counts.updated);
            run.setSkippedCount(counts.skipped);
            run.setNotFoundCount(counts.notFound);
            run.setErrorCount(counts.errors);

            // marca timestamps no config
            KbSyncConfig cfg = configRepo.findById(1L).orElseGet(KbSyncConfig::new);
            cfg.setLastStartedAt(started);
            cfg.setLastFinishedAt(finished);
            configRepo.save(cfg);

            return runRepo.save(run);

        } catch (Exception e) {
            OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);
            run.setFinishedAt(finished);
            run.setDurationMs(Duration.between(started, finished).toMillis());
            run.setStatus(SyncRunStatus.FAILED);
            run.setNote(trunc(e.getMessage(), 350));
            runRepo.save(run);
            throw e;
        }
    }

    // ==============================
    // Estratégias de sync
    // ==============================

    private ResultCounts runFull(ResultCounts c) {
        // FULL = usa teu syncAll atual (paginado no Movidesk)
        // (se você quiser, depois a gente move a lógica do loop pra cá e deixa o ArticleSync só com sync(id)+classificação)
        articleSyncService.syncAll();
        c.synced += 1; // marcador simples (depois dá pra contar de verdade)
        return c;
    }

    private ResultCounts runDeltaWindow(int daysBack, ResultCounts c) {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysBack);

        // 1) pega candidatos por janela (local)
        List<Long> deltaIds = articleRepo.findDeltaIds(since);

        // 2) opcional: reforça “problemas” (ex: GERAL) pra reclassificar se menu_map mudou
        List<Long> geralIds = articleRepo.findIdsInGeral();

        Set<Long> ids = new HashSet<>(deltaIds);
        ids.addAll(geralIds);

        log.info("🟦 DELTA_WINDOW daysBack={} candidates={}", daysBack, ids.size());

        for (Long id : ids) {
            try {
                var saved = articleSyncService.sync(id); // puxa artigo completo
                if (saved == null) {
                    c.notFound++;
                } else {
                    c.updated++; // aqui “updated” = reprocessado
                }
            } catch (Exception ex) {
                c.errors++;
            }
        }

        return c;
    }

    // ==============================
    // helpers
    // ==============================

    private static ResultCounts countsInit() { return new ResultCounts(); }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static class ResultCounts {
        int synced;
        int updated;
        int skipped;
        int notFound;
        int errors;
    }
}
