package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbMenuMap;
import br.com.consisa.gov.kb.repository.KbMenuMapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 🧠 Serviço de leitura do mapeamento Menu -> Sistema
 *
 * RESPONSABILIDADES:
 * -------------------------------------------------
 * - Centralizar regra de busca
 * - Evitar acesso direto ao repository no syncAll
 * - Proteger contra parâmetros inválidos
 */
@Service
public class KbMenuMapService {

    private final KbMenuMapRepository repository;

    public KbMenuMapService(KbMenuMapRepository repository) {
        this.repository = repository;
    }

    /**
     * Retorna o mapeamento ATIVO para um menu.
     *
     * @Transactional(readOnly = true)
     * - mantém sessão aberta
     * - garante consistência
     *
     * Retorno:
     * - Optional.empty() se não houver mapeamento
     */
    @Transactional(readOnly = true)
    public Optional<KbMenuMap> findActive(String sourceSystem, Long sourceMenuId) {

        // 🛡️ validações defensivas
        if (sourceSystem == null || sourceSystem.isBlank()) return Optional.empty();
        if (sourceMenuId == null) return Optional.empty();

        return repository.findFirstBySourceSystemAndSourceMenuIdAndActiveTrue(
                sourceSystem,
                sourceMenuId
        );
    }
}
