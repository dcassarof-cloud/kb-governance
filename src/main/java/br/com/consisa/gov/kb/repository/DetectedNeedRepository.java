package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.DetectedNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetectedNeedRepository extends JpaRepository<DetectedNeed, Long> {
    Optional<DetectedNeed> findByClusterIdAndRuleId(Long clusterId, Long ruleId);

    long countByStatus(String status);
}
