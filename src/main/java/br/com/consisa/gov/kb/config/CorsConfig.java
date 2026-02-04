package br.com.consisa.gov.kb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Bean
    @Profile("dev")
    public CorsConfigurationSource devCorsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        return buildConfiguration(allowedOrigins, true);
    }

    @Bean
    @Profile("prod")
    public CorsConfigurationSource prodCorsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        return buildConfiguration(allowedOrigins, false);
    }

    private CorsConfigurationSource buildConfiguration(String origins, boolean allowCredentials) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> originList = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());

        if (originList.contains("*")) {
            originList = originList.stream()
                    .filter(origin -> !origin.equals("*"))
                    .collect(Collectors.toList());
        }

        config.setAllowedOrigins(originList);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(allowCredentials && !originList.contains("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
