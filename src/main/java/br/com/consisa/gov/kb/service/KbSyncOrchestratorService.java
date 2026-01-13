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
import java.util.List;

@Service
public class KbSyncOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(KbSyncOrchestratorService.class);

    private static final int DEFAULT_FALLBACK_DAYS = 2;
    private static final int MAX_LOOKBACK_DAYS = 7;

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
        cfg.setIntervalMinutes(Math.max(1, incoming.getIntervalMinutes()));
        cfg.setDaysBack(Math.max(0, incoming.getDaysBack()));

        return configRepo.save(cfg);
    }

    @Transactional(readOnly = true)
    public KbSyncRun latestRun() {
        return runRepo.findTop1ByOrderByStartedAtDesc().orElse(null);
    }

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
                    : runDelta(countsInit(), daysBack);

            OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);

            run.setFinishedAt(finished);
            run.setDurationMs(Duration.between(started, finished).toMillis());
            run.setStatus(SyncRunStatus.SUCCESS);

            run.setSyncedCount(counts.synced);
            run.setUpdatedCount(counts.updated);
            run.setSkippedCount(counts.skipped);
            run.setNotFoundCount(counts.notFound);
            run.setErrorCount(counts.errors);

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

    // ======================
    // Estratégias
    // ======================

    private ResultCounts runFull(ResultCounts c) {
        articleSyncService.syncAllFull(); // ✅ FULL real
        c.synced = 1;
        return c;
    }


    private ResultCounts runDelta(ResultCounts c, Integer daysBack) {
        OffsetDateTime since = computeSince(daysBack);
        List<Long> ids = articleRepo.findDeltaIdsAfter(since);

        log.info("🟦 DELTA_SINCE since={} candidates={}", since, ids.size());

        for (Long id : ids) {
            try {
                var saved = articleSyncService.sync(id);
                if (saved == null) c.notFound++;
                else c.updated++;
            } catch (Exception ex) {
                c.errors++;
            }
        }

        return c;
    }

    // ======================
    // Helpers
    // ======================

    private OffsetDateTime computeSince(Integer daysBack) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (daysBack != null) {
            return clamp(now.minusDays(daysBack), now);
        }

        OffsetDateTime since = runRepo
                .findTop1ByStatusOrderByFinishedAtDesc(SyncRunStatus.SUCCESS)
                .map(KbSyncRun::getFinishedAt)
                .orElse(now.minusDays(DEFAULT_FALLBACK_DAYS));

        return clamp(since, now);
    }

    private OffsetDateTime clamp(OffsetDateTime since, OffsetDateTime now) {
        OffsetDateTime min = now.minusDays(MAX_LOOKBACK_DAYS);
        return since.isBefore(min) ? min : since;
    }

    private static ResultCounts countsInit() {
        return new ResultCounts();
    }

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
