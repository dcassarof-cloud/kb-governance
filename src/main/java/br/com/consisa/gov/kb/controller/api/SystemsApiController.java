package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.SystemResponse;
import br.com.consisa.gov.kb.domain.KbSystem;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🗂️ Systems API Controller
 * 
 * Endpoints:
 * - GET /api/v1/systems (lista todos)
 */
@RestController
@RequestMapping("/api/v1/systems")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
public class SystemsApiController {

    private static final Logger log = LoggerFactory.getLogger(SystemsApiController.class);

    private final KbSystemRepository systemRepo;

    public SystemsApiController(KbSystemRepository systemRepo) {
        this.systemRepo = systemRepo;
    }

    /**
     * GET /api/v1/systems
     * 
     * Retorna lista de todos os sistemas cadastrados.
     */
    @GetMapping
    public ResponseEntity<List<SystemResponse>> getSystems() {
        log.info("GET /api/v1/systems");

        try {
            List<KbSystem> systems = systemRepo.findAll();

            List<SystemResponse> response = systems.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            log.info("✅ Retornando {} sistemas", response.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar sistemas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ======================
    // MAPPING
    // ======================

    private SystemResponse mapToDto(KbSystem system) {
        return new SystemResponse(
                system.getId(),
                system.getCode(),
                system.getName(),
                system.getDescription(),
                system.getIsActive()
        );
    }
}
