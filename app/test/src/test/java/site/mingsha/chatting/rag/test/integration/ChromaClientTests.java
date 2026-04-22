package site.mingsha.chatting.rag.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.config.ChromaProperties;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChromaClient Tests")
class ChromaClientTests {

    private static final String SERVICE_URL = "http://localhost:8000";

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private ChromaClient chromaClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        ChromaProperties props = new ChromaProperties(SERVICE_URL);
        chromaClient = new ChromaClient(props, objectMapper);

        Field webClientField = ChromaClient.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(chromaClient, webClient);

        // POST chain: webClient.post() -> uri(...) -> contentType() -> bodyValue() -> retrieve() -> bodyToMono()
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        // bodyValue() returns RequestHeadersSpec<?> — bypass type check with doReturn
        lenient().doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        // retrieve() on wildcard RequestHeadersSpec — use doReturn
        lenient().doReturn(responseSpec).when(requestHeadersSpec).retrieve();

        // GET chain: webClient.get() -> uri(...) -> retrieve() -> bodyToMono()
        // All these return wildcard types — must use doReturn().when() to bypass type mismatch
        lenient().doReturn(requestHeadersUriSpec).when(webClient).get();
        lenient().doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(anyString());
        lenient().doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        lenient().doReturn(responseSpec).when(requestHeadersUriSpec).retrieve();
    }

    private void resetCollectionId() throws Exception {
        Field colIdField = ChromaClient.class.getDeclaredField("collectionId");
        colIdField.setAccessible(true);
        colIdField.set(chromaClient, null);
    }

    private void setCollectionId(String id) throws Exception {
        Field colIdField = ChromaClient.class.getDeclaredField("collectionId");
        colIdField.setAccessible(true);
        colIdField.set(chromaClient, id);
    }

    private void setGetCollectionIdViaReflection(String id) throws Exception {
        // getCollectionId is private, set it via reflection
        java.lang.reflect.Method setColMethod = ChromaClient.class.getDeclaredMethod("setCollectionIdInternal", String.class);
        setColMethod.setAccessible(true);
        try {
            setColMethod.invoke(chromaClient, id);
        } catch (Exception e) {
            // Fallback: set field directly
            Field colIdField = ChromaClient.class.getDeclaredField("collectionId");
            colIdField.setAccessible(true);
            colIdField.set(chromaClient, id);
        }
    }

    // --- addDocuments() ---

    @Nested
    @DisplayName("addDocuments()")
    class AddDocumentsTests {

        @Test
        @DisplayName("should send POST with JSON payload")
        void shouldSendCorrectPayload() throws Exception {
            setCollectionId("col-test-123");
            when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just("{\"success\":true}"));

            chromaClient.addDocuments(
                    List.of("chunk-1", "chunk-2"),
                    List.of("Hello world", "Foo bar baz"),
                    List.of(new float[]{0.1f, 0.2f, 0.3f}, new float[]{0.4f, 0.5f, 0.6f}),
                    List.of("{\"source\":\"doc-a\"}", "{\"source\":\"doc-b\"}")
            );

            verify(requestBodySpec).bodyValue(anyString());
            verify(responseSpec).bodyToMono(String.class);
        }

        @Test
        @DisplayName("should send POST with single document")
        void shouldSendSingleDocument() throws Exception {
            setCollectionId("col-single");
            when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just("{\"success\":true}"));

            chromaClient.addDocuments(
                    List.of("id-only"),
                    List.of("Single doc"),
                    List.of(new float[]{0.1f}),
                    List.of("{}")
            );

            verify(webClient).post();
        }
    }

    // --- query() ---

    @Nested
    @DisplayName("query()")
    class QueryTests {

        @Test
        @DisplayName("should parse ChromaDB response and return ChromaSearchResultVO list")
        void shouldParseResponseCorrectly() throws Exception {
            setCollectionId("col-query");
            String apiResponse = """
                {
                  "ids": [["chunk-a", "chunk-b"]],
                  "documents": [["doc A content", "doc B content"]],
                  "distances": [[0.1, 0.3]],
                  "metadatas": [[{"source":"s1"},{"source":"s2"}]],
                  "embeddings": [[[0.1,0.2,0.3],[0.4,0.5,0.6]]]
                }
                """;
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(apiResponse));

            List<ChromaSearchResultVO> results = chromaClient.query(new float[]{1.0f, 0.0f}, 5);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).id()).isEqualTo("chunk-a");
            assertThat(results.get(0).content()).isEqualTo("doc A content");
            assertThat(results.get(0).distance()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("should return empty list when ChromaDB returns empty arrays")
        void shouldReturnEmptyListForEmptyResponse() throws Exception {
            setCollectionId("col-empty");
            String emptyResponse = """
                {
                  "ids": [[]],
                  "documents": [[]],
                  "distances": [[]],
                  "metadatas": [[]],
                  "embeddings": [[]]
                }
                """;
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(emptyResponse));

            List<ChromaSearchResultVO> results = chromaClient.query(new float[]{0.1f}, 5);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should throw RuntimeException when ChromaDB returns non-2xx")
        void shouldThrowOnErrorResponse() throws Exception {
            setCollectionId("col-err");
            when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            assertThatThrownBy(() -> chromaClient.query(new float[]{0.1f}, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 ChromaDB query 响应失败");
        }
    }

    // --- getCollectionId via deleteAll (since it's private) ---

    @Nested
    @DisplayName("deleteAll()")
    class DeleteAllTests {

        @Test
        @SuppressWarnings("unchecked")
        void shouldDeleteAndClearCollectionId() throws Exception {
            setCollectionId("to-be-deleted");
            when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just("{\"success\":true}"));

            chromaClient.deleteAll();

            verify(webClient).delete();
        }
    }

    // --- computeCosineSimilarity ---

    @Nested
    @DisplayName("computeCosineSimilarity()")
    class ComputeCosineSimilarityTests {

        @Test
        @DisplayName("should return 1.0 for identical vectors")
        void identicalVectors() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            JsonNode vec = objectMapper.readTree("[1.0, 0.0, 0.0]");
            double sim = (double) method.invoke(chromaClient, new float[]{1.0f, 0.0f, 0.0f}, vec);
            assertThat(sim).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 for orthogonal vectors")
        void orthogonalVectors() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            JsonNode vec = objectMapper.readTree("[0.0, 1.0, 0.0]");
            double sim = (double) method.invoke(chromaClient, new float[]{1.0f, 0.0f, 0.0f}, vec);
            assertThat(sim).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 when first vector is zero")
        void zeroFirstVector() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            JsonNode vec = objectMapper.readTree("[1.0, 0.0]");
            double sim = (double) method.invoke(chromaClient, new float[]{0.0f, 0.0f}, vec);
            assertThat(sim).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 when second vector is zero")
        void zeroSecondVector() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            JsonNode vec = objectMapper.readTree("[0.0, 0.0]");
            double sim = (double) method.invoke(chromaClient, new float[]{1.0f, 0.0f}, vec);
            assertThat(sim).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should compute cosine similarity correctly for non-trivial vectors")
        void nonTrivialVectors() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            // a = [1, 2], b = [2, 1]; dot=4, |a|=sqrt(5), |b|=sqrt(5); cos=0.8
            JsonNode vec = objectMapper.readTree("[2.0, 1.0]");
            double sim = (double) method.invoke(chromaClient, new float[]{1.0f, 2.0f}, vec);
            assertThat(sim).isCloseTo(0.8, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should compute cosine similarity correctly for 3D vectors")
        void threeDimensionalVectors() throws Exception {
            var method = ChromaClient.class.getDeclaredMethod("computeCosineSimilarity", float[].class, JsonNode.class);
            method.setAccessible(true);
            // a = [1, 0, 0], b = [0.5, 0.5, 0.7071]
            // dot = 0.5, |a| = 1, |b| = sqrt(1) = 1; cos = 0.5
            JsonNode vec = objectMapper.readTree("[0.5, 0.5, 0.7071]");
            double sim = (double) method.invoke(chromaClient, new float[]{1.0f, 0.0f, 0.0f}, vec);
            assertThat(sim).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
        }
    }
}
