package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.config.GlobalExceptionHandler;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GovernanceApiController.class)
@Import(GlobalExceptionHandler.class)
class GovernanceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KbGovernanceIssueRepository issueRepository;

    @MockBean
    private KbArticleRepository articleRepository;

    @MockBean
    private GovernanceService governanceService;

    @MockBean
    private GovernanceIssueWorkflowService workflowService;

    @MockBean
    private GovernanceAssigneeService assigneeService;

    @MockBean
    private GovernanceOverviewService overviewService;

    @MockBean
    private IssueTypeMetaRegistry issueTypeMetaRegistry;

    @MockBean
    private GovernanceLanguageService languageService;

    @MockBean
    private KbSystemRepository systemRepository;

    @Test
    void getIssuesRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/governance/issues")
                        .param("status", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status inválido: invalid"));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void getIssuesRejectsInvalidSeverity() throws Exception {
        mockMvc.perform(get("/api/v1/governance/issues")
                        .param("severity", "wrong"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Severidade inválida: wrong"));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void getIssuesRejectsInvalidIssueType() throws Exception {
        mockMvc.perform(get("/api/v1/governance/issues")
                        .param("issueType", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tipo de issue inválido: unknown"));

        verifyNoInteractions(issueRepository);
    }

    @Test
    void getIssuesRejectsInvalidSystemCode() throws Exception {
        when(systemRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/governance/issues")
                        .param("systemCode", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("systemCode inválido"));

        verifyNoInteractions(issueRepository);
    }
}
