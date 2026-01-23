package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.ArticleListItemDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.service.ArticlesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Articles API
 * GET /api/v1/articles?page=1&size=10
 */
@RestController
@RequestMapping("/api/v1/articles")
public class ArticlesController {

    private final ArticlesService service;

    public ArticlesController(ArticlesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<ArticleListItemDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.list(page, size));
    }
}
