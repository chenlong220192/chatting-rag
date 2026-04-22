package site.mingsha.chatting.rag.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ChromaDB vector database configuration properties.
 *
 * <p>Bound to the {@code chroma.*} prefix in Spring Profile YAML files.</p>
 *
 * <p>Example YAML:</p>
 * <pre>
 * chroma:
 *   service-url: http://localhost:8000
 * </pre>
 *
 * @see site.mingsha.chatting.rag.integration.client.ChromaClient
 */
@ConfigurationProperties(prefix = "chroma")
public record ChromaProperties(
        /** Base URL of the ChromaDB REST API server. */
        String serviceUrl
) {}
