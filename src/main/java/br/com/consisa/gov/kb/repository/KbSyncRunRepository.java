package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbSyncRun;
import br.com.consisa.gov.kb.domain.SyncRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbSyncRunRepository extends JpaRepository<KbSyncRun, Long> {

    Optional<KbSyncRun> findTop1ByOrderByStartedAtDesc();

    Optional<KbSyncRun> findTop1ByStatusOrderByFinishedAtDesc(SyncRunStatus status);
}
