package site.mingsha.chatting.rag.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM service configuration properties.
 *
 * <p>Bound to the {@code llm.*} prefix in Spring Profile YAML files.
 * Values are injected via environment variables at runtime.</p>
 *
 * <p>Example YAML:</p>
 * <pre>
 * llm:
 *   provider: openai
 *   base-url: https://api.openai.com
 *   api-key: ${LLM_API_KEY}
 *   model: gpt-4o
 *   max-tokens: 4096
 *   context-limit: 128000
 * </pre>
 *
 * @see site.mingsha.chatting.rag.integration.client.LlmClient
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        /** LLM provider identifier (openai-compatible or anthropic). */
        String provider,
        /** HTTP base URL of the LLM API endpoint. */
        String baseUrl,
        /** API key for authentication (Bearer token). */
        String apiKey,
        /** Model identifier (e.g., gpt-4o, claude-3-5-sonnet). */
        String model,
        /** Maximum number of tokens in a single response. */
        int maxTokens,
        /** Context window size of the model (for frontend progress display). */
        int contextLimit
) {}
