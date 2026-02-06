package br.com.consisa.gov.kb.repository.projection;

public record SlaComplianceTotals(
        long totalResolved,
        long resolvedOnTime
) {
}
