package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.AppUser;
import br.com.consisa.gov.kb.domain.RefreshToken;
import br.com.consisa.gov.kb.repository.RefreshTokenRepository;
import br.com.consisa.gov.kb.security.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken issueToken(AppUser user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()));
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public Optional<RefreshToken> findValidToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(found -> found.getRevokedAt() == null)
                .filter(found -> found.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }
}
