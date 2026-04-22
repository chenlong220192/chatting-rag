package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;

/**
 * Business-level service for text embedding operations.
 *
 * <p>Provides a thin facade over {@link EmbeddingClient}, exposing
 * the single {@code embed(text)} operation used throughout the RAG pipeline.</p>
 *
 * @see EmbeddingClient
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;

    /**
     * Constructs the service with the underlying embedding client.
     *
     * @param embeddingClient the embedding HTTP client
     */
    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    /**
     * Converts a text string into a dense vector representation.
     *
     * @param text the input text to embed
     * @return a float array representing the embedding vector
     */
    public float[] embed(String text) {
        return embeddingClient.embed(text);
    }
}
