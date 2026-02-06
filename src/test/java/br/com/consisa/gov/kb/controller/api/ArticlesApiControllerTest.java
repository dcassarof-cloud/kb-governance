package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticlesApiController.class)
class ArticlesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KbArticleRepository articleRepository;

    @MockitoBean
    private GovernanceLanguageService languageService;

    @Test
    void getArticlesReturnsPaginatedEnvelope() throws Exception {
        KbArticle article = new KbArticle();
        article.setTitle("Guia");
        article.setSlug("guia");
        article.setSourceUrl("https://example.com");
        article.setUpdatedDate(OffsetDateTime.now());

        when(articleRepository.findRecent(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1)
        );

        mockMvc.perform(get("/api/v1/articles")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
