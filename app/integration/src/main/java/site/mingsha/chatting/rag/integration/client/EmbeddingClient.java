package site.mingsha.chatting.rag.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client for calling a text embedding service.
 *
 * <p>Sends text to a configurable embedding API endpoint and parses
 * the returned float vector. The supported API shape is:
 * {@code POST /api/embeddings → { "embedding": [...] }}</p>
 *
 * <p>The configured {@code baseUrl} should point to an OpenAI-compatible
 * embedding endpoint (e.g., an Ollama server).</p>
 */
@Slf4j
@Component
public class EmbeddingClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    /**
     * Constructs the embedding client.
     *
     * @param props        embedding configuration (baseUrl, model name)
     * @param objectMapper Jackson ObjectMapper for response parsing
     */
    public EmbeddingClient(site.mingsha.chatting.rag.integration.config.EmbeddingProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = props.model();
        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .build();
        log.info("[EmbeddingClient] 初始化完成，baseUrl={}, model={}", props.baseUrl(), model);
    }

    /**
     * Generates a dense vector embedding for the given text.
     *
     * <p>Sends a POST request to {@code /api/embeddings} with the model
     * name and text prompt. The response is expected to contain an
     * {@code "embedding"} array of numbers.</p>
     *
     * @param text the input text to embed
     * @return a float array representing the embedding vector
     * @throws RuntimeException if the API call fails or the response format is unexpected
     */
    public float[] embed(String text) {
        long start = System.currentTimeMillis();
        String preview = text.length() > 80 ? text.substring(0, 80) + "..." : text;
        log.info("[EmbeddingClient] 生成向量，文本预览=[{}], 长度={}", preview, text.length());

        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", text
            );

            log.debug("[EmbeddingClient] 请求体: {}", request);

            String response = webClient.post()
                    .uri("/api/embeddings")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            log.trace("[EmbeddingClient] 原始响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new RuntimeException("Embedding 返回的向量格式异常: " + response);
            }
            int dims = embeddingNode.size();
            float[] result = new float[dims];
            for (int i = 0; i < dims; i++) {
                double val = embeddingNode.get(i).asDouble();
                if (val > Float.MAX_VALUE) val = Float.MAX_VALUE;
                else if (val < -Float.MAX_VALUE) val = -Float.MAX_VALUE;
                result[i] = (float) val;
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("[EmbeddingClient] 向量生成完成，维度={}, 耗时={}ms", result.length, elapsed);
            log.debug("[EmbeddingClient] 向量前5维: {}",
                    java.util.Arrays.toString(java.util.Arrays.copyOf(result, Math.min(5, result.length))));
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[EmbeddingClient] 生成向量失败，耗时={}ms，错误: {}", elapsed, e.getMessage(), e);
            throw new RuntimeException("解析 Embedding 向量失败", e);
        }
    }
}
