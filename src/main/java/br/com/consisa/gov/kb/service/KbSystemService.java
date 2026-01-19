package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbSystem;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🗂️ Service de acesso a sistemas da KB
 *
 * RESPONSABILIDADE:
 * -----------------
 * - Buscar sistemas por código
 * - Validar existência
 * - Lançar exceção se não encontrar
 *
 * POR QUE EXISTE?
 * ---------------
 * Evita acoplamento direto com repository.
 * Centraliza validações e logs.
 */
@Service
public class KbSystemService {

    private final KbSystemRepository repository;

    public KbSystemService(KbSystemRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca sistema por código.
     *
     * Lança exceção se não encontrar.
     *
     * @param code código do sistema (ex: GERAL, NOTAON, CONSISANET)
     * @return sistema encontrado
     * @throws IllegalStateException se sistema não existe
     */
    @Transactional(readOnly = true)
    public KbSystem getByCodeOrThrow(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Sistema não encontrado no banco: " + code
                ));
    }
}
