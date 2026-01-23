package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.ArticleListItemDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ArticlesService {

    public PageResponseDto<ArticleListItemDto> list(int page1Based, int size) {
        // TODO (fase 2): trocar por repository com PageRequest (0-based)
        // int page0 = Math.max(page1Based - 1, 0);

        return new PageResponseDto<>(
                page1Based,
                size,
                0,
                0,
                List.of(
                        // Exemplo: pode remover depois
                        // new ArticleListItemDto(1L, "Exemplo", "exemplo", "https://...", "CSN", "ConsisaNet", "OK", Instant.now())
                )
        );
    }
}
