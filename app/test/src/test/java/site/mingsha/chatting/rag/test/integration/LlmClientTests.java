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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import site.mingsha.chatting.rag.integration.client.LlmClient;
import site.mingsha.chatting.rag.integration.config.LlmProperties;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmClient Tests")
class LlmClientTests {

    private static final String BASE_URL = "https://api.example.com";
    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "gpt-4o";

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
    private LlmClient llmClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        LlmProperties props = new LlmProperties("openai", BASE_URL, API_KEY, MODEL, 4096, 128000);
        llmClient = new LlmClient(props, objectMapper);

        Field webClientField = LlmClient.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(llmClient, webClientMock);
    }

    private void stubNonStreaming(String responseBody) {
        lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        lenient().when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        lenient().when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        lenient().when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        lenient().when(responseSpecMock.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    private void stubStreaming(Flux<byte[]> flux) {
        lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        lenient().when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        lenient().when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        lenient().when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        lenient().when(responseSpecMock.bodyToFlux(byte[].class)).thenReturn(flux);
    }

    @Nested
    @DisplayName("chat() — non-streaming")
    class ChatNonStreamingTests {

        @Test
        @DisplayName("should return full response string from choices")
        void shouldReturnFullResponse() {
            stubNonStreaming("""
                {"choices":[{"message":{"content":"Hello world, this is the assistant response."}}]}
                """);
            String result = llmClient.chat("You are a helpful assistant.", "Say hello");
            assertThat(result).isEqualTo("Hello world, this is the assistant response.");
        }

        @Test
        @DisplayName("should return empty string when choices array is empty")
        void shouldReturnEmptyStringForEmptyChoices() {
            stubNonStreaming("{\"choices\":[]}");
            String result = llmClient.chat("System prompt", "User message");
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should return empty string when message content is null")
        void shouldReturnEmptyStringForNullContent() {
            stubNonStreaming("{\"choices\":[{\"message\":{\"content\":null}}]}");
            String result = llmClient.chat("System", "User");
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should throw RuntimeException when WebClient throws exception")
        void shouldThrowRuntimeExceptionOnError() {
            lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
            lenient().when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
            lenient().when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
            lenient().when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
            lenient().when(responseSpecMock.bodyToMono(String.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            assertThatThrownBy(() -> llmClient.chat("system", "user"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解析 LLM 响应失败");
        }

        @Test
        @DisplayName("should return content with newlines and special characters")
        void shouldReturnContentWithSpecialCharacters() {
            stubNonStreaming("{\"choices\":[{\"message\":{\"content\":\"Line 1\\nLine 2\\n\\\"Quoted\\\"\"}}]}");
            String result = llmClient.chat("system", "user");
            assertThat(result).contains("Line 1");
            assertThat(result).contains("Line 2");
        }
    }

    @Nested
    @DisplayName("chatStreamWithDone() — streaming")
    class ChatStreamTests {

        @Test
        @DisplayName("StepVerifier confirms Flux emits correct chunks then completes")
        void verifyFluxEmitsChunksAndCompletes() {
            byte[] chunk1 = "data:{\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] chunk2 = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(chunk1, chunk2));

            AtomicReference<String> lastChunk = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);

            llmClient.chatStreamWithDone(
                    "system prompt", "user message",
                    lastChunk::set, () -> completed.set(true)
            );

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            assertThat(lastChunk.get()).isEqualTo("[DONE]");
            assertThat(completed.get()).isTrue();
        }

        @Test
        @DisplayName("StepVerifier confirms multiple chunks are emitted before [DONE]")
        void verifyMultipleChunksEmitted() throws Exception {
            byte[] chunk1 = "data:{\"choices\":[{\"delta\":{\"content\":\"First part\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] chunk2 = "data:{\"choices\":[{\"delta\":{\"content\":\"Second part\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] chunk3 = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(chunk1, chunk2, chunk3));

            List<String> receivedChunks = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            llmClient.chatStreamWithDone(
                    "system", "user",
                    chunk -> { synchronized (receivedChunks) { receivedChunks.add(chunk); } },
                    latch::countDown
            );

            boolean waited = latch.await(2, TimeUnit.SECONDS);
            assertThat(waited).isTrue();
            synchronized (receivedChunks) {
                assertThat(receivedChunks).containsExactly("First part", "Second part", "[DONE]");
            }
        }

        @Test
        @DisplayName("[DONE] event triggers onComplete and sends [DONE] to onChunk")
        void doneEventTriggersOnComplete() throws Exception {
            byte[] chunk = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(chunk));

            AtomicBoolean completeCalled = new AtomicBoolean(false);
            AtomicReference<String> lastChunk = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            llmClient.chatStreamWithDone(
                    "system", "user",
                    lastChunk::set,
                    () -> { completeCalled.set(true); latch.countDown(); }
            );

            boolean waited = latch.await(2, TimeUnit.SECONDS);
            assertThat(waited).isTrue();
            assertThat(lastChunk.get()).isEqualTo("[DONE]");
            assertThat(completeCalled.get()).isTrue();
        }

        @Test
        @DisplayName("parse error on a line is skipped and stream continues")
        void parseErrorSkippedAndStreamContinues() throws Exception {
            byte[] badChunk = "data:{bad json".getBytes(StandardCharsets.UTF_8);
            byte[] goodChunk = "data:{\"choices\":[{\"delta\":{\"content\":\"valid content\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] doneChunk = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(badChunk, goodChunk, doneChunk));

            List<String> receivedChunks = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            llmClient.chatStreamWithDone(
                    "system", "user",
                    chunk -> { synchronized (receivedChunks) { receivedChunks.add(chunk); } },
                    latch::countDown
            );

            boolean waited = latch.await(2, TimeUnit.SECONDS);
            assertThat(waited).isTrue();
            synchronized (receivedChunks) {
                assertThat(receivedChunks).doesNotContain("bad json");
                assertThat(receivedChunks).contains("valid content", "[DONE]");
            }
        }

        @Test
        @DisplayName("empty content delta does not emit a chunk")
        void emptyContentDeltaDoesNotEmit() throws Exception {
            byte[] emptyContent = "data:{\"choices\":[{\"delta\":{\"content\":\"\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] realContent = "data:{\"choices\":[{\"delta\":{\"content\":\"real\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] done = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(emptyContent, realContent, done));

            List<String> receivedChunks = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            llmClient.chatStreamWithDone(
                    "system", "user",
                    chunk -> { synchronized (receivedChunks) { receivedChunks.add(chunk); } },
                    latch::countDown
            );

            boolean waited = latch.await(2, TimeUnit.SECONDS);
            assertThat(waited).isTrue();
            synchronized (receivedChunks) {
                assertThat(receivedChunks).doesNotContain("");
                assertThat(receivedChunks).containsExactly("real", "[DONE]");
            }
        }

        @Test
        @DisplayName("lines without data: prefix are filtered out")
        void nonDataLinesFiltered() throws Exception {
            byte[] nonDataLine = "this is not an SSE line".getBytes(StandardCharsets.UTF_8);
            byte[] dataLine = "data:{\"choices\":[{\"delta\":{\"content\":\"data line\"}}]}".getBytes(StandardCharsets.UTF_8);
            byte[] done = "data:[DONE]".getBytes(StandardCharsets.UTF_8);
            stubStreaming(Flux.just(nonDataLine, dataLine, done));

            List<String> receivedChunks = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            llmClient.chatStreamWithDone(
                    "system", "user",
                    chunk -> { synchronized (receivedChunks) { receivedChunks.add(chunk); } },
                    latch::countDown
            );

            boolean waited = latch.await(2, TimeUnit.SECONDS);
            assertThat(waited).isTrue();
            synchronized (receivedChunks) {
                assertThat(receivedChunks).doesNotContain("this is not an SSE line");
                assertThat(receivedChunks).containsExactly("data line", "[DONE]");
            }
        }
    }
}
