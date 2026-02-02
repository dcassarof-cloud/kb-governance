package br.com.consisa.gov.kb.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends RuntimeException {

    private final HttpStatus status;

    public IntegrationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_GATEWAY;
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_GATEWAY;
    }

    public IntegrationException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status == null ? HttpStatus.BAD_GATEWAY : status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
