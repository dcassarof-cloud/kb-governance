package br.com.consisa.gov.kb.client.movidesk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client HTTP responsável por consumir a API pública do Movidesk.
 *
 * Papel na arquitetura:
 * Controller -> Service -> MovideskClient -> API Movidesk
 *
 * Importante:
 * - NÃO contém regra de negócio
 * - NÃO conhece banco
 * - NÃO decide governança
 *
 * Apenas:
 * - chama API
 * - lança exceções
 * - loga com segurança
 */
@Component
public class MovideskClient {

    private static final Logger log =
            LoggerFactory.getLogger(MovideskClient.class);

    private final RestClient restClient;
    private final String token;

    public MovideskClient(
            RestClient movideskRestClient,
            @Value("${movidesk.token}") String token
    ) {
        this.restClient = movideskRestClient;
        this.token = token;
    }

    /**
     * Busca um artigo completo pelo ID.
     */
    public MovideskArticleDto getArticleById(long id) {
        try {
            log.debug("Movidesk: buscando artigo id={}", id);

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/article/{id}")
                            .queryParam("token", token)
                            .build(id))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MovideskArticleDto.class);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Erro Movidesk getArticleById id={} status={} body={}",
                    id, ex.getStatusCode(), safeBody(ex));
            throw ex;

        } catch (ResourceAccessException ex) {
            // timeout / DNS / conexão
            log.error("Erro de rede Movidesk getArticleById id={} msg={}",
                    id, safeMsg(ex));
            throw ex;
        }
    }

    /**
     * Search oficial da KB (usado no syncAll).
     */
    public MovideskArticleSearchResponse searchArticles(int page, int pageSize) {
        try {
            log.debug("Movidesk: searchArticles page={} pageSize={}", page, pageSize);

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/kb/article")
                            .queryParam("page", page)
                            .queryParam("pageSize", pageSize)
                            .queryParam("status", 1)
                            .queryParam("token", token)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(MovideskArticleSearchResponse.class);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Erro Movidesk searchArticles page={} status={} body={}",
                    page, ex.getStatusCode(), safeBody(ex));
            throw ex;

        } catch (ResourceAccessException ex) {
            log.error("Erro de rede Movidesk searchArticles page={} msg={}",
                    page, safeMsg(ex));
            throw ex;
        }
    }

    /* =========================================================
       HELPERS DE LOG (seguros)
       ========================================================= */

    private String safeBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null) return "";
        return body.length() > 1500
                ? body.substring(0, 1500) + "...(cortado)"
                : body;
    }

    private String safeMsg(Exception e) {
        return (e.getMessage() == null) ? "(null)" : e.getMessage();
    }
}
