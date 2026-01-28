package br.com.consisa.gov.kb.domain;

/**
 * Status de uma issue de governança.
 *
 * REGRA DE NEGÓCIO (Sprint 1):
 * - OPEN = issue detectada, aguardando análise
 * - IN_PROGRESS = analista assumiu a issue, em tratamento
 * - RESOLVED = issue resolvida/fechada
 *
 * IMPORTANTE:
 * - "Issue aberta" = OPEN ou IN_PROGRESS
 * - Quando um analista "pega" uma issue, ela continua sendo problema aberto
 * - Só deixa de ser problema quando status = RESOLVED
 */
public enum GovernanceIssueStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
}
