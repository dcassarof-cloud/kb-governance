package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class IssueTypeMetaRegistry {

    public record IssueTypeMeta(String displayName, String description, String recommendation) {}

    private final Map<KbGovernanceIssueType, IssueTypeMeta> registry;

    public IssueTypeMetaRegistry() {
        Map<KbGovernanceIssueType, IssueTypeMeta> meta = new EnumMap<>(KbGovernanceIssueType.class);
        meta.put(KbGovernanceIssueType.REVIEW_REQUIRED, new IssueTypeMeta(
                "Revisão necessária",
                "Conteúdo precisa de revisão editorial ou técnica.",
                "Direcione para revisão com especialista do sistema."
        ));
        meta.put(KbGovernanceIssueType.NOT_AI_READY, new IssueTypeMeta(
                "Não pronto para IA",
                "Manual não atende aos critérios mínimos de IA-ready.",
                "Complemente seções obrigatórias e valide conteúdo."
        ));
        meta.put(KbGovernanceIssueType.DUPLICATE_CONTENT, new IssueTypeMeta(
                "Conteúdo duplicado",
                "Artigo duplicado detectado no mesmo ou em outro sistema.",
                "Consolide os artigos e mantenha apenas a versão correta."
        ));
        meta.put(KbGovernanceIssueType.INCOMPLETE_CONTENT, new IssueTypeMeta(
                "Conteúdo incompleto",
                "O artigo não possui informações suficientes para uso.",
                "Inclua passos, contexto e detalhes faltantes."
        ));
        meta.put(KbGovernanceIssueType.INCONSISTENT_CONTENT, new IssueTypeMeta(
                "Conteúdo inconsistente",
                "O conteúdo possui divergências ou conflitos.",
                "Revisar regras e alinhar com o sistema atual."
        ));
        meta.put(KbGovernanceIssueType.OUTDATED_CONTENT, new IssueTypeMeta(
                "Conteúdo desatualizado",
                "O artigo está desatualizado em relação ao sistema.",
                "Atualize o conteúdo conforme o processo vigente."
        ));
        this.registry = Map.copyOf(meta);
    }

    public IssueTypeMeta getMeta(KbGovernanceIssueType type) {
        if (type == null) {
            return null;
        }
        return registry.get(type);
    }
}
