package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupArticleResponse;
import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupDetailResponse;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DuplicateGroupService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateGroupService.class);

    private final KbArticleRepository articleRepository;
    private final KbGovernanceIssueRepository issueRepository;
    private final GovernanceIssueWorkflowService workflowService;
    private final GovernanceLanguageService languageService;

    public DuplicateGroupService(
            KbArticleRepository articleRepository,
            KbGovernanceIssueRepository issueRepository,
            GovernanceIssueWorkflowService workflowService,
            GovernanceLanguageService languageService
    ) {
        this.articleRepository = articleRepository;
        this.issueRepository = issueRepository;
        this.workflowService = workflowService;
        this.languageService = languageService;
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroupDetailResponse> listGroups() {
        List<String> hashes = articleRepository.findDuplicateContentHashes();
        List<DuplicateGroupDetailResponse> groups = new ArrayList<>();

        for (String hash : hashes) {
            if (hash == null || hash.isBlank()) {
                continue;
            }
            List<DuplicateGroupArticleResponse> articles = articleRepository.findDuplicateArticlesByHash(hash).stream()
                    .map(row -> new DuplicateGroupArticleResponse(
                            row.getId(),
                            row.getTitle(),
                            row.getSystemCode(),
                            DateTimeUtils.toOffsetDateTime(row.getUpdatedAt()),
                            row.getSourceUrl()
                    ))
                    .toList();

            if (articles.size() < 2) {
                continue;
            }

            String status = deriveGroupStatus(issueRepository.findDuplicateIssueStatusesByHash(hash));

            groups.add(new DuplicateGroupDetailResponse(
                    hash,
                    hash,
                    languageService.issueStatusLabel(status),
                    articles
            ));
        }

        return groups;
    }

    @Transactional
    public void setPrimary(String hash, Long primaryArticleId, String actor) {
        if (primaryArticleId != null) {
            List<Long> articleIds = articleRepository.findArticleIdsByContentHash(hash);
            if (!articleIds.contains(primaryArticleId)) {
                throw new IllegalArgumentException("Artigo primário não pertence ao grupo: " + primaryArticleId);
            }
        }

        List<Long> issueIds = issueRepository.findDuplicateIssueIdsByHash(hash);
        if (issueIds.isEmpty()) {
            return;
        }

        workflowService.bulkUpdateStatus(
                issueIds,
                GovernanceIssueStatus.RESOLVED,
                actor,
                "PRIMARY_SET",
                primaryArticleId != null ? "primary=" + primaryArticleId : "primary=unknown"
        );

        log.info("✅ Grupo {} marcado com primary={} (issues resolvidas={})", hash, primaryArticleId, issueIds.size());
    }

    @Transactional
    public void ignoreGroup(String hash, String actor) {
        List<Long> issueIds = issueRepository.findDuplicateIssueIdsByHash(hash);
        if (issueIds.isEmpty()) {
            return;
        }

        workflowService.bulkUpdateStatus(
                issueIds,
                GovernanceIssueStatus.IGNORED,
                actor,
                "GROUP_IGNORED",
                "ignored"
        );

        log.info("🙈 Grupo {} ignorado (issues={})", hash, issueIds.size());
    }

    @Transactional
    public void requestMerge(String hash, String actor) {
        List<Long> issueIds = issueRepository.findDuplicateIssueIdsByHash(hash);
        if (issueIds.isEmpty()) {
            return;
        }

        workflowService.bulkUpdateStatus(
                issueIds,
                GovernanceIssueStatus.IN_PROGRESS,
                actor,
                "MERGE_REQUESTED",
                "merge-request"
        );

        log.info("🔀 Merge solicitado para grupo {} (issues={})", hash, issueIds.size());
    }

    private String deriveGroupStatus(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return GovernanceIssueStatus.OPEN.name();
        }

        boolean allResolved = statuses.stream().allMatch(status -> GovernanceIssueStatus.RESOLVED.name().equals(status));
        if (allResolved) {
            return GovernanceIssueStatus.RESOLVED.name();
        }

        boolean allIgnored = statuses.stream().allMatch(status -> GovernanceIssueStatus.IGNORED.name().equals(status));
        if (allIgnored) {
            return GovernanceIssueStatus.IGNORED.name();
        }

        if (statuses.stream().anyMatch(status -> GovernanceIssueStatus.IN_PROGRESS.name().equals(status))) {
            return GovernanceIssueStatus.IN_PROGRESS.name();
        }

        if (statuses.stream().anyMatch(status -> GovernanceIssueStatus.ASSIGNED.name().equals(status))) {
            return GovernanceIssueStatus.ASSIGNED.name();
        }

        return GovernanceIssueStatus.OPEN.name();
    }
}
