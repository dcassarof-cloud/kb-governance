package br.com.consisa.gov.kb.controller.api.dto;

public record SupportImportResponse(
        int ticketsCreated,
        int ticketsUpdated,
        int messagesCreated
) {
}
