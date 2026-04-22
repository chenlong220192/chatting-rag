package site.mingsha.chatting.rag.test.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import site.mingsha.chatting.rag.web.GlobalExceptionHandler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void handleWebClientResponseException_returns502WithUpstreamErrorCode() {
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                null, null, null, null
        );

        ResponseEntity<Map<String, Object>> entity = handler.handleWebClientResponseException(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody()).containsEntry("code", "UPSTREAM_ERROR");
    }

    @Test
    void handleIllegalArgumentException_returns400WithBadRequestCode() {
        IllegalArgumentException ex = new IllegalArgumentException("message is blank");

        ResponseEntity<Map<String, Object>> entity = handler.handleIllegalArgumentException(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody()).containsEntry("code", "BAD_REQUEST");
        assertThat(entity.getBody()).containsEntry("error", "Invalid request");
    }

    @Test
    void handleGenericException_returns500WithInternalErrorCode() {
        Exception ex = new RuntimeException("something went wrong");

        ResponseEntity<Map<String, Object>> entity = handler.handleGenericException(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody()).containsEntry("code", "INTERNAL_ERROR");
    }

    @Test
    void handleMultipartException_returns413WithPayloadTooLargeCode() {
        MultipartException ex = new MultipartException("Request size exceeded maximum allowed size");

        ResponseEntity<Map<String, Object>> entity = handler.handleMultipartException(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody()).containsEntry("code", "PAYLOAD_TOO_LARGE");
        assertThat(entity.getBody()).containsEntry(
                "error", "Uploaded file exceeds the maximum allowed size."
        );
    }
}
