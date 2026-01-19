package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.service.KbArticleSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kb/articles")
public class KbArticleController {

    private final KbArticleSyncService service;

    public KbArticleController(KbArticleSyncService service) {
        this.service = service;
    }

    /**
     * Sincroniza um único artigo pelo ID do Movidesk.
     * POST /kb/articles/{id}/sync
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<KbArticle> sync(@PathVariable("id") long id) {
        return ResponseEntity.ok(service.sync(id));
    }

    /**
     * Atribui manualmente um sistema a um artigo.
     * POST /kb/articles/{id}/assign-system/{code}
     */
    @PostMapping("/{id}/assign-system/{code}")
    public ResponseEntity<Void> assignSystem(
            @PathVariable long id,
            @PathVariable String code
    ) {
        service.assignSystem(id, code);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista artigos sem classificação (system_id null).
     * GET /kb/articles/unclassified
     */
    @GetMapping("/unclassified")
    public ResponseEntity<List<KbArticle>> unclassified() {
        return ResponseEntity.ok(service.listUnclassified());
    }
}
