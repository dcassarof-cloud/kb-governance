package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbSyncRunRepository extends JpaRepository<KbSyncRun, Long> {
    Optional<KbSyncRun> findTop1ByOrderByStartedAtDesc();
}
