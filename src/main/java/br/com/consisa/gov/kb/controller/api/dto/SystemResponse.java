package br.com.consisa.gov.kb.controller.api.dto;

/**
 * DTO para sistema.
 * 
 * Formato esperado pelo front:
 * {
 *   "id": 1,
 *   "code": "CONSISANET",
 *   "name": "ConsisaNET",
 *   "description": "Sistema ERP",
 *   "isActive": true
 * }
 */
public record SystemResponse(
        Long id,
        String code,
        String name,
        String description,
        Boolean isActive
) {
}
