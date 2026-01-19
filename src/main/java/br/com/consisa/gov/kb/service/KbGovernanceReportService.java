package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.KbArticleGovernanceReportDto;
import br.com.consisa.gov.kb.repository.KbGovernanceReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de relatório de governança
 *
 * Responsabilidades:
 * - Consultar a VIEW
 * - Transformar Object[] em DTO
 * - Calcular lista de ações
 * - Calcular score e IA-ready flag
 */
@Service
public class KbGovernanceReportService {

    private static final Logger log = LoggerFactory.getLogger(KbGovernanceReportService.class);

    private final KbGovernanceReportRepository repository;

    public KbGovernanceReportService(KbGovernanceReportRepository repository) {
        this.repository = repository;
    }

    /**
     * Gera relatório completo
     */
    @Transactional(readOnly = true)
    public List<KbArticleGovernanceReportDto> generateFullReport() {
        log.info("📊 Gerando relatório completo de governança...");

        List<Object[]> rows = repository.findAllReport();
        List<KbArticleGovernanceReportDto> report = transformRows(rows);

        log.info("✅ Relatório gerado: {} artigos analisados", report.size());
        return report;
    }

    /**
     * Gera relatório filtrado por sistema
     */
    @Transactional(readOnly = true)
    public List<KbArticleGovernanceReportDto> generateReportBySystem(String systemCode) {
        log.info("📊 Gerando relatório para sistema: {}", systemCode);

        List<Object[]> rows = repository.findBySystemCode(systemCode);
        List<KbArticleGovernanceReportDto> report = transformRows(rows);

        log.info("✅ Relatório gerado: {} artigos do sistema {}", report.size(), systemCode);
        return report;
    }

    /**
     * Gera relatório apenas de artigos com problemas
     */
    @Transactional(readOnly = true)
    public List<KbArticleGovernanceReportDto> generateIssuesOnlyReport() {
        log.info("📊 Gerando relatório de artigos com problemas...");

        List<Object[]> rows = repository.findWithIssuesOnly();
        List<KbArticleGovernanceReportDto> report = transformRows(rows);

        log.info("✅ Encontrados {} artigos com problemas", report.size());
        return report;
    }

    /**
     * Gera relatório de artigos IA-Ready
     */
    @Transactional(readOnly = true)
    public List<KbArticleGovernanceReportDto> generateIaReadyReport() {
        log.info("📊 Gerando relatório de artigos IA-Ready...");

        List<Object[]> rows = repository.findIaReady();
        List<KbArticleGovernanceReportDto> report = transformRows(rows);

        log.info("✅ Encontrados {} artigos IA-Ready", report.size());
        return report;
    }

    /**
     * Retorna estatísticas agregadas
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSummaryStatistics() {
        log.info("📊 Gerando estatísticas de governança...");

        Object[] stats = repository.getSummaryStats();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", convertToLong(stats[0]));
        summary.put("emptyCount", convertToLong(stats[1]));
        summary.put("shortCount", convertToLong(stats[2]));
        summary.put("duplicateCount", convertToLong(stats[3]));
        summary.put("hashReusedCount", convertToLong(stats[4]));
        summary.put("noStructureCount", convertToLong(stats[5]));
        summary.put("iaReadyCount", convertToLong(stats[6]));

        long total = (Long) summary.get("total");
        long iaReady = (Long) summary.get("iaReadyCount");

        if (total > 0) {
            double percentage = (iaReady * 100.0) / total;
            summary.put("iaReadyPercentage", Math.round(percentage * 100.0) / 100.0);
        } else {
            summary.put("iaReadyPercentage", 0.0);
        }

        log.info("✅ Estatísticas: {} total, {} IA-Ready ({}%)",
                total, iaReady, summary.get("iaReadyPercentage"));

        return summary;
    }

    // ===========================
    // Métodos Privados
    // ===========================

    /**
     * Transforma linhas do banco em DTOs
     */
    private List<KbArticleGovernanceReportDto> transformRows(List<Object[]> rows) {
        List<KbArticleGovernanceReportDto> result = new ArrayList<>();

        for (Object[] row : rows) {
            try {
                KbArticleGovernanceReportDto dto = mapRowToDto(row);
                populateActions(dto);
                dto.calculateIaReady();
                dto.calculateQualityScore();
                result.add(dto);
            } catch (Exception e) {
                log.error("❌ Erro ao processar linha do relatório: {}", e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * Mapeia Object[] da query nativa para DTO
     */
    private KbArticleGovernanceReportDto mapRowToDto(Object[] row) {
        KbArticleGovernanceReportDto dto = new KbArticleGovernanceReportDto();

        int i = 0;
        dto.setArticleId(convertToLong(row[i++]));
        dto.setSystemCode((String) row[i++]);
        dto.setSystemName((String) row[i++]);
        dto.setTitle((String) row[i++]);
        dto.setContentHash((String) row[i++]);
        dto.setSourceUrl((String) row[i++]);
        dto.setUpdatedDate(convertToOffsetDateTime(row[i++]));
        dto.setIsEmpty((Boolean) row[i++]);
        dto.setIsTooShort((Boolean) row[i++]);
        dto.setIsDuplicateSameSystem((Boolean) row[i++]);
        dto.setIsHashReusedOtherSystem((Boolean) row[i++]);
        dto.setLacksMinStructure((Boolean) row[i++]);
        dto.setContentLength(convertToInteger(row[i++]));
        dto.setHeaderCount(convertToInteger(row[i++]));
        dto.setHasLists((Boolean) row[i++]);
        dto.setHasActionVerbs((Boolean) row[i++]);
        dto.setHasSystemContext((Boolean) row[i++]);

        return dto;
    }

    /**
     * Popula lista de ações com base nas flags
     */
    private void populateActions(KbArticleGovernanceReportDto dto) {
        if (Boolean.TRUE.equals(dto.getIsEmpty())) {
            dto.addAction(KbArticleGovernanceReportDto.ACTION_MANUAL_VAZIO);
        }

        if (Boolean.TRUE.equals(dto.getIsTooShort())) {
            dto.addAction(KbArticleGovernanceReportDto.ACTION_MANUAL_CURTO);
        }

        if (Boolean.TRUE.equals(dto.getIsDuplicateSameSystem())) {
            dto.addAction(KbArticleGovernanceReportDto.ACTION_DUPLICADO_MESMO_SISTEMA);
        }

        if (Boolean.TRUE.equals(dto.getIsHashReusedOtherSystem())) {
            dto.addAction(KbArticleGovernanceReportDto.ACTION_HASH_OUTRO_SISTEMA);
        }

        if (Boolean.TRUE.equals(dto.getLacksMinStructure())) {
            dto.addAction(KbArticleGovernanceReportDto.ACTION_SEM_ESTRUTURA);
        }
    }

    /**
     * Conversões seguras de tipos do banco
     */
    private Long convertToLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof BigInteger) return ((BigInteger) obj).longValue();
        return null;
    }

    private Integer convertToInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof BigInteger) return ((BigInteger) obj).intValue();
        return null;
    }

    private OffsetDateTime convertToOffsetDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        }
        if (obj instanceof OffsetDateTime) return (OffsetDateTime) obj;
        return null;
    }
}