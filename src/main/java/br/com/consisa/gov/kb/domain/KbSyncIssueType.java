package br.com.consisa.gov.kb.domain;

/**
 * Tipos de issues geradas durante a sincronização.
 * Usado em kb_sync_issue.
 */
public enum KbSyncIssueType {
    NOT_FOUND,
    EMPTY_CONTENT,
    MENU_NULL,
    MENU_NOT_MAPPED,
    ERROR
}
