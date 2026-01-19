package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbMenuMap;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository do mapeamento oficial Menu -> Sistema.
 *
 * Regra:
 * - Um mapeamento ativo por (source_system + source_menu_id)
 *
 * Importante:
 * - Carrega o system junto (EntityGraph) pra evitar LazyInitializationException
 */
public interface KbMenuMapRepository extends JpaRepository<KbMenuMap, Long> {

    @EntityGraph(attributePaths = "system")
    Optional<KbMenuMap> findBySourceSystemAndSourceMenuIdAndActiveTrue(
            String sourceSystem,
            Long sourceMenuId
    );
}