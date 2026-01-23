package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.SyncRunDto;
import br.com.consisa.gov.kb.dto.SyncTriggerResponseDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {

    public List<SyncRunDto> listRuns() {
        return List.of();
    }

    public SyncTriggerResponseDto triggerSync() {
        // TODO (fase 2): iniciar job/worker e persistir kb_sync_run
        return new SyncTriggerResponseDto(true, Instant.now(), "Sync disparado com sucesso (stub).");
    }
}
