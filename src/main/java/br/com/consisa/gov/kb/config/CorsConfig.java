package br.com.consisa.gov.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS para o front (Vite/React).
 *
 * Analogia simples:
 * - CORS é a "catraca" do prédio.
 * - Sem liberar, o navegador não deixa o front entrar no back.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // ou "/api/v1/**" se você tiver prefixo
                        .allowedOrigins(
                                "http://localhost:8080",
                                "http://localhost:5173"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("x-correlation-id")
                        .allowCredentials(false) // deixe false se você usa Bearer Token
                        .maxAge(3600);
            }
        };
    }
}
