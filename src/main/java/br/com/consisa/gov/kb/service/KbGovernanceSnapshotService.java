package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 📸 SERVICE DE SNAPSHOTS HISTÓRICOS
 *
 * RESPONSABILIDADES:
 * ------------------
 * ✅ Criar snapshots diários/semanais de métricas
 * ✅ Consultar tendências históricas
 * ✅ Comparar períodos
 * ✅ Gerar dados para gráficos
 *
 * MÉTRICAS CAPTURADAS:
 * --------------------
 * - Total de artigos
 * - Artigos IA-ready
 * - Score médio de qualidade
 * - Problemas por tipo (vazio, curto, duplicado, sem estrutura)
 * - Issues abertas
 * - Atribuições pendentes/concluídas
 */
@Service
public class KbGovernanceSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(KbGovernanceSnapshotService.class);

    private final KbGovernanceSnapshotRepository snapshotRepo;
    private final KbGovernanceReportRepository reportRepo;
    private final KbGovernanceIssueRepository issueRepo;
    private final KbArticleAssignmentRepository assignmentRepo;
    private final KbSystemRepository systemRepo;

    public KbGovernanceSnapshotService(
            KbGovernanceSnapshotRepository snapshotRepo,
            KbGovernanceReportRepository reportRepo,
            KbGovernanceIssueRepository issueRepo,
            KbArticleAssignmentRepository assignmentRepo,
            KbSystemRepository systemRepo
    ) {
        this.snapshotRepo = snapshotRepo;
        this.reportRepo = reportRepo;
        this.issueRepo = issueRepo;
        this.assignmentRepo = assignmentRepo;
        this.systemRepo = systemRepo;
    }

    // ======================
    // CRIAÇÃO DE SNAPSHOTS
    // ======================

    /**
     * 📸 Cria snapshot diário (global + por sistema)
     */
    @Transactional
    public void createDailySnapshot() {
        LocalDate today = LocalDate.now();

        log.info("📸 Criando snapshots diários para {}", today);

        // 1. Snapshot global
        createGlobalSnapshot(today);

        // 2. Snapshots por sistema
        var systems = systemRepo.findAll();
        int created = 0;

        for (var system : systems) {
            if (system.getIsActive()) {
                createSystemSnapshot(today, system.getCode());
                created++;
            }
        }

        log.info("✅ Snapshots criados: 1 global + {} sistemas", created);
    }

    /**
     * 📸 Cria snapshot semanal (mais detalhado)
     */
    @Transactional
    public void createWeeklySnapshot() {
        createDailySnapshot(); // mesma lógica, mas em dia específico
    }

    /**
     * 📸 Cria snapshot GLOBAL
     */
    @Transactional
    public KbGovernanceSnapshot createGlobalSnapshot(LocalDate date) {
        log.debug("📸 Criando snapshot global para {}", date);

        var snapshot = new KbGovernanceSnapshot();
        snapshot.setSnapshotDate(date);
        snapshot.setSystemCode(null); // global

        // Busca métricas do report
        Object[] summaryStats = reportRepo.getSummaryStats();

        if (summaryStats != null && summaryStats.length > 0) {
            Object[] stats = summaryStats;

            snapshot.setTotalArticles(toInt(stats[0]));
            snapshot.setEmptyCount(toInt(stats[1]));
            snapshot.setShortCount(toInt(stats[2]));
            snapshot.setDuplicateCount(toInt(stats[3]));
            snapshot.setNoStructureCount(toInt(stats[5]));
            snapshot.setIaReadyCount(toInt(stats[6]));

            // Score médio (aproximação)
            double scoreApprox = calculateAverageQualityScore();
            snapshot.setAvgQualityScore(scoreApprox);
        }

        // Issues abertas
        long openIssues = issueRepo.count();
        snapshot.setOpenIssuesCount((int) openIssues);

        // Atribuições
        List<Object[]> assignmentStats = assignmentRepo.getStatistics();
        if (assignmentStats.size() > 0) {
            Object[] aStats = assignmentStats.get(0);
            snapshot.setPendingAssignments(toInt(aStats[1]));  // PENDING
            snapshot.setCompletedAssignments(toInt(aStats[3])); // COMPLETED
        } else {
            snapshot.setPendingAssignments(0);
            snapshot.setCompletedAssignments(0);
        }

        var saved = snapshotRepo.save(snapshot);

        log.info("✅ Snapshot global salvo: {} artigos, {} IA-ready ({}%)",
                saved.getTotalArticles(),
                saved.getIaReadyCount(),
                String.format("%.1f", saved.getIaReadyPercentage()));

        return saved;
    }

    /**
     * 📸 Cria snapshot de um SISTEMA específico
     */
    @Transactional
    public KbGovernanceSnapshot createSystemSnapshot(LocalDate date, String systemCode) {
        log.debug("📸 Criando snapshot para sistema {} em {}", systemCode, date);

        var snapshot = new KbGovernanceSnapshot();
        snapshot.setSnapshotDate(date);
        snapshot.setSystemCode(systemCode);

        // Busca report do sistema
        List<Object[]> systemReport = reportRepo.findBySystemCode(systemCode);

        if (systemReport.size() == 0) {
            log.warn("⚠️ Nenhum dado encontrado para sistema {}", systemCode);
            snapshot.setTotalArticles(0);
            snapshot.setIaReadyCount(0);
            snapshot.setAvgQualityScore(0.0);
            return snapshotRepo.save(snapshot);
        }

        // Contadores
        int total = systemReport.size();
        int empty = 0, tooShort = 0, duplicate = 0, noStructure = 0, iaReady = 0;

        for (Object[] row : systemReport) {
            if (Boolean.TRUE.equals(row[7])) empty++;
            if (Boolean.TRUE.equals(row[8])) tooShort++;
            if (Boolean.TRUE.equals(row[9])) duplicate++;
            if (Boolean.TRUE.equals(row[11])) noStructure++;

            // IA-ready: sem problemas críticos
            boolean hasProblems = Boolean.TRUE.equals(row[7]) ||
                    Boolean.TRUE.equals(row[9]) ||
                    Boolean.TRUE.equals(row[11]);
            if (!hasProblems) iaReady++;
        }

        snapshot.setTotalArticles(total);
        snapshot.setEmptyCount(empty);
        snapshot.setShortCount(tooShort);
        snapshot.setDuplicateCount(duplicate);
        snapshot.setNoStructureCount(noStructure);
        snapshot.setIaReadyCount(iaReady);

        // Score aproximado
        double scoreApprox = total > 0 ? ((double) iaReady / total) * 100 : 0.0;
        snapshot.setAvgQualityScore(scoreApprox);

        // Atribuições do sistema (aproximação)
        snapshot.setPendingAssignments(0);  // TODO: filtrar por sistema
        snapshot.setCompletedAssignments(0);

        return snapshotRepo.save(snapshot);
    }

    // ======================
    // CONSULTAS DE TENDÊNCIAS
    // ======================

    /**
     * 📈 Retorna tendência dos últimos N dias (global)
     */
    @Transactional(readOnly = true)
    public List<KbGovernanceSnapshot> getTrend(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        return snapshotRepo.findGlobalSnapshotsBetween(startDate, endDate);
    }

    /**
     * 📈 Retorna tendência de um sistema
     */
    @Transactional(readOnly = true)
    public List<KbGovernanceSnapshot> getSystemTrend(String systemCode, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        return snapshotRepo.findSystemSnapshotsBetween(startDate, endDate, systemCode);
    }

    /**
     * 📊 Compara dois períodos (ex: semana atual vs semana passada)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> comparePeriods(int periodDays) {
        LocalDate today = LocalDate.now();

        // Período atual
        LocalDate currentStart = today.minusDays(periodDays);
        var currentSnaps = snapshotRepo.findGlobalSnapshotsBetween(currentStart, today);

        // Período anterior
        LocalDate previousStart = currentStart.minusDays(periodDays);
        LocalDate previousEnd = currentStart.minusDays(1);
        var previousSnaps = snapshotRepo.findGlobalSnapshotsBetween(previousStart, previousEnd);

        // Médias
        double currentAvgIaReady = currentSnaps.stream()
                .mapToDouble(KbGovernanceSnapshot::getIaReadyPercentage)
                .average()
                .orElse(0.0);

        double previousAvgIaReady = previousSnaps.stream()
                .mapToDouble(KbGovernanceSnapshot::getIaReadyPercentage)
                .average()
                .orElse(0.0);

        double change = currentAvgIaReady - previousAvgIaReady;

        Map<String, Object> result = new HashMap<>();
        result.put("currentPeriod", currentSnaps);
        result.put("previousPeriod", previousSnaps);
        result.put("currentAvgIaReady", currentAvgIaReady);
        result.put("previousAvgIaReady", previousAvgIaReady);
        result.put("change", change);
        result.put("improving", change > 0);

        return result;
    }

    /**
     * 📊 Retorna dados formatados para gráficos (Chart.js, etc)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChartData(int days) {
        var snapshots = getTrend(days);

        List<String> dates = new ArrayList<>();
        List<Integer> totalArticles = new ArrayList<>();
        List<Integer> iaReady = new ArrayList<>();
        List<Double> iaReadyPercentage = new ArrayList<>();
        List<Integer> openIssues = new ArrayList<>();

        for (var snap : snapshots) {
            dates.add(snap.getSnapshotDate().toString());
            totalArticles.add(snap.getTotalArticles());
            iaReady.add(snap.getIaReadyCount());
            iaReadyPercentage.add(snap.getIaReadyPercentage());
            openIssues.add(snap.getOpenIssuesCount());
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", dates);
        chartData.put("totalArticles", totalArticles);
        chartData.put("iaReady", iaReady);
        chartData.put("iaReadyPercentage", iaReadyPercentage);
        chartData.put("openIssues", openIssues);

        return chartData;
    }

    // ======================
    // HELPERS
    // ======================

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof java.math.BigInteger) return ((java.math.BigInteger) obj).intValue();
        return 0;
    }

    private double calculateAverageQualityScore() {
        List<Object[]> iaReadyArticles = reportRepo.findIaReady();
        if (iaReadyArticles.size() == 0) return 0.0;

        // Aproximação: conta headers como proxy de qualidade
        double avgHeaders = iaReadyArticles.stream()
                .mapToInt(row -> toInt(row[16]))  // header_count
                .average()
                .orElse(0.0);

        // Normaliza para escala 0-100
        return Math.min(avgHeaders * 10, 100);
    }
}