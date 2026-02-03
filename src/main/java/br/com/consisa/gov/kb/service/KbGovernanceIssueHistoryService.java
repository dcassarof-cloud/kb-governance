package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueHistory;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KbGovernanceIssueHistoryService {

    private final KbGovernanceIssueHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public KbGovernanceIssueHistoryService(
            KbGovernanceIssueHistoryRepository historyRepository,
            ObjectMapper objectMapper
    ) {
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordCreated(KbGovernanceIssue issue, String actor) {
        saveHistory(issue, "CREATED", null, snapshot(issue), actor);
    }

    @Transactional
    public void recordAssigned(KbGovernanceIssue before, KbGovernanceIssue after, String actor) {
        saveHistory(after, "ASSIGNED", snapshot(before), snapshot(after), actor);
    }

    @Transactional
    public void recordUnassigned(KbGovernanceIssue before, KbGovernanceIssue after, String actor) {
        saveHistory(after, "UNASSIGNED", snapshot(before), snapshot(after), actor);
    }

    @Transactional
    public void recordStatusChanged(KbGovernanceIssue before, KbGovernanceIssue after, String actor) {
        saveHistory(after, "STATUS_CHANGED", snapshot(before), snapshot(after), actor);
    }

    @Transactional
    public void recordReopened(KbGovernanceIssue before, KbGovernanceIssue after, String actor) {
        saveHistory(after, "REOPENED", snapshot(before), snapshot(after), actor);
    }

    private String snapshot(KbGovernanceIssue issue) {
        if (issue == null) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        if (issue.getStatus() != null) {
            node.put("status", issue.getStatus().name());
        } else {
            node.putNull("status");
        }
        if (issue.getSlaDueAt() != null) {
            node.put("slaDueAt", issue.getSlaDueAt().toString());
        } else {
            node.putNull("slaDueAt");
        }
        if (issue.getResponsibleId() != null) {
            node.put("responsibleId", issue.getResponsibleId());
        } else {
            node.putNull("responsibleId");
        }
        if (issue.getResponsibleType() != null) {
            node.put("responsibleType", issue.getResponsibleType().name());
        } else {
            node.putNull("responsibleType");
        }
        return node.toString();
    }

    private void saveHistory(KbGovernanceIssue issue, String action, String oldValue, String newValue, String actor) {
        if (issue == null) {
            return;
        }
        KbGovernanceIssueHistory history = new KbGovernanceIssueHistory();
        history.setIssueId(issue.getId());
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setActor(actor);
        historyRepository.save(history);
    }
}
