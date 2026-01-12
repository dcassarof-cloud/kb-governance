package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbSyncConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbSyncConfigRepository extends JpaRepository<KbSyncConfig, Long> { }
