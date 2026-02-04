package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.DuplicateGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DuplicatesApiController.class)
class DuplicatesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KbArticleRepository articleRepository;

    @MockBean
    private DuplicateGroupService duplicateGroupService;

    @Test
    void setPrimaryAcceptsPrimaryArticleId() throws Exception {
        mockMvc.perform(post("/api/v1/duplicates/groups/hash-1/primary")
                        .contentType("application/json")
                        .content("""
                                {
                                  "primaryArticleId": 42,
                                  "actor": "tester"
                                }
                                """))
                .andExpect(status().isOk());

        verify(duplicateGroupService).setPrimary("hash-1", 42L, "tester");
    }

    @Test
    void setPrimaryAcceptsLegacyArticleId() throws Exception {
        mockMvc.perform(post("/api/v1/duplicates/groups/hash-2/primary")
                        .contentType("application/json")
                        .content("""
                                {
                                  "articleId": 21,
                                  "actor": "tester"
                                }
                                """))
                .andExpect(status().isOk());

        verify(duplicateGroupService).setPrimary("hash-2", 21L, "tester");
    }

    @Test
    void setPrimaryRequiresActor() throws Exception {
        mockMvc.perform(post("/api/v1/duplicates/groups/hash-3/primary")
                        .contentType("application/json")
                        .content("""
                                {
                                  "primaryArticleId": 12
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(duplicateGroupService);
    }
}
