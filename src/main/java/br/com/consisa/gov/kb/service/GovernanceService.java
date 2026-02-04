package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceIssueDto;
import br.com.consisa.gov.kb.dto.GovernanceManualDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GovernanceService {

    private final KbGovernanceIssueRepository issueRepo;
    private final KbArticleRepository articleRepo;
    private final GovernanceLanguageService languageService;

    public GovernanceService(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo,
            GovernanceLanguageService languageService
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
        this.languageService = languageService;
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
                        languageService.issueTypeLabel(r.getIssueType()),
                        languageService.severityLabel(r.getSeverity()),
                        languageService.issueStatusLabel(r.getStatus()),
                        r.getArticleId(),
                        r.getArticleTitle(),
                        r.getSystemCode(),
                        r.getSystemName(),
                        r.getMessage(),
                        r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now()  // já é Instant
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
    public PageResponseDto<GovernanceManualDto> listManuals(int page1Based, int size, String system, String status, String q) {
        int page0 = Math.max(page1Based - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(page0, safeSize);

        Page<KbArticleRepository.GovernanceManualRow> page = articleRepo.pageGovernanceManuals(
                pageable,
                normalizeFilter(system),
                normalizeFilter(status),
                normalizeFilter(q)
        );

        List<GovernanceManualDto> items = page.getContent().stream()
                .map(row -> new GovernanceManualDto(
                        row.getId(),
                        row.getTitle(),
                        row.getSystemCode(),
                        row.getSystemName(),
                        languageService.governanceStatusLabel(row.getGovernanceStatus()),
                        row.getUpdatedAt() != null ? row.getUpdatedAt() : Instant.now(),
                        row.getIssuesCount() != null ? row.getIssuesCount() : 0L
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
        return articleRepo.findDuplicateContentHashes().stream()
                .filter(hash -> hash != null && !hash.isBlank())
                .map(hash -> {
                    var articles = articleRepo.findDuplicateArticlesByHash(hash).stream()
                            .map(row -> new DuplicateGroupDto.DuplicateArticleDto(
                                    row.getId(),
                                    row.getTitle(),
                                    row.getSystemCode(),
                                    row.getSourceUrl(),
                                    br.com.consisa.gov.kb.util.DateTimeUtils.toOffsetDateTime(row.getUpdatedAt())
                            ))
                            .toList();

                    return new DuplicateGroupDto(hash, articles.size(), articles);
                })
                .filter(group -> group.count() > 1)
                .toList();
    }

    private String normalizeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
