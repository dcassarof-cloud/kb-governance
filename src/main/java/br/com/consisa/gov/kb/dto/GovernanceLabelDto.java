package br.com.consisa.gov.kb.dto;

public record GovernanceLabelDto(
        String code,
        String label,
        String description,
        String color,
        String impactSummary,
        String businessImpactLevel
) {
}
