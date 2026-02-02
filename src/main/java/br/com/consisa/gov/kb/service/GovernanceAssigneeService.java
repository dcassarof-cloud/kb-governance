package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.ResponsibleSummaryDto;
import br.com.consisa.gov.kb.controller.api.dto.SuggestedAssigneeDto;
import br.com.consisa.gov.kb.controller.api.dto.SuggestedAssigneeResponse;
import br.com.consisa.gov.kb.domain.KbAgent;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueAssignmentRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GovernanceAssigneeService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceAssigneeService.class);

    private final KbGovernanceIssueRepository issueRepository;
    private final KbGovernanceIssueAssignmentRepository assignmentRepository;
    private final KbAgentService agentService;

    public GovernanceAssigneeService(
            KbGovernanceIssueRepository issueRepository,
            KbGovernanceIssueAssignmentRepository assignmentRepository,
            KbAgentService agentService
    ) {
        this.issueRepository = issueRepository;
        this.assignmentRepository = assignmentRepository;
        this.agentService = agentService;
    }

    @Transactional(readOnly = true)
    public SuggestedAssigneeResponse suggestAssignee(Long issueId) {
        if (issueId == null) {
            throw new IllegalArgumentException("IssueId é obrigatório");
        }

        issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        List<KbAgent> activeAgents = agentService.findAllActive();
        if (activeAgents.isEmpty()) {
            log.warn("Nenhum agente ativo disponível para sugestão (issueId={})", issueId);
            return new SuggestedAssigneeResponse(null, List.of());
        }

        Map<String, KbGovernanceIssueAssignmentRepository.PendingByAgentRow> pendingByAgent = assignmentRepository
                .countPendingIssuesByAgent()
                .stream()
                .filter(row -> row.getAgentId() != null)
                .collect(Collectors.toMap(
                        KbGovernanceIssueAssignmentRepository.PendingByAgentRow::getAgentId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<SuggestedAssigneeDto> ranked = activeAgents.stream()
                .map(agent -> {
                    var pendingRow = pendingByAgent.get(agent.getId());
                    long pendingIssues = pendingRow != null && pendingRow.getPendingIssues() != null
                            ? pendingRow.getPendingIssues()
                            : 0L;
                    double score = calculateScore(pendingIssues);
                    return new SuggestedAssigneeDto(
                            agent.getId(),
                            resolveAgentName(agent),
                            pendingIssues,
                            score
                    );
                })
                .sorted(Comparator
                        .comparingDouble(SuggestedAssigneeDto::score).reversed()
                        .thenComparingLong(SuggestedAssigneeDto::pendingIssues)
                        .thenComparing(SuggestedAssigneeDto::agentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        SuggestedAssigneeDto suggested = ranked.isEmpty() ? null : ranked.get(0);
        List<SuggestedAssigneeDto> others = ranked.size() > 1
                ? ranked.subList(1, ranked.size())
                : List.of();

        return new SuggestedAssigneeResponse(suggested, others);
    }

    @Transactional(readOnly = true)
    public List<ResponsibleSummaryDto> listResponsiblesSummary() {
        List<KbAgent> activeAgents = agentService.findAllActive();
        Map<String, KbGovernanceIssueAssignmentRepository.ResponsibleSummaryRow> summaryByAgent = assignmentRepository
                .summarizeByResponsible()
                .stream()
                .filter(row -> row.getAgentId() != null)
                .collect(Collectors.toMap(
                        KbGovernanceIssueAssignmentRepository.ResponsibleSummaryRow::getAgentId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return activeAgents.stream()
                .map(agent -> {
                    var row = summaryByAgent.get(agent.getId());
                    long openIssues = row != null && row.getOpenIssues() != null ? row.getOpenIssues() : 0L;
                    Long overdueIssues = row != null ? row.getOverdueIssues() : null;
                    return new ResponsibleSummaryDto(
                            agent.getId(),
                            resolveAgentName(agent),
                            openIssues,
                            overdueIssues,
                            row != null ? row.getLastAssignedAt() : null,
                            row != null ? row.getAvgResolutionDays() : null
                    );
                })
                .sorted(Comparator
                        .comparingLong(ResponsibleSummaryDto::openIssues).reversed()
                        .thenComparing(ResponsibleSummaryDto::agentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private double calculateScore(long pendingIssues) {
        double score = 100.0 - pendingIssues;
        return Math.max(0.0, score);
    }

    private String resolveAgentName(KbAgent agent) {
        if (agent == null) {
            return null;
        }
        return Objects.requireNonNullElse(agent.getBusinessName(), agent.getUserName());
    }
}
