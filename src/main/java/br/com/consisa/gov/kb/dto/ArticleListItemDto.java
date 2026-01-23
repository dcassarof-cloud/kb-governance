package br.com.consisa.gov.kb.dto;

import java.time.Instant;

/**
 * Item de lista de artigos para tela de Artigos.
 * Mantém só o necessário para tabela/listagem.
 */
public record ArticleListItemDto(
        Long id,
        String title,
        String slug,
        String manualLink,
        String systemCode,
        String systemName,
        String governanceStatus,
        Instant updatedAt
) {}
