package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import br.com.consisa.gov.kb.service.GovernanceAssigneeService;
import br.com.consisa.gov.kb.service.GovernanceIssueWorkflowService;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import br.com.consisa.gov.kb.service.GovernanceOverviewService;
import br.com.consisa.gov.kb.service.GovernanceService;
import br.com.consisa.gov.kb.service.IssueTypeMetaRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GovernanceApiController.class)
class GovernanceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KbGovernanceIssueRepository issueRepository;

    @MockitoBean
    private KbArticleRepository articleRepository;

    @MockitoBean
    private GovernanceService governanceService;

    @MockitoBean
    private GovernanceIssueWorkflowService workflowService;

    @MockitoBean
    private GovernanceAssigneeService assigneeService;

    @MockitoBean
    private GovernanceOverviewService overviewService;

    @MockitoBean
    private IssueTypeMetaRegistry issueTypeMetaRegistry;

    @MockitoBean
    private GovernanceLanguageService languageService;

    @MockitoBean
    private KbSystemRepository systemRepository;

    @Test
    void assignIssueAcceptsLocalDatePayload() throws Exception {
        KbGovernanceIssue issue = new KbGovernanceIssue();
        issue.setArticleId(10L);
        issue.setIssueType(KbGovernanceIssueType.DUPLICATE_CONTENT);
        issue.setStatus(GovernanceIssueStatus.OPEN);
        issue.setSeverity(GovernanceSeverity.WARN);
        issue.setResolvedAt(null);
        issue.setResolvedBy(null);
        issue.setIgnoredReason(null);
        issue.setSlaDueAt(null);
        issue.setResponsibleId(null);
        issue.setResponsibleType(null);
        issue.setMessage("Teste");
        issue.prePersist();

        when(issueRepository.findIssueRowById(1L)).thenReturn(Optional.empty());
        when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        when(articleRepository.findById(10L)).thenReturn(Optional.empty());
        doReturn(null).when(issueTypeMetaRegistry).getMeta(any());

        mockMvc.perform(post("/api/v1/governance/issues/1/assign")
                        .contentType("application/json")
                        .content("""
                                {
                                  "agentId": "123",
                                  "agentName": "Agente",
                                  "dueDate": "2026-02-15",
                                  "actor": "tester"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(workflowService).assignIssue(
                any(),
                any(),
                any(),
                captor.capture(),
                any()
        );

        assertThat(captor.getValue()).isEqualTo(LocalDate.of(2026, 2, 15));
    }
}
