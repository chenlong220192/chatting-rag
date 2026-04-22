package site.mingsha.chatting.rag.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Text embedding service configuration properties.
 *
 * <p>Bound to the {@code embedding.*} prefix in Spring Profile YAML files.</p>
 *
 * <p>Example YAML:</p>
 * <pre>
 * embedding:
 *   provider: ollama
 *   base-url: http://localhost:11434
 *   model: nomic-embed-text
 * </pre>
 *
 * @see site.mingsha.chatting.rag.integration.client.EmbeddingClient
 */
@ConfigurationProperties(prefix = "embedding")
public record EmbeddingProperties(
        /** Embedding provider identifier. */
        String provider,
        /** HTTP base URL of the embedding API (e.g., Ollama server). */
        String baseUrl,
        /** Embedding model name. */
        String model
) {}
