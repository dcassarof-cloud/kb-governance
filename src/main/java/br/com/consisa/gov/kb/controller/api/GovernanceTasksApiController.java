package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.*;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.KbManualTaskRepository;
import br.com.consisa.gov.kb.service.KbManualTaskService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.consisa.gov.kb.util.DateTimeUtils.toOffsetDateTimeOrNull;

@RestController
@RequestMapping("/api/v1/governance")
@CrossOrigin(origins = "*")
public class GovernanceTasksApiController {

    private final KbManualTaskService taskService;
    private final KbManualTaskRepository taskRepository;
    private final KbGovernanceIssueRepository issueRepository;
    private final KbArticleRepository articleRepository;

    public GovernanceTasksApiController(
            KbManualTaskService taskService,
            KbManualTaskRepository taskRepository,
            KbGovernanceIssueRepository issueRepository,
            KbArticleRepository articleRepository
    ) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.issueRepository = issueRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * GET /api/v1/governance/tasks
     */
    @GetMapping("/tasks")
    @Transactional(readOnly = true)
    public ResponseEntity<PaginatedResponse<GovernanceTaskResponse>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false, name = "q") String text
    ) {
        int pageIndex = Math.max(0, page - 1);
        int safeSize = Math.max(1, Math.min(size, 100));

        var pageable = PageRequest.of(pageIndex, safeSize);
        var pageResult = taskService.listTasks(
                pageable,
                systemId,
                safeFilter(status),
                safeFilter(risk),
                safeFilter(priority),
                safeFilter(assigneeId),
                safeFilter(issueType),
                safeFilter(text)
        );

        List<GovernanceTaskResponse> items = pageResult.getContent().stream()
                .map(this::mapRow)
                .collect(Collectors.toList());

        PaginatedResponse<GovernanceTaskResponse> response = new PaginatedResponse<>(
                page,
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                items
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/governance/tasks/{taskId}/assign
     */
    @PostMapping("/tasks/{taskId}/assign")
    public ResponseEntity<GovernanceTaskResponse> assignTask(
            @PathVariable Long taskId,
            @RequestBody ManualTaskAssignRequest request
    ) {
        taskService.assign(taskId, request);
        return ResponseEntity.ok(mapRow(taskService.getTaskRow(taskId)));
    }

    /**
     * POST /api/v1/governance/tasks/{taskId}/status
     */
    @PostMapping("/tasks/{taskId}/status")
    public ResponseEntity<GovernanceTaskResponse> updateStatus(
            @PathVariable Long taskId,
            @RequestBody ManualTaskStatusRequest request
    ) {
        taskService.updateStatus(taskId, request);
        return ResponseEntity.ok(mapRow(taskService.getTaskRow(taskId)));
    }

    /**
     * POST /api/v1/governance/tasks/{taskId}/merge
     */
    @PostMapping("/tasks/{taskId}/merge")
    public ResponseEntity<GovernanceTaskResponse> mergeManual(
            @PathVariable Long taskId,
            @RequestBody ManualTaskMergeRequest request
    ) {
        taskService.mergeManual(taskId, request);
        return ResponseEntity.ok(mapRow(taskService.getTaskRow(taskId)));
    }

    /**
     * POST /api/v1/governance/tasks/{taskId}/move
     */
    @PostMapping("/tasks/{taskId}/move")
    public ResponseEntity<GovernanceTaskResponse> moveManual(
            @PathVariable Long taskId,
            @RequestBody ManualTaskMoveRequest request
    ) {
        taskService.moveManual(taskId, request);
        return ResponseEntity.ok(mapRow(taskService.getTaskRow(taskId)));
    }

    /**
     * POST /api/v1/governance/tasks/{taskId}/review
     */
    @PostMapping("/tasks/{taskId}/review")
    public ResponseEntity<GovernanceTaskResponse> reviewManual(
            @PathVariable Long taskId,
            @RequestBody ManualTaskReviewRequest request
    ) {
        taskService.reviewManual(taskId, request);
        return ResponseEntity.ok(mapRow(taskService.getTaskRow(taskId)));
    }

    /**
     * GET /api/v1/governance/summary
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<GovernanceSummaryResponse> getSummary() {
        long totalArticles = articleRepository.count();
        long totalIssues = issueRepository.countTotalIssues();
        long articlesWithIssues = issueRepository.countDistinctArticlesWithIssues();
        long articlesOk = Math.max(0, totalArticles - articlesWithIssues);
        long openIssues = issueRepository.countOpenIssues();

        var riskRows = taskRepository.countByRiskLevel();
        List<GovernanceSummaryResponse.RiskSummary> byRisk = riskRows == null
                ? List.of()
                : riskRows.stream()
                    .map(row -> new GovernanceSummaryResponse.RiskSummary(
                            row.getRiskLevel(),
                            row.getTotal() != null ? row.getTotal() : 0L
                    ))
                    .collect(Collectors.toList());

        var slaRows = taskRepository.countOverdueByPriority();
        List<GovernanceSummaryResponse.SlaSummary> slaOverdue = slaRows == null
                ? List.of()
                : slaRows.stream()
                    .map(row -> new GovernanceSummaryResponse.SlaSummary(
                            row.getPriority(),
                            row.getTotal() != null ? row.getTotal() : 0L
                    ))
                    .collect(Collectors.toList());

        var criticalRows = taskRepository.findTopCriticalSystems(5);
        List<GovernanceSummaryResponse.CriticalSystemSummary> criticalSystems = criticalRows == null
                ? List.of()
                : criticalRows.stream()
                    .map(row -> new GovernanceSummaryResponse.CriticalSystemSummary(
                            row.getSystemCode(),
                            row.getSystemName(),
                            row.getTotal() != null ? row.getTotal() : 0L
                    ))
                    .collect(Collectors.toList());

        GovernanceSummaryResponse response = new GovernanceSummaryResponse(
                totalArticles,
                totalIssues,
                articlesWithIssues,
                articlesOk,
                openIssues,
                byRisk,
                slaOverdue,
                0.0,
                criticalSystems
        );

        return ResponseEntity.ok(response);
    }

    private GovernanceTaskResponse mapRow(KbManualTaskRepository.ManualTaskRow row) {
        if (row == null) {
            return null;
        }

        List<String> issues = row.getIssueTypes() == null || row.getIssueTypes().isBlank()
                ? List.of()
                : Arrays.stream(row.getIssueTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

        return new GovernanceTaskResponse(
                row.getTaskId(),
                row.getArticleId(),
                row.getArticleTitle(),
                row.getArticleSlug(),
                row.getArticleUrl(),
                row.getSystemCode(),
                row.getSystemName(),
                row.getStatus(),
                row.getRiskLevel(),
                row.getPriority(),
                row.getAssigneeType(),
                row.getAssigneeId(),
                toOffsetDateTimeOrNull(row.getDueAt()),
                toOffsetDateTimeOrNull(row.getLastActionAt()),
                issues
        );
    }

    private String safeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
