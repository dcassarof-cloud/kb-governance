package br.com.consisa.gov.kb.security;

import br.com.consisa.gov.kb.repository.AppUserRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.stereotype.Component;

@Component
public class IssueAccessService {

    private final KbGovernanceIssueRepository issueRepository;
    private final AppUserRepository userRepository;

    public IssueAccessService(KbGovernanceIssueRepository issueRepository,
                              AppUserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    public boolean isAssignedToCurrentUser(Long issueId) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            return false;
        }
        String agentId = userRepository.findById(userId)
                .map(user -> user.getAgentId())
                .orElse(null);
        if (agentId == null) {
            return false;
        }
        return issueRepository.findById(issueId)
                .map(issue -> agentId.equals(issue.getResponsibleId()))
                .orElse(false);
    }
}
