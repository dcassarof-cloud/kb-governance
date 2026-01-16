package br.com.consisa.gov.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de saída do relatório de governança
 *
 * Representa a análise de qualidade de um artigo da KB
 * focado em preparação para IA.
 */
public class KbArticleGovernanceReportDto {

    private Long articleId;
    private String systemCode;
    private String systemName;
    private String title;
    private String contentHash;
    private String sourceUrl;
    private OffsetDateTime updatedDate;

    // Flags booleanas (vindas da VIEW)
    private Boolean isEmpty;
    private Boolean isTooShort;
    private Boolean isDuplicateSameSystem;
    private Boolean isHashReusedOtherSystem;
    private Boolean lacksMinStructure;

    // Métricas auxiliares
    private Integer contentLength;
    private Integer headerCount;
    private Boolean hasLists;
    private Boolean hasActionVerbs;
    private Boolean hasSystemContext;

    /**
     * Lista de ações necessárias (calculada pelo service)
     */
    private List<String> actions = new ArrayList<>();

    /**
     * Flag calculada: está pronto para IA?
     */
    @JsonProperty("isIaReady")
    private Boolean iaReady;

    /**
     * Score de qualidade (0-100)
     * Calculado baseado nos critérios atendidos
     */
    private Integer qualityScore;

    // ===========================
    // Constantes de Ações
    // ===========================

    public static final String ACTION_MANUAL_VAZIO = "MANUAL_VAZIO";
    public static final String ACTION_MANUAL_CURTO = "MANUAL_CURTO_DEMAIS";
    public static final String ACTION_DUPLICADO_MESMO_SISTEMA = "MANUAL_DUPLICADO_NO_MESMO_SISTEMA";
    public static final String ACTION_HASH_OUTRO_SISTEMA = "HASH_REPETIDO_EM_OUTRO_SISTEMA_ALERTA";
    public static final String ACTION_SEM_ESTRUTURA = "MANUAL_SEM_ESTRUTURA_MINIMA";

    // ===========================
    // Métodos de Negócio
    // ===========================

    /**
     * Adiciona uma ação à lista
     */
    public void addAction(String action) {
        if (!this.actions.contains(action)) {
            this.actions.add(action);
        }
    }

    /**
     * Verifica se tem alguma ação crítica
     */
    public boolean hasCriticalIssues() {
        return isEmpty || isDuplicateSameSystem || lacksMinStructure;
    }

    /**
     * Verifica se tem apenas avisos (não críticos)
     */
    public boolean hasWarningsOnly() {
        return !actions.isEmpty() && !hasCriticalIssues();
    }

    /**
     * Calcula se está IA Ready
     * IA Ready = não tem nenhuma ação crítica
     */
    public void calculateIaReady() {
        this.iaReady = !hasCriticalIssues();
    }

    /**
     * Calcula score de qualidade (0-100)
     *
     * Critérios:
     * - Não vazio: 20 pontos
     * - Tamanho adequado: 20 pontos
     * - Não duplicado: 20 pontos
     * - Tem estrutura: 40 pontos
     */
    public void calculateQualityScore() {
        int score = 0;

        // 20 pontos: não vazio
        if (!Boolean.TRUE.equals(isEmpty)) {
            score += 20;
        }

        // 20 pontos: tamanho adequado
        if (!Boolean.TRUE.equals(isTooShort)) {
            score += 20;
        }

        // 20 pontos: não duplicado
        if (!Boolean.TRUE.equals(isDuplicateSameSystem)) {
            score += 20;
        }

        // 40 pontos: tem estrutura mínima
        if (!Boolean.TRUE.equals(lacksMinStructure)) {
            score += 40;
        }

        this.qualityScore = score;
    }

    // ===========================
    // Getters/Setters
    // ===========================

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public OffsetDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(OffsetDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Boolean getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(Boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    public Boolean getIsTooShort() {
        return isTooShort;
    }

    public void setIsTooShort(Boolean isTooShort) {
        this.isTooShort = isTooShort;
    }

    public Boolean getIsDuplicateSameSystem() {
        return isDuplicateSameSystem;
    }

    public void setIsDuplicateSameSystem(Boolean isDuplicateSameSystem) {
        this.isDuplicateSameSystem = isDuplicateSameSystem;
    }

    public Boolean getIsHashReusedOtherSystem() {
        return isHashReusedOtherSystem;
    }

    public void setIsHashReusedOtherSystem(Boolean isHashReusedOtherSystem) {
        this.isHashReusedOtherSystem = isHashReusedOtherSystem;
    }

    public Boolean getLacksMinStructure() {
        return lacksMinStructure;
    }

    public void setLacksMinStructure(Boolean lacksMinStructure) {
        this.lacksMinStructure = lacksMinStructure;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public void setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
    }

    public Integer getHeaderCount() {
        return headerCount;
    }

    public void setHeaderCount(Integer headerCount) {
        this.headerCount = headerCount;
    }

    public Boolean getHasLists() {
        return hasLists;
    }

    public void setHasLists(Boolean hasLists) {
        this.hasLists = hasLists;
    }

    public Boolean getHasActionVerbs() {
        return hasActionVerbs;
    }

    public void setHasActionVerbs(Boolean hasActionVerbs) {
        this.hasActionVerbs = hasActionVerbs;
    }

    public Boolean getHasSystemContext() {
        return hasSystemContext;
    }

    public void setHasSystemContext(Boolean hasSystemContext) {
        this.hasSystemContext = hasSystemContext;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public Boolean getIaReady() {
        return iaReady;
    }

    public void setIaReady(Boolean iaReady) {
        this.iaReady = iaReady;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }
}