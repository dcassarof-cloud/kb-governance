package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskArticleSearchItemDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class KbDeltaSyncService {

    private final MovideskClient movideskClient;
    private final KbArticleRepository kbArticleRepository;
    private final KbArticleSyncService kbArticleSyncService;

    public KbDeltaSyncService(
            MovideskClient movideskClient,
            KbArticleRepository kbArticleRepository,
            KbArticleSyncService kbArticleSyncService
    ) {
        this.movideskClient = movideskClient;
        this.kbArticleRepository = kbArticleRepository;
        this.kbArticleSyncService = kbArticleSyncService;
    }

    /**
     * DELTA cirúrgico:
     * - varre poucas páginas (descoberta leve)
     * - só sincroniza (GET /article/{id}) quando detectar mudança
     */
    @Transactional
    public void deltaCirurgico(int pagesToScan, int pageSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (int page = 0; page < pagesToScan; page++) {
            var resp = movideskClient.searchArticles(page, pageSize);
            var items = resp.getItems();

            if (items == null || items.isEmpty()) break;

            for (MovideskArticleSearchItemDto item : items) {
                Long id = item.getId();
                if (id == null) continue;

                var existingOpt = kbArticleRepository.findById(id);

                // NEW
                if (existingOpt.isEmpty()) {
                    kbArticleSyncService.sync(id);

                    // após sync, marca visto/estado
                    kbArticleRepository.findById(id).ifPresent(created -> {
                        created.setLastSeenAt(now);
                        created.setSyncState("NEW");
                        kbArticleRepository.save(created);
                    });
                    continue;
                }

                // EXISTE
                KbArticle existing = existingOpt.get();
                existing.setLastSeenAt(now);

                boolean changed = hasChanged(existing, item);

                if (changed) {
                    kbArticleSyncService.sync(id);
                    existing.setSyncState("UPDATED");
                } else {
                    existing.setSyncState("UNCHANGED");
                }

                kbArticleRepository.save(existing);
            }
        }
    }

    /**
     * Detecta mudança usando revisionId (melhor) ou updatedDate.
     * - KbArticle.revisionId é Long
     * - SearchItemDto.revisionId é String (vamos tentar converter)
     */
    private boolean hasChanged(KbArticle existing, MovideskArticleSearchItemDto item) {

        // 1) revisionId (se vier e for parseável)
        Long incomingRev = parseRevisionId(item.getRevisionId());
        if (incomingRev != null) {
            Long currentRev = existing.getRevisionId();
            return currentRev == null || !incomingRev.equals(currentRev);
        }

        // 2) updatedDate
        OffsetDateTime incomingUpdated = parseUpdated(item.getUpdatedDate());
        if (incomingUpdated != null) {
            OffsetDateTime currentUpdated = existing.getUpdatedDate();
            return currentUpdated == null || incomingUpdated.isAfter(currentUpdated);
        }

        // 3) sem sinal → não sincroniza
        return false;
    }

    /**
     * Converte updatedDate string do Movidesk para OffsetDateTime.
     * Espera ISO 8601 (com Z ou offset).
     */
    private OffsetDateTime parseUpdated(String updatedDate) {
        if (updatedDate == null || updatedDate.isBlank()) return null;
        try {
            return OffsetDateTime.parse(updatedDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converte revisionId string para Long (quando possível).
     * Se não for numérico, retorna null e cai no updatedDate.
     */
    private Long parseRevisionId(String revisionId) {
        if (revisionId == null || revisionId.isBlank()) return null;
        try {
            return Long.parseLong(revisionId.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
