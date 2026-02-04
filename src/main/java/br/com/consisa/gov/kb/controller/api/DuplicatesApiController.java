package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupActionRequest;
import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupDetailResponse;
import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupPrimaryRequest;
import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupResponse;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.DuplicateGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DuplicatesApiController {

    private static final Logger log = LoggerFactory.getLogger(DuplicatesApiController.class);

    private final KbArticleRepository articleRepo;
    private final DuplicateGroupService duplicateGroupService;

    public DuplicatesApiController(KbArticleRepository articleRepo,
                                   DuplicateGroupService duplicateGroupService) {
        this.articleRepo = articleRepo;
        this.duplicateGroupService = duplicateGroupService;
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

    /**
     * GET /api/v1/duplicates/groups
     * Lista grupos de duplicados com artigos.
     */
    @GetMapping("/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DuplicateGroupDetailResponse>> listGroups() {
        log.info("GET /api/v1/duplicates/groups");
        return ResponseEntity.ok(duplicateGroupService.listGroups());
    }

    /**
     * POST /api/v1/duplicates/groups/{id}/primary
     */
    @PostMapping("/groups/{id}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable("id") String hash,
            @RequestBody DuplicateGroupPrimaryRequest request
    ) {
        if (request == null || request.primaryArticleId() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "primaryArticleId é obrigatório.");
        }
        if (request.actor() == null || request.actor().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "actor é obrigatório.");
        }
        duplicateGroupService.setPrimary(hash, request.primaryArticleId(), request.actor());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/duplicates/groups/{id}/ignore
     */
    @PostMapping("/groups/{id}/ignore")
    public ResponseEntity<Void> ignoreGroup(
            @PathVariable("id") String hash,
            @RequestBody DuplicateGroupActionRequest request
    ) {
        duplicateGroupService.ignoreGroup(hash, request.actor());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/duplicates/groups/{id}/merge-request
     */
    @PostMapping("/groups/{id}/merge-request")
    public ResponseEntity<Void> mergeRequest(
            @PathVariable("id") String hash,
            @RequestBody DuplicateGroupActionRequest request
    ) {
        duplicateGroupService.requestMerge(hash, request.actor());
        return ResponseEntity.ok().build();
    }
}
