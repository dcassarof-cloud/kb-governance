package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.NeedActionRequest;
import br.com.consisa.gov.kb.controller.api.dto.NeedResponse;
import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.FaqCluster;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import br.com.consisa.gov.kb.service.NeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/needs")
@CrossOrigin(origins = "*")
public class NeedsApiController {

    private final DetectedNeedRepository needRepository;
    private final FaqClusterRepository clusterRepository;
    private final RecurrenceRuleRepository ruleRepository;
    private final NeedService needService;

    public NeedsApiController(
            DetectedNeedRepository needRepository,
            FaqClusterRepository clusterRepository,
            RecurrenceRuleRepository ruleRepository,
            NeedService needService
    ) {
        this.needRepository = needRepository;
        this.clusterRepository = clusterRepository;
        this.ruleRepository = ruleRepository;
        this.needService = needService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<NeedResponse>> listNeeds() {
        List<NeedResponse> items = needRepository.findAll().stream()
                .map(this::mapNeed)
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<NeedResponse> getNeed(@PathVariable Long id) {
        DetectedNeed need = needService.getNeed(id);
        return ResponseEntity.ok(mapNeed(need));
    }

    @PostMapping("/{id}/create-task")
    public ResponseEntity<NeedResponse> createTask(@PathVariable Long id) {
        DetectedNeed need = needService.createTask(id);
        return ResponseEntity.ok(mapNeed(need));
    }

    @PostMapping("/{id}/create-master-ticket")
    public ResponseEntity<NeedResponse> createMasterTicket(
            @PathVariable Long id,
            @RequestBody(required = false) NeedActionRequest request
    ) {
        String actor = request != null ? request.actor() : null;
        DetectedNeed need = needService.createMasterTicket(id, actor);
        return ResponseEntity.ok(mapNeed(need));
    }

    private NeedResponse mapNeed(DetectedNeed need) {
        FaqCluster cluster = clusterRepository.findById(need.getClusterId()).orElse(null);
        RecurrenceRule rule = ruleRepository.findById(need.getRuleId()).orElse(null);
        return new NeedResponse(
                need.getId(),
                need.getStatus(),
                need.getTaskStatus(),
                need.getLastDetectedAt(),
                need.getClusterId(),
                cluster != null ? cluster.getSampleText() : null,
                need.getRuleId(),
                rule != null ? rule.getName() : null,
                need.getExternalTicketId()
        );
    }
}
