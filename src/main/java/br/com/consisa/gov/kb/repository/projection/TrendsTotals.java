package br.com.consisa.gov.kb.repository.projection;

public record TrendsTotals(
        long openedLast7,
        long resolvedLast7,
        long overdueLast7,
        long overduePrev7
) {
}
