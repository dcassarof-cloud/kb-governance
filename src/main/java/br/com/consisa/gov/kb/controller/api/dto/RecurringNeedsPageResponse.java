package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record RecurringNeedsPageResponse(
        List<RecurringNeedItemResponse> items,
        int page,
        int size,
        long total
) {
}
