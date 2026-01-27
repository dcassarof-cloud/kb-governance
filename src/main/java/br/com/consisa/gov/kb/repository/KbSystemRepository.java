package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA responsável pelo acesso à entidade KbSystem.
 *
 * Utilizado para:
 * - Buscar sistemas/módulos pelo código lógico (ex: QUINTOEIXO)
 * - Persistir e manter catálogo de sistemas da KB
 */
public interface KbSystemRepository extends JpaRepository<KbSystem, Long> {

    /**
     * Busca um sistema pelo código único.
     *
     * Exemplo:
     * - QUINTOEIXO
     * - SGRH
     *
     * Retorna Optional para evitar NullPointerException
     * e forçar tratamento no Service.
     */
    Optional<KbSystem> findByCode(String code);
    List<KbSystem> findByIsActiveTrueOrderByNameAsc();

}
