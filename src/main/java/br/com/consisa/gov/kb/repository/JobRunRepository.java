package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.JobRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunRepository extends JpaRepository<JobRun, Long> {
}
