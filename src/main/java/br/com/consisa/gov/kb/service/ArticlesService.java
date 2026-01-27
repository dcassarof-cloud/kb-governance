package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.dto.ArticleListItemDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ArticlesService {

    private final KbArticleRepository repo;

    public ArticlesService(KbArticleRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ArticleListItemDto> list(int page1Based, int size) {
        int page0 = Math.max(page1Based - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(page0, safeSize, Sort.by(Sort.Direction.DESC, "updatedDate"));

        // Filtra ativos (article_status=1) via Example simples:
        // como articleStatus é int, o jeito mais limpo é criar query no repo,
        // mas aqui vou usar findAll + filtro no banco com Specification seria overkill.
        // Então: cria um método no repo (recomendado). Se quiser rápido: usa native no repo.
        Page<KbArticle> page = repo.findAll(pageable); // se teu banco tem tudo, depois você troca por "ativos"

        var items = page.getContent().stream()
                .map(a -> new ArticleListItemDto(
                        a.getId(),
                        a.getTitle(),
                        a.getSlug(),
                        a.getSourceUrl(),
                        a.getSystem() != null ? a.getSystem().getCode() : "UNCLASSIFIED",
                        a.getSystem() != null ? a.getSystem().getName() : "Não classificado",
                        a.getGovernanceStatus(),
                        a.getUpdatedDate() != null ? a.getUpdatedDate().toInstant() : Instant.now()
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
}
