package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.governance.KbGovernanceDetectorService;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de governança (rodar detectores).
 */
@RestController
@RequestMapping("/kb/governance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class KbGovernanceController {

    private final KbArticleRepository articleRepo;
    private final KbGovernanceDetectorService detector;

    public KbGovernanceController(KbArticleRepository articleRepo,
                                  KbGovernanceDetectorService detector) {
        this.articleRepo = articleRepo;
        this.detector = detector;
    }

    /**
     * Analisa os artigos mais recentes (por updatedDate/createdDate).
     * Ex: POST /kb/governance/analyze/recent?limit=50
     */
    @PostMapping("/analyze/recent")
    public ResponseEntity<?> analyzeRecent(@RequestParam(defaultValue = "50") int limit) {
        int safe = Math.min(Math.max(limit, 1), 500);

        var page = articleRepo.findRecent(
                PageRequest.of(0, safe, Sort.by(Sort.Direction.DESC, "updatedDate"))
        );

        // Analisa cada artigo individualmente (ex: conteúdo incompleto etc.)
        page.forEach(detector::analyzeArticle);

        return ResponseEntity.ok(Map.of(
                "analyzed", page.getNumberOfElements()
        ));
    }

    /**
     * Executa o detector de duplicados para TODOS os hashes duplicados.
     * Ex: POST /kb/governance/run
     */
    @PostMapping("/run")
    public ResponseEntity<?> runAll() {
        int total = detector.analyzeAllDuplicates();
        return ResponseEntity.ok(Map.of("issuesOpenedOrUpdated", total));
    }

    /**
     * Executa o detector de duplicados para UM hash específico.
     * Ex: POST /kb/governance/hash/abc123
     */
    @PostMapping("/hash/{hash}")
    public ResponseEntity<?> runOne(@PathVariable String hash) {
        int total = detector.analyzeHash(hash);
        return ResponseEntity.ok(Map.of(
                "issuesOpenedOrUpdated", total,
                "hash", hash
        ));
    }

    /**
     * Detecta duplicados por content_hash.
     * Ex: POST /kb/governance/analyze/duplicates
     *
     * OBS: esse endpoint agora chama o método que existe: analyzeAllDuplicates().
     */
    @PostMapping("/analyze/duplicates")
    public ResponseEntity<?> analyzeDuplicates() {
        int totalIssues = detector.analyzeAllDuplicates();
        return ResponseEntity.ok(Map.of(
                "issuesOpenedOrUpdated", totalIssues
        ));
    }
}
