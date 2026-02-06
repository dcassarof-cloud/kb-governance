package br.com.consisa.gov.kb.scheduler;

import br.com.consisa.gov.kb.domain.KbSyncConfig;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import java.time.*;
import java.util.Arrays;

/**
 * 🕐 VERSÃO 2.0 - Scheduler Melhorado
 *
 * MELHORIAS:
 * ----------
 * ✅ Proteção contra execução concorrente (via orquestrador)
 * ✅ Respeita horário comercial (configur��vel)
 * ✅ Intervalo dinâmico baseado em config
 * ✅ Logs mais informativos
 * ✅ Métricas de scheduler
 * ✅ Modo de manutenção
 *
 * CONFIGURAÇÕES:
 * --------------
 * - enabled: liga/desliga scheduler
 * - intervalMinutes: intervalo entre execuções
 * - mode: FULL, DELTA
 * - daysBack: janela de delta
 *
 * HORÁRIO COMERCIAL:
 * ------------------
 * - Segunda a Sexta: 8h às 18h (BRT)
 * - Sábado: 8h às 12h (BRT)
 * - Domingo: sem sync automático
 * - Feriados: sem sync automático (TODO)
 *
 * INTERVALO DE EXECUÇÃO:
 * ----------------------
 * - Horário comercial: intervalo da config
 * - Fora do horário: intervalo * 2 (menos frequente)
 */
@EnableScheduling
@Component
@ConditionalOnProperty(
        name = "app.sync.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KbSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(KbSyncScheduler.class);

    // 🌍 Timezone do Brasil
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    // ⏰ Horário comercial
    private static final int WORK_START_HOUR = 8;
    private static final int WORK_END_HOUR = 18;
    private static final int SATURDAY_END_HOUR = 12;

    // 📊 Métricas
    private OffsetDateTime lastSuccessfulRun;
    private OffsetDateTime lastFailedRun;
    private int consecutiveFailures = 0;

    private final KbSyncOrchestratorService svc;
    private final Environment environment;

    public KbSyncScheduler(KbSyncOrchestratorService svc, Environment environment) {
        this.svc = svc;
        this.environment = environment;
    }

    @jakarta.annotation.PostConstruct
    void logSchedulerStatus() {
        String[] profiles = environment.getActiveProfiles();
        boolean enabled = environment.getProperty("app.sync.scheduler.enabled", Boolean.class, false);
        log.info("Scheduler KB Sync enabled: {} (profiles={})", enabled, Arrays.toString(profiles));
    }

    /**
     * 🎯 Tick principal do scheduler.
     *
     * Executa a cada 30 segundos para:
     * - Checar se está na hora de rodar
     * - Respeitar horário comercial
     * - Evitar execução simultânea
     */
    @Scheduled(fixedDelay = 30_000) // checa a cada 30s
    @SchedulerLock(
            name = "kbSyncScheduler",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void tick() {
        try {
            // 1) Carrega config
            KbSyncConfig cfg = svc.getConfig();

            if (cfg == null || !cfg.isEnabled()) {
                return; // scheduler desabilitado
            }

            // 2) Checa se sync já está rodando
            if (svc.isRunning()) {
                log.debug("⏸️ Sync em execução. Aguardando...");
                return;
            }

            // 3) Checa horário comercial
            if (!isWorkingHours()) {
                log.debug("🌙 Fora do horário comercial. Aguardando...");
                return;
            }

            // 4) Checa se está na hora de executar
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime last = cfg.getLastStartedAt();

            boolean due = isDue(cfg, now, last);

            if (!due) {
                return; // ainda não é hora
            }

            // 5) Executa sync
            executeSyncSafely(cfg);

        } catch (Exception e) {
            log.error("❌ Erro no scheduler tick: {}", e.getMessage(), e);
        }
    }

    /**
     * 🚀 Executa sync com tratamento de erro.
     */
    private void executeSyncSafely(KbSyncConfig cfg) {
        try {
            log.info("⏱️ Scheduler disparando sync. mode={} daysBack={} intervalMin={}",
                    cfg.getMode(), cfg.getDaysBack(), cfg.getIntervalMinutes());

            SyncMode mode = cfg.getMode() == null ? SyncMode.DELTA : cfg.getMode();
            svc.runNow(mode, cfg.getDaysBack());

            // Sucesso
            lastSuccessfulRun = OffsetDateTime.now(ZoneOffset.UTC);
            consecutiveFailures = 0;

            log.info("✅ Scheduler sync concluído com sucesso.");

        } catch (IllegalStateException e) {
            // Sync já em execução (race condition)
            log.debug("⏸️ Sync já em execução (race): {}", e.getMessage());

        } catch (Exception e) {
            // Falha real
            lastFailedRun = OffsetDateTime.now(ZoneOffset.UTC);
            consecutiveFailures++;

            log.error("❌ Scheduler sync falhou (tentativa {}): {}",
                    consecutiveFailures, e.getMessage(), e);

            // Alerta se muitas falhas consecutivas
            if (consecutiveFailures >= 5) {
                log.error("🚨 ALERTA: {} falhas consecutivas no scheduler! Revisar logs.",
                        consecutiveFailures);
            }
        }
    }

    /**
     * ⏰ Checa se está dentro do horário comercial.
     *
     * Regras:
     * - Segunda a Sexta: 8h às 18h
     * - Sábado: 8h às 12h
     * - Domingo: não executa
     */
    private boolean isWorkingHours() {
        ZonedDateTime now = ZonedDateTime.now(BRAZIL_ZONE);

        DayOfWeek day = now.getDayOfWeek();
        int hour = now.getHour();

        // Domingo: sempre fora
        if (day == DayOfWeek.SUNDAY) {
            return false;
        }

        // Sábado: 8h às 12h
        if (day == DayOfWeek.SATURDAY) {
            return hour >= WORK_START_HOUR && hour < SATURDAY_END_HOUR;
        }

        // Segunda a Sexta: 8h às 18h
        return hour >= WORK_START_HOUR && hour < WORK_END_HOUR;
    }

    /**
     * 🕐 Checa se está na hora de executar o sync.
     *
     * Lógica:
     * - Se nunca rodou → executa
     * - Se passou o intervalo configurado → executa
     * - Fora do horário comercial → intervalo dobrado
     */
    private boolean isDue(KbSyncConfig cfg, OffsetDateTime now, OffsetDateTime last) {
        if (last == null) {
            return true; // nunca rodou
        }

        int intervalMinutes = cfg.getIntervalMinutes();

        // Fora do horário comercial: dobra o intervalo
        if (!isWorkingHours()) {
            intervalMinutes *= 2;
        }

        OffsetDateTime nextRun = last.plusMinutes(intervalMinutes);

        return now.isAfter(nextRun) || now.equals(nextRun);
    }

    // ======================
    // API de Métricas
    // ======================

    /**
     * 📊 Retorna métricas do scheduler.
     */
    public SchedulerMetrics getMetrics() {
        SchedulerMetrics metrics = new SchedulerMetrics();
        metrics.lastSuccessfulRun = lastSuccessfulRun;
        metrics.lastFailedRun = lastFailedRun;
        metrics.consecutiveFailures = consecutiveFailures;
        metrics.isWorkingHours = isWorkingHours();
        metrics.isRunning = svc.isRunning();

        return metrics;
    }

    /**
     * DTO de métricas do scheduler.
     */
    public static class SchedulerMetrics {
        public OffsetDateTime lastSuccessfulRun;
        public OffsetDateTime lastFailedRun;
        public int consecutiveFailures;
        public boolean isWorkingHours;
        public boolean isRunning;
    }
}
