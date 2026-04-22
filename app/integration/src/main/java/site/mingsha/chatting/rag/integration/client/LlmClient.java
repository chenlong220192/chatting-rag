package site.mingsha.chatting.rag.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP client for interacting with an OpenAI-compatible LLM API.
 *
 * <p>Supports both streaming (SSE) and blocking chat completion requests
 * via the {@code /v1/chat/completions} endpoint. Authentication uses
 * the {@code Authorization: Bearer <apiKey>} header.</p>
 *
 * <p>Streaming responses are parsed line-by-line from SSE data frames,
 * extracting {@code delta.content} fields from each Server-Sent Event.</p>
 */
@Slf4j
@Component
public class LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;

    /**
     * Constructs the LLM client with the given configuration.
     *
     * @param props        LLM configuration (baseUrl, apiKey, model, maxTokens)
     * @param objectMapper Jackson ObjectMapper for JSON parsing
     */
    public LlmClient(site.mingsha.chatting.rag.integration.config.LlmProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = props.model();
        this.maxTokens = props.maxTokens();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(java.time.Duration.ofMinutes(5));

        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        log.info("[LlmClient] 初始化完成，baseUrl={}, model={}", props.baseUrl(), model);
    }

    /**
     * Initiates a streaming chat completion session.
     *
     * <p>Sends a POST to {@code /v1/chat/completions} with {@code stream: true}.
     * Each SSE data line is parsed to extract {@code delta.content}.
     * The callbacks are invoked asynchronously on the reactive thread.</p>
     *
     * @param systemPrompt the system prompt (may include RAG context)
     * @param userMessage  the user's message
     * @param onChunk     called for each text fragment received
     * @param onComplete  called when the stream finishes or completes
     */
    public void chatStreamWithDone(String systemPrompt, String userMessage,
                                   Consumer<String> onChunk, Runnable onComplete) {
        long start = System.currentTimeMillis();
        String sysPreview = systemPrompt.length() > 60 ? systemPrompt.substring(0, 60) + "..." : systemPrompt;
        log.info("[LlmClient] 流式对话开始，systemPrompt预览=[{}], userMessage=[{}]",
                sysPreview, userMessage);

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", maxTokens,
                "stream", true
        );

        log.debug("[LlmClient] 请求体: {}", request);

        webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(byte[].class)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(text -> Flux.fromArray(text.split("\n")))
                .filter(line -> !line.isBlank())
                .subscribe(
                        line -> {
                            log.trace("[LlmClient] 收到原始行: {}", line);
                            if (line.startsWith("data:")) {
                                String json = line.substring(5).trim();
                                if ("[DONE]".equals(json)) {
                                    long elapsed = System.currentTimeMillis() - start;
                                    log.info("[LlmClient] 流式对话结束，收到 [DONE]，总耗时={}ms", elapsed);
                                    onChunk.accept("[DONE]");
                                    return;
                                }
                                try {
                                    JsonNode root = objectMapper.readTree(json);
                                    String content = root.path("choices").path(0).path("delta").path("content").asText("");
                                    if (!content.isEmpty()) {
                                        log.trace("[LlmClient] 流式片段: [{}]", content);
                                        onChunk.accept(content);
                                    }
                                } catch (Exception e) {
                                    log.warn("[LlmClient] 解析 SSE 行失败: {}", e.getMessage());
                                }
                            }
                        },
                        err -> log.error("[LlmClient] 流式对话异常: {}", err.getMessage(), err),
                        () -> {
                            long elapsed = System.currentTimeMillis() - start;
                            log.info("[LlmClient] 流式订阅完成，耗时={}ms", elapsed);
                            onComplete.run();
                        }
                );
    }

    /**
     * Sends a blocking (non-streaming) chat completion request.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user's message
     * @return the complete assistant reply as a string
     * @throws RuntimeException if the API call or response parsing fails
     */
    public String chat(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        log.info("[LlmClient] 非流式对话开始，systemPrompt长度={}, userMessage=[{}]",
                systemPrompt.length(), userMessage);

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", maxTokens,
                "stream", false
        );

        log.debug("[LlmClient] 请求体: {}", request);

        try {
            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.trace("[LlmClient] 原始响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                String answer = choicesNode.get(0).path("message").path("content").asText("");
                long elapsed = System.currentTimeMillis() - start;
                log.info("[LlmClient] 非流式对话完成，回复长度={}, 耗时={}ms", answer.length(), elapsed);
                log.debug("[LlmClient] 回复内容预览: [{}]",
                        answer.length() > 100 ? answer.substring(0, 100) + "..." : answer);
                return answer;
            }
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[LlmClient] 非流式对话完成但无 choices，耗时={}ms", elapsed);
            return "";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[LlmClient] 非流式对话失败，耗时={}ms，错误: {}", elapsed, e.getMessage(), e);
            throw new RuntimeException("解析 LLM 响应失败", e);
        }
    }
}
