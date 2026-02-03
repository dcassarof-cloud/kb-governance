package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceOverviewSystemDto(
        String systemCode,
        String systemName,
        long totalOpen,
        long errorOpen,
        long warnOpen,
        long infoOpen,
        long overdueOpen,
        long unassignedOpen,
        long criticalOpen,
        double healthScore
) {
}
