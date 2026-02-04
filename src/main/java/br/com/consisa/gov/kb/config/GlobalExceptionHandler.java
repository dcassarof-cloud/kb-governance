package br.com.consisa.gov.kb.config;

import br.com.consisa.gov.kb.exception.IntegrationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 🛡️ Handler Global de Exceções
 *
 * REGRA DE NEGÓCIO (Sprint 1):
 * - Sem dados ≠ erro (retorna lista vazia ou null)
 * - Erro ≠ sucesso silencioso (retorna HTTP 500 com detalhes)
 *
 * IMPORTANTE:
 * - Toda exceção não tratada passa por aqui
 * - Gera traceId único para rastreabilidade
 * - Log completo no servidor, resposta resumida para o cliente
 * - Nunca mascara erro real com "lista vazia"
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * DTO de resposta de erro padronizada.
     */
    public record ErrorResponse(
            String message,
            String details,
            OffsetDateTime timestamp,
            String path
    ) {}

    /**
     * Handler para IllegalStateException (ex: sync já em execução).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        log.warn("⚠️ [requestId={}] IllegalStateException: {}", requestId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Conflito na operação.",
                ex.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handler para IllegalArgumentException (ex: parâmetro inválido).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        log.warn("⚠️ [requestId={}] IllegalArgumentException: {}", requestId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Parâmetro inválido.",
                ex.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler para erros de parsing do corpo da requisição.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        log.warn("⚠️ [requestId={}] HttpMessageNotReadableException: {}", requestId, ex.getMessage(), ex);

        String message = "Formato inválido para o corpo da requisição.";
        String details = ex.getMessage();
        if (details != null && details.contains("dueDate")) {
            message = "Formato inválido para dueDate. Use yyyy-MM-dd.";
        }

        ErrorResponse response = new ErrorResponse(
                message,
                details,
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler para recurso não encontrado (ex: rota inexistente).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        log.warn("⚠️ [requestId={}] Recurso não encontrado: {}", requestId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Recurso não encontrado",
                ex.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handler genérico para todas as outras exceções.
     *
     * REGRA: Erro real deve retornar HTTP 500, não lista vazia.
     */
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(IntegrationException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        log.warn("⚠️ [requestId={}] Erro de integração: {}", requestId, ex.getMessage(), ex);

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_GATEWAY;

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                "Falha ao comunicar com serviço externo.",
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handler para ResponseStatusException (400/404/502/etc).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;

        log.warn("⚠️ [requestId={}] ResponseStatusException {}: {}", requestId, resolved, ex.getReason(), ex);

        ErrorResponse response = new ErrorResponse(
                ex.getReason() != null ? ex.getReason() : resolved.getReasonPhrase(),
                ex.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(resolved).body(response);
    }

    /**
     * Handler genérico para todas as outras exceções.
     *
     * REGRA: Erro real deve retornar HTTP 500, não lista vazia.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);

        // Log completo com stack trace para debug no servidor
        log.error("❌ [requestId={}] Erro não tratado: {}", requestId, ex.getMessage(), ex);

        // Resposta resumida para o cliente (sem expor detalhes internos)
        ErrorResponse response = new ErrorResponse(
                "Erro interno do servidor.",
                "Erro inesperado. requestId=" + requestId,
                OffsetDateTime.now(ZoneOffset.UTC),
                requestPath(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Gera um traceId único para rastreabilidade.
     * Formato: 8 caracteres hexadecimais (ex: "a1b2c3d4")
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return generateTraceId();
        }
        String requestId = request.getHeader("x-request-id");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("x-correlation-id");
        }
        return (requestId == null || requestId.isBlank()) ? generateTraceId() : requestId;
    }

    private String requestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }
}
