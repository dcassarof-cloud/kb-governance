package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceIssueDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.service.GovernanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Governance API
 * GET /api/v1/governance/issues?page=1&size=10
 * GET /api/v1/governance/duplicates
 */
@RestController
@RequestMapping("/api/v1/governance")
public class GovernanceController {

    private final GovernanceService service;

    public GovernanceController(GovernanceService service) {
        this.service = service;
    }

    @GetMapping("/issues")
    public ResponseEntity<PageResponseDto<GovernanceIssueDto>> issues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.listIssues(page, size));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<DuplicateGroupDto>> duplicates() {
        return ResponseEntity.ok(service.listDuplicates());
    }
}
