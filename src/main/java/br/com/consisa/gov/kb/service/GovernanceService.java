package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceIssueDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class GovernanceService {

    private final KbGovernanceIssueRepository issueRepo;
    private final KbArticleRepository articleRepo;

    public GovernanceService(KbGovernanceIssueRepository issueRepo, KbArticleRepository articleRepo) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<GovernanceIssueDto> listIssues(int page1Based, int size) {
        int page0 = Math.max(page1Based - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(page0, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<KbGovernanceIssueRepository.IssueRow> page = issueRepo.pageIssues(pageable);

        List<GovernanceIssueDto> items = page.getContent().stream()
                .map(r -> new GovernanceIssueDto(
                        r.getId(),
                        r.getIssueType(),
                        r.getSeverity(),
                        r.getStatus(),
                        r.getArticleId(),
                        r.getArticleTitle(),
                        r.getSystemCode(),
                        r.getSystemName(),
                        r.getMessage(),
                        r.getCreatedAt() != null ? r.getCreatedAt().toInstant() : Instant.now()
                ))
                .toList();

        return new PageResponseDto<>(
                page1Based,
                safeSize,
                page.getTotalElements(),
                page.getTotalPages(),
                items
        );
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroupDto> listDuplicates() {
        return articleRepo.findDuplicateGroups().stream()
                .map(row -> {
                    String hash = String.valueOf(row[0]);
                    int count = ((Number) row[1]).intValue();

                    // Postgres retorna array como java.sql.Array
                    List<Long> ids;
                    try {
                        java.sql.Array arr = (java.sql.Array) row[2];
                        Long[] raw = (Long[]) arr.getArray();
                        ids = Arrays.asList(raw);
                    } catch (Exception e) {
                        ids = List.of();
                    }

                    return new DuplicateGroupDto(hash, count, ids);
                })
                .toList();
    }
}
