package br.com.consisa.gov.kb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuração de fallback do ObjectMapper.
 *
     * Usa o builder padrão do Spring Boot quando disponível para garantir:
     * - compatibilidade com MappingJackson2HttpMessageConverter
     * - respeito às customizações padrão do Spring Boot
     * - criação apenas quando não existe bean definido
     *
     * Quando o builder não está disponível, cria um ObjectMapper padrão
     * e registra módulos automaticamente para manter o comportamento esperado.
     */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper(ObjectProvider<Jackson2ObjectMapperBuilder> builderProvider) {
        Jackson2ObjectMapperBuilder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            return builder.build();
        }
        return new ObjectMapper().findAndRegisterModules();
    }
}
