package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO que representa a resposta da API de busca de artigos
 * da Knowledge Base do Movidesk.
 *
 * 👉 Este objeto encapsula:
 * - Informações de paginação
 * - Lista de artigos encontrados
 *
 * Normalmente utilizado em:
 * - Telas de listagem
 * - Sincronizações em lote
 * - Paginação de resultados
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskArticleSearchResponse {

    /**
     * Quantidade de registros retornados por página
     */
    private Integer pageSize;

    /**
     * Quantidade total de registros disponíveis na busca
     */
    private Integer totalSize;

    /**
     * Lista de artigos retornados na busca
     * Cada item contém apenas informações resumidas
     */
    private List<MovideskArticleSearchItemDto> items;

    /* ======================
       Getters e Setters
       ====================== */

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Integer totalSize) {
        this.totalSize = totalSize;
    }

    public List<MovideskArticleSearchItemDto> getItems() {
        return items;
    }

    public void setItems(List<MovideskArticleSearchItemDto> items) {
        this.items = items;
    }
}
