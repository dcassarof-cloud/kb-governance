package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    long deleteByExpiresAtBefore(OffsetDateTime timestamp);
}
