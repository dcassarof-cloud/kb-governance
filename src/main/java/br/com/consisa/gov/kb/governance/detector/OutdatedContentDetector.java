package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * 📅 Detector: conteúdo desatualizado.
 *
 * REGRA DE NEGÓCIO (Sprint 2):
 * - Cria issue se o manual não é atualizado há mais de X dias
 * - Usa updated_at como referência principal
 * - Fallback para created_at se updated_at for nulo
 *
 * CONFIGURAÇÃO:
 * - MAX_DAYS_WITHOUT_UPDATE = 365 dias (1 ano)
 * - Pode ser ajustado conforme necessidade do negócio
 */
@Component
public class OutdatedContentDetector {

    private static final Logger log = LoggerFactory.getLogger(OutdatedContentDetector.class);

    /**
     * Máximo de dias sem atualização antes de considerar desatualizado.
     * Padrão: 365 dias (1 ano).
     */
    private static final int MAX_DAYS_WITHOUT_UPDATE = 365;

    private final KbGovernanceIssueService issueService;

    public OutdatedContentDetector(KbGovernanceIssueService issueService) {
        this.issueService = issueService;
    }

    /**
     * Analisa um artigo e cria issue se estiver desatualizado.
     *
     * @param article Artigo a ser analisado
     * @return true se criou/atualizou issue, false caso contrário
     */
    public boolean analyze(KbArticle article) {
        if (article == null || article.getId() == null) {
            return false;
        }

        // Usa updated_at ou created_at como fallback
        OffsetDateTime lastUpdate = article.getUpdatedDate();
        if (lastUpdate == null) {
            lastUpdate = article.getCreatedDate();
        }

        // Se não tem nenhuma data, não pode avaliar
        if (lastUpdate == null) {
            log.debug("Artigo {} sem data de atualização/criação, ignorando", article.getId());
            return false;
        }

        // Calcula dias desde última atualização
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long daysSinceUpdate = ChronoUnit.DAYS.between(lastUpdate, now);

        // Se está dentro do prazo, não cria issue
        if (daysSinceUpdate <= MAX_DAYS_WITHOUT_UPDATE) {
            return false;
        }

        // Determina severidade baseada no tempo
        GovernanceSeverity severity;
        if (daysSinceUpdate > MAX_DAYS_WITHOUT_UPDATE * 2) {
            severity = GovernanceSeverity.ERROR;  // Mais de 2 anos
        } else if (daysSinceUpdate > MAX_DAYS_WITHOUT_UPDATE * 1.5) {
            severity = GovernanceSeverity.WARN;   // Entre 1.5 e 2 anos
        } else {
            severity = GovernanceSeverity.INFO;   // Entre 1 e 1.5 anos
        }

        String msg = String.format(
                "Conteúdo desatualizado: última atualização há %d dias (limite: %d dias)",
                daysSinceUpdate, MAX_DAYS_WITHOUT_UPDATE
        );

        ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("daysSinceUpdate", daysSinceUpdate);
        evidence.put("maxDaysAllowed", MAX_DAYS_WITHOUT_UPDATE);
        evidence.put("lastUpdateDate", lastUpdate.toString());

        issueService.open(
                article.getId(),
                KbGovernanceIssueType.OUTDATED_CONTENT,
                severity,
                msg,
                evidence
        );

        log.debug("Issue OUTDATED_CONTENT criada para artigo {}: {} dias sem atualização",
                article.getId(), daysSinceUpdate);

        return true;
    }
}
