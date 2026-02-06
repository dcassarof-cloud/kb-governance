package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.config.GlobalExceptionHandler;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import br.com.consisa.gov.kb.service.MovideskTicketService;
import br.com.consisa.gov.kb.service.NeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NeedsApiController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "movidesk.token=test-token")
class NeedsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DetectedNeedRepository detectedNeedRepository;

    @MockitoBean
    private FaqClusterRepository faqClusterRepository;

    @MockitoBean
    private RecurrenceRuleRepository recurrenceRuleRepository;

    @MockitoBean
    private NeedService needService;

    @MockitoBean
    private MovideskTicketService movideskTicketService;

    @Test
    void listRecurringNeedsDefaultsToLast30Days() throws Exception {
        when(needService.fetchRecurringTickets(any(), any())).thenReturn(List.of());
        when(movideskTicketService.buildTicketUrl(anyString())).thenReturn("http://ticket");

        mockMvc.perform(get("/api/v1/needs/recurring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listRecurringNeedsRejectsInvalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/needs/recurring")
                        .param("start", "2024-99-01")
                        .param("end", "2024-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Formato de data inválido para start. Use YYYY-MM-DD."));
    }

    @Test
    void listRecurringNeedsReturnsBadGatewayOnMovideskFailure() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(needService)
                .fetchRecurringTickets(any(), any());

        mockMvc.perform(get("/api/v1/needs/recurring"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Falha ao consultar Movidesk"));
    }
}
