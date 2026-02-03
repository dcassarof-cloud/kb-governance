package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.FaqCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaqClusterRepository extends JpaRepository<FaqCluster, Long> {
    Optional<FaqCluster> findByFingerprint(String fingerprint);
}
