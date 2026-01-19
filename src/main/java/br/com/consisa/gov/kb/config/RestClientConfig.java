package br.com.consisa.gov.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient movideskRestClient() {

        HttpClient httpClient = HttpClient.newBuilder()
                // ✅ força HTTP/1.1 (evita alguns problemas de proxy/DNS com HTTP/2)
                .version(HttpClient.Version.HTTP_1_1)
                // ✅ timeout de conexão
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        // ✅ timeout de leitura (request/response)
        factory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.movidesk.com/public/v1")
                .build();
    }
}
