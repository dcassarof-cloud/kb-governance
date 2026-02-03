package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.IssueTypeMeta;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry de metadados para tipos de issues de governança.
 *
 * <p>Centraliza as informações amigáveis (displayName, description, recommendation)
 * para cada tipo de issue, facilitando a exibição no frontend.
 *
 * <p>Exemplo de uso:
 * <pre>
 * IssueTypeMeta meta = registry.getMeta(KbGovernanceIssueType.INCOMPLETE_CONTENT);
 * String displayName = meta.displayName(); // "Conteúdo Incompleto"
 * </pre>
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
@Component
public class IssueTypeMetaRegistry {

    private final Map<KbGovernanceIssueType, IssueTypeMeta> registry;

    public IssueTypeMetaRegistry() {
        this.registry = new EnumMap<>(KbGovernanceIssueType.class);
        initializeRegistry();
    }

    private void initializeRegistry() {
        register(new IssueTypeMeta(
                KbGovernanceIssueType.REVIEW_REQUIRED,
                "Revisão Necessária",
                "O artigo precisa de revisão manual por um especialista.",
                "Agende uma revisão com o responsável pelo sistema para validar e atualizar o conteúdo."
        ));

        register(new IssueTypeMeta(
                KbGovernanceIssueType.NOT_AI_READY,
                "Não Preparado para IA",
                "O artigo não atende aos critérios mínimos para uso por assistentes de IA.",
                "Estruture o conteúdo com seções claras, adicione exemplos práticos e remova ambiguidades."
        ));

        register(new IssueTypeMeta(
                KbGovernanceIssueType.DUPLICATE_CONTENT,
                "Conteúdo Duplicado",
                "Existe outro artigo com conteúdo idêntico ou muito similar.",
                "Consolide os artigos duplicados em um único, redirecionando ou removendo os demais."
        ));

        register(new IssueTypeMeta(
                KbGovernanceIssueType.INCOMPLETE_CONTENT,
                "Conteúdo Incompleto",
                "O artigo está vazio ou com conteúdo insuficiente para ser útil.",
                "Complete o artigo com informações relevantes, exemplos e passo-a-passo quando aplicável."
        ));

        register(new IssueTypeMeta(
                KbGovernanceIssueType.INCONSISTENT_CONTENT,
                "Estrutura Inconsistente",
                "O artigo não segue o padrão de estrutura esperado para a base de conhecimento.",
                "Reorganize o conteúdo seguindo o template padrão: título, descrição, procedimento e observações."
        ));

        register(new IssueTypeMeta(
                KbGovernanceIssueType.OUTDATED_CONTENT,
                "Conteúdo Desatualizado",
                "O artigo não é atualizado há muito tempo e pode conter informações obsoletas.",
                "Revise o conteúdo verificando se os procedimentos e informações ainda são válidos."
        ));
    }

    private void register(IssueTypeMeta meta) {
        registry.put(meta.type(), meta);
    }

    /**
     * Retorna os metadados de um tipo de issue.
     *
     * @param type tipo da issue
     * @return metadados ou Optional vazio se tipo não registrado
     */
    public Optional<IssueTypeMeta> getMeta(KbGovernanceIssueType type) {
        return Optional.ofNullable(registry.get(type));
    }

    /**
     * Retorna os metadados de um tipo de issue pelo nome.
     *
     * @param typeName nome do tipo (case-insensitive)
     * @return metadados ou Optional vazio se tipo não encontrado
     */
    public Optional<IssueTypeMeta> getMeta(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return Optional.empty();
        }

        try {
            KbGovernanceIssueType type = KbGovernanceIssueType.valueOf(typeName.toUpperCase());
            return getMeta(type);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Retorna todos os metadados registrados.
     *
     * @return lista de metadados
     */
    public List<IssueTypeMeta> getAllMeta() {
        return List.copyOf(registry.values());
    }

    /**
     * Retorna o displayName de um tipo, ou o próprio nome se não registrado.
     *
     * @param type tipo da issue
     * @return displayName ou nome do enum
     */
    public String getDisplayName(KbGovernanceIssueType type) {
        return getMeta(type)
                .map(IssueTypeMeta::displayName)
                .orElse(type != null ? type.name() : "UNKNOWN");
    }

    /**
     * Retorna o displayName de um tipo pelo nome.
     *
     * @param typeName nome do tipo
     * @return displayName ou o próprio typeName
     */
    public String getDisplayName(String typeName) {
        return getMeta(typeName)
                .map(IssueTypeMeta::displayName)
                .orElse(typeName);
    }

    /**
     * Retorna a descrição de um tipo.
     *
     * @param type tipo da issue
     * @return descrição ou null
     */
    public String getDescription(KbGovernanceIssueType type) {
        return getMeta(type)
                .map(IssueTypeMeta::description)
                .orElse(null);
    }

    /**
     * Retorna a recomendação de um tipo.
     *
     * @param type tipo da issue
     * @return recomendação ou null
     */
    public String getRecommendation(KbGovernanceIssueType type) {
        return getMeta(type)
                .map(IssueTypeMeta::recommendation)
                .orElse(null);
    }
}
