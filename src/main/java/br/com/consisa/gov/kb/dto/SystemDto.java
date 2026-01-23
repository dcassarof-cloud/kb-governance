package br.com.consisa.gov.kb.dto;

/**
 * Sistema (ConsisaNet, NotaOn, Quinto Eixo, etc).
 */
public record SystemDto(
        Long id,
        String code,
        String name,
        boolean active
) {}
