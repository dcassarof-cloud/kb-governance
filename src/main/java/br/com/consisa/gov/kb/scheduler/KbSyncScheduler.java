package br.com.consisa.gov.kb.scheduler;

import br.com.consisa.gov.kb.domain.KbSyncConfig;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@EnableScheduling
@Component
public class KbSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(KbSyncScheduler.class);

    private final KbSyncOrchestratorService svc;

    public KbSyncScheduler(KbSyncOrchestratorService svc) {
        this.svc = svc;
    }

    @Scheduled(fixedDelay = 30_000) // checa a cada 30s
    public void tick() {
        KbSyncConfig cfg = svc.getConfig();
        if (cfg == null || !cfg.isEnabled()) return;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime last = cfg.getLastStartedAt();

        boolean due;
        if (last == null) {
            due = true;
        } else {
            due = last.plusMinutes(cfg.getIntervalMinutes()).isBefore(now);
        }

        if (!due) return;

        try {
            log.info("⏱️ Scheduler disparando sync mode={} daysBack={} intervalMin={}",
                    cfg.getMode(), cfg.getDaysBack(), cfg.getIntervalMinutes());

            SyncMode mode = cfg.getMode() == null ? SyncMode.DELTA_WINDOW : cfg.getMode();
            svc.runNow(mode, cfg.getDaysBack());

        } catch (Exception e) {
            log.error("❌ Scheduler sync failed: {}", e.toString(), e);
        }
    }
}
