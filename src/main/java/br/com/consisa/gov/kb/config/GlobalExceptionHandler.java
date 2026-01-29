package br.com.consisa.gov.kb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
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
            String traceId,
            int status,
            String error,
            String message,
            String path,
            OffsetDateTime timestamp
    ) {}

    /**
     * Handler para IllegalStateException (ex: sync já em execução).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        String traceId = generateTraceId();

        log.warn("⚠️ [{}] IllegalStateException: {}", traceId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                traceId,
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handler para IllegalArgumentException (ex: parâmetro inválido).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = generateTraceId();

        log.warn("⚠️ [{}] IllegalArgumentException: {}", traceId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                traceId,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler para recurso não encontrado (ex: rota inexistente).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String traceId = generateTraceId();

        log.warn("⚠️ [{}] Recurso não encontrado: {}", traceId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                traceId,
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Recurso não encontrado",
                request != null ? request.getRequestURI() : null,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handler genérico para todas as outras exceções.
     *
     * REGRA: Erro real deve retornar HTTP 500, não lista vazia.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = generateTraceId();

        // Log completo com stack trace para debug no servidor
        log.error("❌ [{}] Erro não tratado: {}", traceId, ex.getMessage(), ex);

        // Resposta resumida para o cliente (sem expor detalhes internos)
        ErrorResponse response = new ErrorResponse(
                traceId,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno do servidor. TraceId: " + traceId,
                null,
                OffsetDateTime.now(ZoneOffset.UTC)
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
}
