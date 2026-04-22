package site.mingsha.chatting.rag.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG pipeline configuration properties.
 *
 * <p>Bound to the {@code rag.*} prefix in Spring Profile YAML files.</p>
 *
 * <p>Example YAML:</p>
 * <pre>
 * rag:
 *   top-k: 5
 *   min-score: 0.5
 *   chunk:
 *     size: 512
 *     overlap: 128
 * </pre>
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        /** Maximum number of document chunks to retrieve per query. */
        int topK,
        /** Minimum cosine similarity score threshold for result filtering. */
        double minScore,
        /** Document chunking configuration. */
        ChunkConfig chunk
) {
    /**
     * Document chunking parameters controlling how text is split
     * before embedding and indexing.
     *
     * @param size    maximum number of characters per chunk
     * @param overlap number of overlapping characters between consecutive chunks
     */
    public record ChunkConfig(
            /** Maximum character count per chunk. */
            int size,
            /** Character overlap between adjacent chunks to preserve context. */
            int overlap
    ) {}
}
