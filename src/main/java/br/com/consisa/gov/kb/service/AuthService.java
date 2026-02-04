package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.AppUser;
import br.com.consisa.gov.kb.domain.RefreshToken;
import br.com.consisa.gov.kb.repository.AppUserRepository;
import br.com.consisa.gov.kb.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       AppUserRepository userRepository,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuário não encontrado"));

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                List.copyOf(user.getRoles())
        );
        RefreshToken refreshToken = refreshTokenService.issueToken(user);

        return new AuthTokens(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthTokens refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.findValidToken(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Refresh token inválido"));

        AppUser user = refreshToken.getUser();
        refreshTokenService.revoke(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                List.copyOf(user.getRoles())
        );
        RefreshToken newRefreshToken = refreshTokenService.issueToken(user);

        return new AuthTokens(newAccessToken, newRefreshToken.getToken());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenService.findValidToken(refreshTokenValue)
                .ifPresent(refreshTokenService::revoke);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
