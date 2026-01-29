package br.com.consisa.gov.kb.domain;

/**
 * Status de governança de um artigo.
 *
 * REGRA DE NEGÓCIO (Sprint 2):
 * - OK = artigo sem issues abertas (OPEN ou IN_PROGRESS)
 * - WITH_ISSUES = artigo com pelo menos 1 issue aberta
 * - IGNORED = artigo ignorado da governança (ex: legado, descontinuado)
 *
 * IMPORTANTE:
 * - Este status é CALCULADO on-the-fly com base nas issues
 * - Não é persistido no artigo (evita inconsistência)
 * - Usado para dashboard, relatórios e filtros
 */
public enum GovernanceStatus {
    OK,
    WITH_ISSUES,
    IGNORED
}
