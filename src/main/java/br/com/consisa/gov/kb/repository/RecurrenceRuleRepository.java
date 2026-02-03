package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {
    List<RecurrenceRule> findByActiveTrue();
}
