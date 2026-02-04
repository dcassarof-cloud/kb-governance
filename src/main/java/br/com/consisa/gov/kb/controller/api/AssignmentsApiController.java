package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.domain.AssignmentPriority;
import br.com.consisa.gov.kb.domain.AssignmentReason;
import br.com.consisa.gov.kb.domain.KbArticleAssignment;
import br.com.consisa.gov.kb.service.KbAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * 📋 Assignments API Controller
 *
 * Endpoints:
 * - POST /api/v1/assignments/manual
 * - POST /api/v1/assignments/{id}/create-ticket
 */
@RestController
@RequestMapping("/api/v1/assignments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AssignmentsApiController {

    private static final Logger log = LoggerFactory.getLogger(AssignmentsApiController.class);

    private final KbAssignmentService assignmentService;

    public AssignmentsApiController(KbAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * POST /api/v1/assignments/manual
     */
    @PostMapping("/manual")
    public ResponseEntity<KbArticleAssignment> createManualAssignment(
            @RequestBody ManualAssignmentRequest request
    ) {
        log.info("POST /api/v1/assignments/manual: article={} agent={} createTicket={}",
                request.articleId(), request.agentId(), request.createTicket());

        KbArticleAssignment assignment = assignmentService.createManualAssignment(
                request.articleId(),
                request.agentId(),
                request.reason(),
                request.priority(),
                request.dueDate(),
                request.description(),
                request.createTicket() != null ? request.createTicket() : true
        );

        return ResponseEntity.ok(assignment);
    }

    /**
     * POST /api/v1/assignments/{id}/create-ticket
     */
    @PostMapping("/{id}/create-ticket")
    public ResponseEntity<KbArticleAssignment> createTicket(@PathVariable Long id) {
        log.info("POST /api/v1/assignments/{}/create-ticket", id);
        KbArticleAssignment assignment = assignmentService.createTicketForAssignment(id);
        return ResponseEntity.ok(assignment);
    }

    public record ManualAssignmentRequest(
            Long articleId,
            String agentId,
            AssignmentReason reason,
            AssignmentPriority priority,
            OffsetDateTime dueDate,
            String description,
            Boolean createTicket
    ) {}
}
