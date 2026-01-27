package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupResponse;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔄 Duplicates API Controller
 *
 * Endpoint alternativo para duplicados:
 * - GET /api/v1/duplicates (mesmo que /api/v1/governance/duplicates)
 */
@RestController
@RequestMapping("/api/v1/duplicates")
@CrossOrigin(origins = "*")
public class DuplicatesApiController {

    private static final Logger log = LoggerFactory.getLogger(DuplicatesApiController.class);

    private final KbArticleRepository articleRepo;

    public DuplicatesApiController(KbArticleRepository articleRepo) {
        this.articleRepo = articleRepo;
    }

    /**
     * GET /api/v1/duplicates
     * Lista grupos de artigos duplicados
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DuplicateGroupResponse>> getDuplicates() {
        log.info("GET /api/v1/duplicates");

        try {
            // 1. Busca hashes duplicados
            List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();

            // 2. Para cada hash, busca os artigos
            List<DuplicateGroupResponse> response = new ArrayList<>();

            for (String hash : duplicateHashes) {
                List<Long> articleIds = articleRepo.findArticleIdsByContentHash(hash);

                if (articleIds.size() > 1) {
                    response.add(new DuplicateGroupResponse(
                            hash,
                            articleIds.size(),
                            articleIds
                    ));
                }
            }

            log.info("✅ Retornando {} grupos de duplicados", response.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar duplicados: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}