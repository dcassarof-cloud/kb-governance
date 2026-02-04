package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceAssignmentStatus;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceResponsibleType;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.GovernanceStatus;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.domain.KbManualAssigneeType;
import br.com.consisa.gov.kb.domain.KbManualPriority;
import br.com.consisa.gov.kb.domain.KbManualRiskLevel;
import br.com.consisa.gov.kb.domain.KbManualTaskStatus;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.domain.SyncRunStatus;
import br.com.consisa.gov.kb.dto.GovernanceLabelDto;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class GovernanceLanguageService {

    private static final String DEFAULT_IMPACT_SUMMARY = "Impacto em avaliação.";
    private static final String DEFAULT_BUSINESS_IMPACT_LEVEL = "LOW";

    private final IssueTypeMetaRegistry issueTypeMetaRegistry;

    public GovernanceLanguageService(IssueTypeMetaRegistry issueTypeMetaRegistry) {
        this.issueTypeMetaRegistry = issueTypeMetaRegistry;
    }

    public GovernanceLabelDto issueTypeLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return issueTypeLabel(KbGovernanceIssueType.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Pendência de Governança.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto issueTypeLabel(KbGovernanceIssueType type) {
        if (type == null) {
            return null;
        }
        IssueTypeMetaRegistry.IssueTypeMeta meta = issueTypeMetaRegistry.getMeta(type);
        String label = meta != null ? meta.displayName() : humanize(type.name());
        String description = meta != null ? meta.description() : "Pendência de Governança.";
        return buildLabel(type.name(), label, description, "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
    }

    public GovernanceLabelDto severityLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "LOW" -> {
                return buildLabel("LOW", "Baixo Impacto", "Impacto limitado para operação e suporte.", "green", "Impacto reduzido no atendimento.", "LOW");
            }
            case "MEDIUM" -> {
                return buildLabel("MEDIUM", "Impacto Médio", "Pode gerar retrabalho e atrasos em atendimento.", "yellow", "Risco moderado para o suporte.", "MEDIUM");
            }
            case "HIGH" -> {
                return buildLabel("HIGH", "Alto Impacto", "Impacto direto no suporte ou cliente final.", "orange", "Pode afetar o atendimento ao cliente.", "HIGH");
            }
            case "CRITICAL" -> {
                return buildLabel("CRITICAL", "Crítico (Ação Imediata)", "Impacto direto no suporte ou cliente final.", "red", "Risco crítico para o negócio.", "HIGH");
            }
        }
        try {
            return severityLabel(GovernanceSeverity.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Impacto em avaliação.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto severityLabel(GovernanceSeverity severity) {
        if (severity == null) {
            return null;
        }
        return switch (severity) {
            case INFO -> buildLabel(
                    severity.name(),
                    "Baixo Impacto",
                    "Impacto limitado para operação e suporte.",
                    "green",
                    "Impacto reduzido no atendimento.",
                    "LOW"
            );
            case WARN -> buildLabel(
                    severity.name(),
                    "Impacto Médio",
                    "Pode gerar retrabalho e atrasos em atendimento.",
                    "yellow",
                    "Risco moderado para o suporte.",
                    "MEDIUM"
            );
            case ERROR -> buildLabel(
                    severity.name(),
                    "Alto Impacto",
                    "Impacto direto no suporte ou cliente final.",
                    "orange",
                    "Pode afetar o atendimento ao cliente.",
                    "HIGH"
            );
        };
    }

    public GovernanceLabelDto issueStatusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return issueStatusLabel(GovernanceIssueStatus.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Status da pendência de governança.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto issueStatusLabel(GovernanceIssueStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> buildLabel(status.name(), "Em aberto", "Pendência registrada e aguardando tratamento.", "orange", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case ASSIGNED -> buildLabel(status.name(), "Atribuída", "Pendência atribuída ao responsável.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case IN_PROGRESS -> buildLabel(status.name(), "Em andamento", "Pendência em tratamento.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case RESOLVED -> buildLabel(status.name(), "Resolvida", "Pendência concluída.", "green", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case IGNORED -> buildLabel(status.name(), "Ignorada", "Pendência descartada.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto assignmentStatusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return assignmentStatusLabel(GovernanceAssignmentStatus.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Status da atribuição.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto assignmentStatusLabel(GovernanceAssignmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> buildLabel(status.name(), "Aberta", "Atribuição aberta.", "orange", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case IN_PROGRESS -> buildLabel(status.name(), "Em andamento", "Atribuição em andamento.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case DONE -> buildLabel(status.name(), "Concluída", "Atribuição finalizada.", "green", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto manualTaskStatusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return manualTaskStatusLabel(KbManualTaskStatus.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Status da atividade.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto manualTaskStatusLabel(KbManualTaskStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> buildLabel(status.name(), "Em aberto", "Atividade pendente.", "orange", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case IN_PROGRESS -> buildLabel(status.name(), "Em andamento", "Atividade em execução.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case BLOCKED -> buildLabel(status.name(), "Bloqueada", "Atividade aguardando liberação.", "red", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case DONE -> buildLabel(status.name(), "Concluída", "Atividade finalizada.", "green", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case IGNORED -> buildLabel(status.name(), "Ignorada", "Atividade descartada.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto governanceStatusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return governanceStatusLabel(GovernanceStatus.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Status do artigo.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto governanceStatusLabel(GovernanceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OK -> buildLabel(status.name(), "Sem Problemas", "Artigo sem pendências de governança.", "green", "Risco mínimo para operação.", "LOW");
            case WITH_ISSUES -> buildLabel(status.name(), "Com Problemas", "Artigo com pendências de governança.", "orange", "Requer atenção de governança.", "MEDIUM");
            case IGNORED -> buildLabel(status.name(), "Ignorado", "Artigo fora do escopo de governança.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto riskLevelLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return riskLevelLabel(KbManualRiskLevel.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Nível de impacto.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto riskLevelLabel(KbManualRiskLevel level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case LOW -> buildLabel(level.name(), "Baixo Impacto", "Baixo impacto operacional.", "green", "Impacto reduzido no atendimento.", "LOW");
            case MEDIUM -> buildLabel(level.name(), "Impacto Médio", "Impacto moderado na operação.", "yellow", "Pode gerar retrabalho no suporte.", "MEDIUM");
            case HIGH -> buildLabel(level.name(), "Alto Impacto", "Impacto elevado para suporte e cliente.", "orange", "Exige ação rápida.", "HIGH");
            case CRITICAL -> buildLabel(level.name(), "Crítico (Ação Imediata)", "Impacto direto no suporte ou cliente final.", "red", "Risco crítico para o negócio.", "HIGH");
        };
    }

    public GovernanceLabelDto priorityLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return priorityLabel(KbManualPriority.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Prioridade da atividade.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto priorityLabel(KbManualPriority priority) {
        if (priority == null) {
            return null;
        }
        String label = switch (priority) {
            case P1 -> "Prioridade 1";
            case P2 -> "Prioridade 2";
            case P3 -> "Prioridade 3";
            case P4 -> "Prioridade 4";
        };
        return buildLabel(priority.name(), label, "Nível de urgência da atividade.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
    }

    public GovernanceLabelDto manualAssigneeTypeLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return manualAssigneeTypeLabel(KbManualAssigneeType.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Responsável pela atividade.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto manualAssigneeTypeLabel(KbManualAssigneeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case AGENT -> buildLabel(type.name(), "Pessoa", "Responsável individual.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case TEAM -> buildLabel(type.name(), "Equipe", "Responsável em grupo.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto syncModeLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return syncModeLabel(SyncMode.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Modo de análise.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto syncModeLabel(SyncMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case FULL -> buildLabel(mode.name(), "Análise Completa", "Processa todo o histórico.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case DELTA_WINDOW -> buildLabel(mode.name(), "Análise Parcial (Últimas Alterações)", "Processa apenas mudanças recentes.", "teal", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto syncRunStatusLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return syncRunStatusLabel(SyncRunStatus.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Status da execução.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto syncRunStatusLabel(SyncRunStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case RUNNING -> buildLabel(status.name(), "Em execução", "Processo em andamento.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case SUCCESS -> buildLabel(status.name(), "Concluída", "Processo finalizado com sucesso.", "green", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case FAILED -> buildLabel(status.name(), "Falhou", "Processo finalizado com erros.", "red", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    public GovernanceLabelDto responsibleTypeLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return responsibleTypeLabel(GovernanceResponsibleType.valueOf(code));
        } catch (IllegalArgumentException ex) {
            return buildLabel(code, humanize(code), "Tipo de responsável.", "gray", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        }
    }

    public GovernanceLabelDto responsibleTypeLabel(GovernanceResponsibleType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case USER -> buildLabel(type.name(), "Pessoa", "Responsável individual.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
            case TEAM -> buildLabel(type.name(), "Equipe", "Responsável em grupo.", "blue", DEFAULT_IMPACT_SUMMARY, DEFAULT_BUSINESS_IMPACT_LEVEL);
        };
    }

    private GovernanceLabelDto buildLabel(
            String code,
            String label,
            String description,
            String color,
            String impactSummary,
            String businessImpactLevel
    ) {
        return new GovernanceLabelDto(code, label, description, color, impactSummary, businessImpactLevel);
    }

    private String humanize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (normalized.isBlank()) {
            return value;
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
