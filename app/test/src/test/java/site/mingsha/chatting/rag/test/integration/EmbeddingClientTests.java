package site.mingsha.chatting.rag.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;
import site.mingsha.chatting.rag.integration.config.EmbeddingProperties;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingClient Tests")
class EmbeddingClientTests {

    private static final String BASE_URL = "https://api.example.com";
    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "nomic-embed-text";

    @Mock
    private WebClient webClientMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock
    private WebClient.RequestBodySpec requestBodySpecMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock
    private WebClient.ResponseSpec responseSpecMock;

    private ObjectMapper objectMapper;
    private EmbeddingClient embeddingClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        EmbeddingProperties props = new EmbeddingProperties(BASE_URL, API_KEY, MODEL);
        embeddingClient = new EmbeddingClient(props, objectMapper);

        Field webClientField = EmbeddingClient.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(embeddingClient, webClientMock);
    }

    private void stubPost(String responseBody) {
        lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        lenient().when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        lenient().when(requestBodySpecMock.header(anyString(), anyString())).thenReturn(requestBodySpecMock);
        lenient().when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        lenient().when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        lenient().when(responseSpecMock.bodyToMono(String.class))
                .thenReturn(Mono.just(responseBody));
    }

    @Nested
    @DisplayName("embed()")
    class EmbedTests {

        @Test
        @DisplayName("should return float array of expected dimension for normal response")
        void normalCase() {
            stubPost("{\"embedding\":[0.1, -0.2, 0.3, 0.4]}");
            float[] embedding = embeddingClient.embed("Hello world");
            assertThat(embedding).hasSize(4);
            assertThat(embedding[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
            assertThat(embedding[1]).isCloseTo(-0.2f, org.assertj.core.data.Offset.offset(0.001f));
        }

        @Test
        @DisplayName("should return float array for single-dimensional embedding")
        void singleDimensional() {
            stubPost("{\"embedding\":[0.5]}");
            float[] embedding = embeddingClient.embed("x");
            assertThat(embedding).hasSize(1);
            assertThat(embedding[0]).isCloseTo(0.5f, org.assertj.core.data.Offset.offset(0.001f));
        }

        @Test
        @DisplayName("should throw RuntimeException when API response is not valid JSON")
        void badJson() {
            stubPost("not json {{{");
            assertThatThrownBy(() -> embeddingClient.embed("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 Embedding 向量失败");
        }

        @Test
        @DisplayName("should throw RuntimeException when embedding field is missing")
        void missingField() {
            stubPost("{\"other\":\"value\"}");
            assertThatThrownBy(() -> embeddingClient.embed("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 Embedding 向量失败");
        }

        @Test
        @DisplayName("should throw RuntimeException when embedding field is null")
        void nullField() {
            stubPost("{\"embedding\":null}");
            assertThatThrownBy(() -> embeddingClient.embed("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 Embedding 向量失败");
        }

        @Test
        @DisplayName("should throw RuntimeException when WebClient throws exception")
        void webClientError() {
            lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
            lenient().when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
            lenient().when(requestBodySpecMock.header(anyString(), anyString())).thenReturn(requestBodySpecMock);
            lenient().when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
            lenient().when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
            lenient().when(responseSpecMock.bodyToMono(String.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            assertThatThrownBy(() -> embeddingClient.embed("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 Embedding 向量失败");
        }
    }
}
