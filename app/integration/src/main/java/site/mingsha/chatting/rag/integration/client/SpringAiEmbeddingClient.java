package site.mingsha.chatting.rag.integration.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Spring AI-based Embedding client using Ollama.
 *
 * <p>Replaces {@link EmbeddingClient} which uses manual WebClient.
 * The return type ({@code float[]}) is kept the same to avoid
 * breaking the caller ({@link site.mingsha.chatting.rag.biz.service.EmbeddingService}).</p>
 */
@Slf4j
@Component
public class SpringAiEmbeddingClient {

    private final EmbeddingModel embeddingModel;

    /**
     * Constructs the Spring AI embedding client.
     *
     * @param embeddingModel the Spring AI embedding model (Ollama-backed)
     */
    public SpringAiEmbeddingClient(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        log.info("[SpringAiEmbeddingClient] 初始化完成");
    }

    /**
     * Embeds a single text string and returns the vector as float[].
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    public float[] embed(String text) {
        long start = System.currentTimeMillis();
        try {
            float[] result = embeddingModel.embed(text);

            long elapsed = System.currentTimeMillis() - start;
            log.debug("[SpringAiEmbeddingClient] embed 完成，向量维度={}, 耗时={}ms",
                    result.length, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[SpringAiEmbeddingClient] embed 失败，耗时={}ms，错误: {}",
                    elapsed, e.getMessage(), e);
            throw new RuntimeException("Embedding 调用失败", e);
        }
    }
}
