package br.com.consisa.gov.kb.scheduler;

import br.com.consisa.gov.kb.governance.KbGovernanceDetectorService;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.KbGovernanceSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 🕐 SCHEDULER DE GOVERNANÇA DIÁRIA
 *
 * RESPONSABILIDADES:
 * ------------------
 * ✅ Executar análise de qualidade automaticamente
 * ✅ Detectar problemas (vazios, duplicados, etc)
 * ✅ Gerar snapshot diário de métricas
 * ✅ Rodar em horário de menor impacto
 *
 * QUANDO EXECUTA:
 * ----------------
 * - Segunda a Sexta: 02:00 (madrugada)
 * - Análise completa: artigos recentes (últimos 30 dias)
 * - Snapshot: todos os artigos ativos
 *
 * MÉTRICAS COLETADAS:
 * -------------------
 * - Total de artigos
 * - IA-Ready count
 * - Score médio de qualidade
 * - Issues abertas
 * - Por sistema
 */
@Component
public class KbGovernanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(KbGovernanceScheduler.class);

    private static final int RECENT_DAYS = 30;
    private static final int ANALYSIS_BATCH_SIZE = 100;

    private final KbArticleRepository articleRepo;
    private final KbGovernanceDetectorService detectorService;
    private final KbGovernanceSnapshotService snapshotService;

    public KbGovernanceScheduler(
            KbArticleRepository articleRepo,
            KbGovernanceDetectorService detectorService,
            KbGovernanceSnapshotService snapshotService
    ) {
        this.articleRepo = articleRepo;
        this.detectorService = detectorService;
        this.snapshotService = snapshotService;
    }

    // ======================
    // ANÁLISE DIÁRIA
    // ======================

    /**
     * 🌙 Análise diária de governança
     *
     * Executa: Segunda a Sexta às 02:00
     *
     * O que faz:
     * 1. Analisa artigos recentes (últimos 30 dias)
     * 2. Detecta conteúdo incompleto
     * 3. Detecta duplicados
     * 4. Gera snapshot de métricas
     */
    @Scheduled(cron = "0 0 2 * * MON-FRI", zone = "America/Sao_Paulo")
    public void runDailyGovernanceAnalysis() {
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);

        log.info("🌙 ========================================");
        log.info("🌙 INICIANDO ANÁLISE DIÁRIA DE GOVERNANÇA");
        log.info("🌙 ========================================");

        try {
            // 1. Analisa artigos recentes
            int recentAnalyzed = analyzeRecentArticles();

            // 2. Detecta duplicados em todo o banco
            int duplicatesFound = detectorService.analyzeAllDuplicates();

            // 3. Gera snapshot de métricas
            snapshotService.createDailySnapshot();

            OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);
            Duration duration = Duration.between(started, finished);

            log.info("✅ ========================================");
            log.info("✅ ANÁLISE DIÁRIA CONCLUÍDA");
            log.info("✅ Artigos recentes analisados: {}", recentAnalyzed);
            log.info("✅ Duplicados processados: {}", duplicatesFound);
            log.info("✅ Duração: {}min {}s", duration.toMinutes(), duration.getSeconds() % 60);
            log.info("✅ ========================================");

        } catch (Exception e) {
            log.error("❌ ERRO NA ANÁLISE DIÁRIA: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔄 Análise semanal completa
     *
     * Executa: Domingos às 03:00
     *
     * Mais pesada que a diária, analisa TODOS os artigos ativos.
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = "America/Sao_Paulo")
    public void runWeeklyFullAnalysis() {
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);

        log.info("📅 ========================================");
        log.info("📅 INICIANDO ANÁLISE SEMANAL COMPLETA");
        log.info("📅 ========================================");

        try {
            // Analisa TODOS os artigos ativos
            int totalAnalyzed = analyzeAllActiveArticles();

            // Detecta duplicados
            int duplicatesFound = detectorService.analyzeAllDuplicates();

            // Snapshot semanal
            snapshotService.createWeeklySnapshot();

            OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);
            Duration duration = Duration.between(started, finished);

            log.info("✅ ========================================");
            log.info("✅ ANÁLISE SEMANAL CONCLUÍDA");
            log.info("✅ Total de artigos analisados: {}", totalAnalyzed);
            log.info("✅ Duplicados processados: {}", duplicatesFound);
            log.info("✅ Duração: {}min {}s", duration.toMinutes(), duration.getSeconds() % 60);
            log.info("✅ ========================================");

        } catch (Exception e) {
            log.error("❌ ERRO NA ANÁLISE SEMANAL: {}", e.getMessage(), e);
        }
    }

    // ======================
    // MÉTODOS PRIVADOS
    // ======================

    /**
     * Analisa artigos alterados recentemente (últimos 30 dias).
     */
    private int analyzeRecentArticles() {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusDays(RECENT_DAYS);

        var recentArticles = articleRepo.findRecent(
                PageRequest.of(0, ANALYSIS_BATCH_SIZE)
        );

        int count = 0;

        for (var article : recentArticles) {
            if (article.getUpdatedDate() != null && article.getUpdatedDate().isAfter(since)) {
                detectorService.analyzeArticle(article);
                count++;
            }
        }

        log.info("📊 Artigos recentes analisados: {}/{}", count, recentArticles.getNumberOfElements());

        return count;
    }

    /**
     * Analisa TODOS os artigos ativos (usado na análise semanal).
     */
    private int analyzeAllActiveArticles() {
        int page = 0;
        int count = 0;
        boolean hasMore = true;

        while (hasMore) {
            var articles = articleRepo.findRecent(
                    PageRequest.of(page, ANALYSIS_BATCH_SIZE)
            );

            if (articles.isEmpty()) {
                hasMore = false;
            } else {
                articles.forEach(article -> {
                    detectorService.analyzeArticle(article);
                });

                count += articles.getNumberOfElements();
                page++;

                log.info("📊 Progresso: {} artigos analisados...", count);
            }
        }

        return count;
    }
}
