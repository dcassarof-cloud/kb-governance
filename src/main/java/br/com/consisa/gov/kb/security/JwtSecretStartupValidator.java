package br.com.consisa.gov.kb.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class JwtSecretStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretStartupValidator.class);

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtSecretStartupValidator(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            log.info("JWT secret validation skipped for dev/test profile.");
            return;
        }

        String secret = jwtProperties.secret();

        if (secret == null || secret.isBlank()) {
            failStartup("JWT secret é obrigatório e não pode estar vazio.");
        }

        String normalized = secret.toLowerCase(Locale.ROOT);
        if (normalized.contains("change-me") || normalized.contains("dev-secret") || normalized.contains("default")) {
            failStartup("JWT secret está com valor inseguro/padrão. Ajuste antes de subir.");
        }

        if (secret.length() < 32) {
            failStartup("JWT secret deve ter pelo menos 32 caracteres.");
        }
    }

    private void failStartup(String message) {
        log.error("❌ Falha de configuração crítica: {}", message);
        throw new IllegalStateException(message);
    }
}
