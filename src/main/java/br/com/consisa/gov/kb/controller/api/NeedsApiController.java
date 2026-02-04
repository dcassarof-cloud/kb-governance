package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.NeedActionRequest;
import br.com.consisa.gov.kb.controller.api.dto.NeedResponse;
import br.com.consisa.gov.kb.controller.api.dto.RecurringNeedItemResponse;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.service.MovideskTicketService;
import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.FaqCluster;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import br.com.consisa.gov.kb.service.NeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/needs")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
public class NeedsApiController {

    private static final Logger log = LoggerFactory.getLogger(NeedsApiController.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^&\\s]+)");

    private final DetectedNeedRepository needRepository;
    private final FaqClusterRepository clusterRepository;
    private final RecurrenceRuleRepository ruleRepository;
    private final NeedService needService;
    private final MovideskTicketService movideskTicketService;

    @Value("${movidesk.token:}")
    private String movideskToken;

    public NeedsApiController(
            DetectedNeedRepository needRepository,
            FaqClusterRepository clusterRepository,
            RecurrenceRuleRepository ruleRepository,
            NeedService needService,
            MovideskTicketService movideskTicketService
    ) {
        this.needRepository = needRepository;
        this.clusterRepository = clusterRepository;
        this.ruleRepository = ruleRepository;
        this.needService = needService;
        this.movideskTicketService = movideskTicketService;
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

    @GetMapping("/recurring")
    public ResponseEntity<?> listRecurringNeeds(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        String requestId = resolveRequestId(request);
        log.info("GET /api/v1/needs/recurring requestId={} start={} end={} systemCode={}",
                requestId, start, end, systemCode);

        if (movideskToken == null || movideskToken.isBlank()) {
            log.error("❌ requestId={} Movidesk token não configurado.", requestId);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Movidesk token não configurado.");
        }

        LocalDate endDate = parseDateParam("end", end);
        if (endDate == null) {
            endDate = LocalDate.now(ZoneOffset.UTC);
        }

        LocalDate startDate = parseDateParam("start", start);
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Parâmetro end deve ser maior ou igual a start.");
        }

        OffsetDateTime startAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endAt = endDate.plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);

        String normalizedStatus = status != null ? status.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedSystem = systemCode != null ? systemCode.trim().toLowerCase(Locale.ROOT) : null;

        try {
            List<RecurringNeedItemResponse> items = needService.fetchRecurringTickets(startAt, endAt)
                    .stream()
                    .filter(ticket -> normalizedStatus == null
                            || (ticket.getStatus() != null
                            && ticket.getStatus().toLowerCase(Locale.ROOT).contains(normalizedStatus)))
                    .filter(ticket -> normalizedSystem == null || matchesSystemCode(ticket.getSubject(), normalizedSystem))
                    .map(ticket -> new RecurringNeedItemResponse(
                            ticket.getProtocol() != null ? ticket.getProtocol() : ticket.getId(),
                            ticket.getSubject(),
                            ticket.getStatus(),
                            ticket.getCreatedDate() != null
                                    ? ticket.getCreatedDate().atOffset(ZoneOffset.UTC)
                                    : null,
                            null,
                            systemCode,
                            null,
                            ticket.getId() != null ? movideskTicketService.buildTicketUrl(ticket.getId()) : null
                    ))
                    .toList();

            int safeSize = Math.max(1, Math.min(size, 100));
            int safePage = Math.max(1, page);
            int fromIndex = Math.min(items.size(), (safePage - 1) * safeSize);
            int toIndex = Math.min(items.size(), fromIndex + safeSize);
            List<RecurringNeedItemResponse> pageItems = items.subList(fromIndex, toIndex);

            PaginatedResponse<RecurringNeedItemResponse> response = new PaginatedResponse<>(
                    pageItems,
                    safePage,
                    safeSize,
                    items.size(),
                    (int) Math.ceil((double) items.size() / safeSize)
            );

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            String details = safeMovideskDetails(ex);
            log.warn("⚠️ requestId={} Falha ao consultar Movidesk recorrência: {}", requestId, details, ex);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar Movidesk", ex);
        }
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

    private boolean matchesSystemCode(String subject, String systemCode) {
        if (subject == null || systemCode == null) {
            return false;
        }
        return subject.toLowerCase(Locale.ROOT).contains(systemCode);
    }

    private String safeMovideskDetails(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            return ex.getClass().getSimpleName();
        }
        return TOKEN_PATTERN.matcher(message).replaceAll("token=***");
    }

    private LocalDate parseDateParam(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Formato de data inválido para " + name + ". Use YYYY-MM-DD.");
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String requestId = request.getHeader("x-request-id");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("x-correlation-id");
        }
        return requestId != null ? requestId : "unknown";
    }
}
