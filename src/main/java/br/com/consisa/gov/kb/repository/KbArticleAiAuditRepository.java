package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticleAiAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbArticleAiAuditRepository extends JpaRepository<KbArticleAiAudit, Long> {
    Optional<KbArticleAiAudit> findByArticleId(Long articleId);
}
