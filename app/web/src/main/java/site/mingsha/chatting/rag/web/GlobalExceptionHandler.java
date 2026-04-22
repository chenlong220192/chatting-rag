package site.mingsha.chatting.rag.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Global exception handler for the web layer.
 *
 * <p>Sanitizes error responses returned to clients by never exposing raw
 * exception messages. Full stack traces and internal details are logged
 * server-side only.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles WebClientResponseException — thrown when an upstream HTTP call fails.
     * Propagates the upstream status code where possible.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException e) {
        log.error("[GlobalExceptionHandler] Upstream HTTP error: {} {} — {}",
                e.getStatusCode().value(), e.getStatusText(), e.getMessage(), e);
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of(
                        "error", "Upstream service error",
                        "code", "UPSTREAM_ERROR"
                ));
    }

    /**
     * Handles IllegalArgumentException — bad request from the client.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[GlobalExceptionHandler] Bad request: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid request",
                        "code", "BAD_REQUEST"
                ));
    }

    /**
     * Catches all other unexpected exceptions.
     * Logs the full stack trace and returns a generic 500 response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("[GlobalExceptionHandler] Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "An unexpected error occurred. Please try again later.",
                        "code", "INTERNAL_ERROR"
                ));
    }
}
