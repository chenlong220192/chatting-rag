package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.mingsha.chatting.rag.integration.client.SpringAiEmbeddingClient;

/**
 * Business-level service for text embedding operations.
 *
 * <p>Provides a thin facade over {@link SpringAiEmbeddingClient}, exposing
 * the single {@code embed(text)} operation used throughout the RAG pipeline.</p>
 *
 * @see SpringAiEmbeddingClient
 */
@Slf4j
@Service
public class EmbeddingService {

    private final SpringAiEmbeddingClient springAiEmbeddingClient;

    /**
     * Constructs the service with the underlying Spring AI embedding client.
     *
     * @param springAiEmbeddingClient the Spring AI embedding client
     */
    public EmbeddingService(SpringAiEmbeddingClient springAiEmbeddingClient) {
        this.springAiEmbeddingClient = springAiEmbeddingClient;
    }

    /**
     * Converts a text string into a dense vector representation.
     *
     * @param text the input text to embed
     * @return a float array representing the embedding vector
     */
    public float[] embed(String text) {
        return springAiEmbeddingClient.embed(text);
    }
}
