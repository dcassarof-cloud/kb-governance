package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.governance.KbContentAnalysisService;
import br.com.consisa.gov.kb.governance.KbGovernanceDetector;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 🔍 Detector: INCONSISTENT_CONTENT
 *
 * REGRA DE NEGÓCIO (Sprint 2):
 * Cria issue se:
 * 1. Manual não tem system_id válido (null ou inativo)
 * 2. Manual está associado a sistema "GERAL" quando poderia estar em específico
 *
 * IMPORTANTE:
 * - Idempotente (não duplica issues)
 * - Log claro do motivo da issue
 */
@Component
public class InconsistentStructureDetector implements KbGovernanceDetector {

    private static final Logger log = LoggerFactory.getLogger(InconsistentStructureDetector.class);

    /** Código do sistema genérico que indica classificação incompleta */
    private static final String GENERIC_SYSTEM_CODE = "GERAL";

    private final KbContentAnalysisService analysis;
    private final KbGovernanceIssueService issueService;

    public InconsistentStructureDetector(KbContentAnalysisService analysis,
                                         KbGovernanceIssueService issueService) {
        this.analysis = analysis;
        this.issueService = issueService;
    }

    /**
     * Analisa um artigo e cria issue se tiver inconsistências estruturais.
     *
     * @param article Artigo a ser analisado
     * @return true se criou/atualizou issue, false caso contrário
     */
    @Override
    public void analyze(KbArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }

        // Verifica se tem sistema associado
        boolean noSystem = (article.getSystem() == null);

        // Verifica se está no sistema genérico "GERAL"
        boolean isGenericSystem = false;
        String systemCode = null;
        if (article.getSystem() != null) {
            systemCode = article.getSystem().getCode();
            isGenericSystem = GENERIC_SYSTEM_CODE.equalsIgnoreCase(systemCode);
        }

        // Se não tem problema, não cria issue
        if (!noSystem && !isGenericSystem) {
            return;
        }

        // Determina severidade e mensagem
        GovernanceSeverity severity;
        String reason;
        String msg;

        if (noSystem) {
            severity = GovernanceSeverity.ERROR;
            reason = "NO_SYSTEM";
            msg = "Artigo sem sistema/módulo associado — precisa ser classificado.";
        } else {
            severity = GovernanceSeverity.WARN;
            reason = "GENERIC_SYSTEM";
            msg = String.format("Artigo associado ao sistema genérico '%s' — considere classificar em sistema específico.", systemCode);
        }

        ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("reason", reason);
        evidence.put("systemCode", systemCode);
        evidence.put("noSystem", noSystem);
        evidence.put("isGenericSystem", isGenericSystem);

        issueService.open(
                article.getId(),
                KbGovernanceIssueType.INCONSISTENT_CONTENT,
                severity,
                msg,
                evidence
        );

        log.debug("Issue INCONSISTENT_CONTENT criada para artigo {}: {}", article.getId(), reason);
    }

    /**
     * Modo forçado: cria issue para TODOS os artigos (reforma geral da base).
     * Usar com cautela — apenas para migrações ou auditorias completas.
     *
     * @param article Artigo a ser analisado
     * @deprecated Usar analyze() que segue regras de negócio
     */
    @Deprecated
    public void analyzeForced(KbArticle article) {
        if (article == null || article.getId() == null) return;

        int textLen = analysis.length(article.getContentText());
        int htmlLen = analysis.length(article.getContentHtml());

        String msg = "Manual marcado para revisão geral (modo forçado).";

        ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("forced", true);
        evidence.put("textLen", textLen);
        evidence.put("htmlLen", htmlLen);
        evidence.put("reason", "FORCED_REVIEW");

        issueService.open(
                article.getId(),
                KbGovernanceIssueType.INCONSISTENT_CONTENT,
                GovernanceSeverity.WARN,
                msg,
                evidence
        );
    }
}
