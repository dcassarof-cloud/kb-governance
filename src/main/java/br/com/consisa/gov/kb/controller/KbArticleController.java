package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.service.KbArticleSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST responsável por expor endpoints HTTP
 * para sincronização e consulta de artigos da Knowledge Base.
 *
 * Papel na arquitetura:
 * - Recebe requisições HTTP (camada Web)
 * - Delega o processamento para o Service (regra de negócio)
 * - Retorna respostas HTTP (ResponseEntity)
 *
 * Boas práticas:
 * - Controller não contém lógica de negócio
 * - Só valida entradas simples e encaminha para o Service
 */
@RestController
@RequestMapping("/kb/articles")
public class KbArticleController {

    private final KbArticleSyncService service;

    public KbArticleController(KbArticleSyncService service) {
        this.service = service;
    }

    /**
     * Sincroniza um único artigo pelo ID do Movidesk.
     *
     * Exemplo:
     * POST /kb/articles/123/sync
     *
     * Retorna o artigo persistido/atualizado no banco local.
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<KbArticle> sync(@PathVariable("id") long id) {
        return ResponseEntity.ok(service.sync(id));
    }

    /**
     * Executa sincronização em massa.
     *
     * Exemplo:
     * POST /kb/articles/sync-all
     *
     * Retorna 200 OK sem body.
     * (Em produção, esse tipo de ação pode virar async + retornar um jobId)
     */
    @PostMapping("/sync-all")
    public ResponseEntity<Void> syncAll() {
        service.syncAll();
        return ResponseEntity.ok().build();
    }

    /**
     * Atribui manualmente um sistema (KbSystem) a um artigo.
     *
     * Exemplo:
     * POST /kb/articles/123/assign-system/QUINTOEIXO
     *
     * Uso típico:
     * - Quando o artigo foi sincronizado mas ainda não foi classificado
     * - Permite curadoria manual
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
     * Lista artigos "não classificados", ou seja,
     * artigos que ainda não possuem system_id (null).
     *
     * Exemplo:
     * GET /kb/articles/unclassified
     */
    @GetMapping("/unclassified")
    public ResponseEntity<List<KbArticle>> unclassified() {
        return ResponseEntity.ok(service.listUnclassified());
    }
}
