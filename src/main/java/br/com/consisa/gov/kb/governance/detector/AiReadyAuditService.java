package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbArticleAiAudit;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleAiAuditRepository;
import br.com.consisa.gov.kb.service.GovernanceIssueWorkflowService;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiReadyAuditService {

    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_SPLIT = Pattern.compile("(?m)^\\s{0,3}(#+\\s+|[A-ZÇÃÕÁÉÍÓÚ].{0,40}:)");
    private static final Pattern LIST_ITEM = Pattern.compile("(?m)^\\s*(\\d+\\.|-\\s+|•\\s+).+");

    private final KbArticleAiAuditRepository auditRepository;
    private final KbGovernanceIssueService issueService;
    private final GovernanceIssueWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    public AiReadyAuditService(
            KbArticleAiAuditRepository auditRepository,
            KbGovernanceIssueService issueService,
            GovernanceIssueWorkflowService workflowService,
            ObjectMapper objectMapper
    ) {
        this.auditRepository = auditRepository;
        this.issueService = issueService;
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void audit(KbArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }

        String raw = buildRawContent(article);
        String normalized = raw.toLowerCase(Locale.ROOT);

        List<String> missing = new ArrayList<>();
        int score = 0;

        boolean hasObjective = hasSection(normalized, "objetivo");
        score += mark("objetivo", hasObjective, missing);

        boolean hasWhenUse = hasSection(normalized, "quando utilizar", "quando usar");
        score += mark("quando utilizar", hasWhenUse, missing);

        boolean hasAccess = hasSection(normalized, "como acessar", "acesso");
        score += mark("como acessar", hasAccess, missing);

        boolean hasPrereq = hasSection(normalized, "pré-requisitos", "prerequisitos", "requisitos");
        score += mark("pré-requisitos", hasPrereq, missing);

        int rulesCount = countItemsForSection(raw, "regras de negócio", "regras do negocio", "regra de negócio");
        boolean hasRules = rulesCount >= 1;
        score += mark("regras de negócio", hasRules, missing);

        boolean hasFields = hasSection(normalized, "campos", "campo");
        score += mark("campos", hasFields, missing);

        int stepsCount = countItemsForSection(raw, "passo a passo", "passos");
        boolean hasSteps = stepsCount >= 3;
        score += mark("passo a passo (>=3)", hasSteps, missing);

        boolean hasErrors = hasSection(normalized, "erros comuns", "problemas comuns");
        score += mark("erros comuns", hasErrors, missing);

        int faqCount = countItemsForSection(raw, "faq", "perguntas frequentes");
        boolean hasFaq = faqCount >= 1;
        score += mark("faq (>=1)", hasFaq, missing);

        int intentCount = countItemsForSection(raw, "intenções ia", "intencoes ia", "intenções de ia", "intencoes de ia");
        boolean hasIntent = intentCount >= 3;
        score += mark("intenções ia (>=3)", hasIntent, missing);

        boolean passed = missing.isEmpty();

        ObjectNode details = objectMapper.createObjectNode();
        details.put("rulesCount", rulesCount);
        details.put("stepsCount", stepsCount);
        details.put("faqCount", faqCount);
        details.put("intentCount", intentCount);
        details.put("emailsDetected", countMatches(EMAIL, raw));

        KbArticleAiAudit audit = auditRepository.findByArticleId(article.getId())
                .orElseGet(KbArticleAiAudit::new);
        audit.setArticleId(article.getId());
        audit.setPassed(passed);
        audit.setScore(score);
        audit.setMissingSections(String.join(", ", missing));
        audit.setDetailsJson(details);
        audit.setAuditedAt(OffsetDateTime.now(ZoneOffset.UTC));
        auditRepository.save(audit);

        if (!passed) {
            issueService.open(
                    article.getId(),
                    KbGovernanceIssueType.NOT_AI_READY,
                    GovernanceSeverity.WARN,
                    "Checklist IA-ready incompleto: " + String.join(", ", missing),
                    details
            );
        } else {
            workflowService.updateStatusIfOpen(article.getId(), KbGovernanceIssueType.NOT_AI_READY, GovernanceIssueStatus.RESOLVED, "ai-audit");
        }
    }

    private int mark(String label, boolean ok, List<String> missing) {
        if (ok) {
            return 10;
        }
        missing.add(label);
        return 0;
    }

    private String buildRawContent(KbArticle article) {
        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null) {
            sb.append(article.getTitle()).append("\n");
        }
        if (article.getContentText() != null) {
            sb.append(article.getContentText());
        } else if (article.getContentHtml() != null) {
            sb.append(article.getContentHtml());
        }
        return sb.toString();
    }

    private boolean hasSection(String normalized, String... keys) {
        for (String key : keys) {
            if (normalized.contains(key.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int countItemsForSection(String raw, String... keys) {
        String section = extractSection(raw, keys);
        if (section.isBlank()) {
            return 0;
        }
        Matcher matcher = LIST_ITEM.matcher(section);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String extractSection(String raw, String... keys) {
        String lower = raw.toLowerCase(Locale.ROOT);
        int index = -1;
        for (String key : keys) {
            int pos = lower.indexOf(key.toLowerCase(Locale.ROOT));
            if (pos >= 0) {
                index = pos;
                break;
            }
        }
        if (index < 0) {
            return "";
        }
        String tail = raw.substring(index);
        Matcher matcher = SECTION_SPLIT.matcher(tail);
        if (matcher.find()) {
            if (matcher.find()) {
                return tail.substring(0, matcher.start());
            }
        }
        return tail;
    }

    private int countMatches(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(raw);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
