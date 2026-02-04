package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.SupportImportRequest;
import br.com.consisa.gov.kb.controller.api.dto.SupportImportResponse;
import br.com.consisa.gov.kb.service.SupportImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/support")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class SupportImportApiController {

    private final SupportImportService importService;

    public SupportImportApiController(SupportImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import/run")
    public ResponseEntity<SupportImportResponse> runImport(@RequestBody(required = false) SupportImportRequest request) {
        OffsetDateTime start = null;
        OffsetDateTime end = null;
        if (request != null) {
            if (request.startDate() != null) {
                start = request.startDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            if (request.endDate() != null) {
                end = request.endDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusSeconds(1);
            }
        }
        var result = importService.runImport(start, end);
        return ResponseEntity.ok(new SupportImportResponse(result.ticketsCreated(), result.ticketsUpdated(), result.messagesCreated()));
    }
}
