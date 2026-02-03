package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceOverviewTotalsDto(
        long totalOpen,
        long errorOpen,
        long warnOpen,
        long infoOpen,
        long overdueOpen,
        long unassignedOpen,
        long criticalOpen
) {
}
