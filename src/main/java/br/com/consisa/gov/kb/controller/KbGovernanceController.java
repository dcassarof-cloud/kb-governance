package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.governance.KbGovernanceDetectorService;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kb/governance")
public class KbGovernanceController {

    private final KbArticleRepository articleRepo;
    private final KbGovernanceDetectorService detector;

    public KbGovernanceController(KbArticleRepository articleRepo,
                                  KbGovernanceDetectorService detector) {
        this.articleRepo = articleRepo;
        this.detector = detector;
    }

    @PostMapping("/analyze/recent")
    public ResponseEntity<?> analyzeRecent(@RequestParam(defaultValue = "200") int limit) {

        List<Long> ids = articleRepo.findAll().stream()
                .sorted((a, b) -> {
                    var da = a.getUpdatedDate();
                    var db = b.getUpdatedDate();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .limit(Math.max(1, limit))
                .map(a -> a.getId())
                .toList();

        int analyzed = 0;
        for (Long id : ids) {
            var art = articleRepo.findById(id).orElse(null);
            if (art == null) continue;
            detector.analyzeArticle(art);
            analyzed++;
        }

        return ResponseEntity.ok("{\"analyzed\":" + analyzed + "}");
    }
}
