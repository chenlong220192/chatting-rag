package site.mingsha.chatting.rag.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Web-layer configuration properties.
 *
 * <p>Bound to the {@code web.*} prefix in Spring Profile YAML files.</p>
 *
 * <p>Example YAML:</p>
 * <pre>
 * web:
 *   cors:
 *     allowed-origins:
 *       - http://localhost:3000
 *       - http://127.0.0.1:3000
 * </pre>
 */
@ConfigurationProperties(prefix = "web")
public record WebProperties(
        /** CORS configuration. */
        CorsProperties cors
) {
    /**
     * CORS-specific configuration options.
     *
     * @param allowedOrigins List of allowed origin patterns (e.g. http://localhost:*).
     *                        Must not contain a wildcard when credentials are enabled.
     */
    public record CorsProperties(
            List<String> allowedOrigins
    ) {}
}
