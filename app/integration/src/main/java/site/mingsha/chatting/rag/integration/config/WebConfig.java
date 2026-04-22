package site.mingsha.chatting.rag.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring web configuration for the integration module.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>A shared {@link ObjectMapper} bean for JSON serialization</li>
 *   <li>A CORS filter allowing explicit origins with credentials support</li>
 * </ul>
 *
 * @see ObjectMapper
 * @see CorsWebFilter
 */
@Configuration
@EnableConfigurationProperties(WebProperties.class)
public class WebConfig {

    /**
     * Creates a shared Jackson {@link ObjectMapper} for JSON serialization
     * and deserialization across the integration module.
     *
     * @return a new ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Creates a CORS filter that permits cross-origin requests from explicitly
     * configured origins, supporting all standard HTTP methods with credentials enabled.
     *
     * <p>Unlike {@code allowedOriginPatterns("*")}, this uses explicit origin allowlist
     * entries read from the {@code web.cors.allowed-origins} configuration property.
     * This complies with the CORS spec, which forbids {@code Access-Control-Allow-Origin: *}
     * when {@code Access-Control-Allow-Credentials: true} is sent.</p>
     *
     * @param webProperties application properties containing the allowed origins list
     * @return a {@link CorsWebFilter} instance covering all paths
     */
    @Bean
    public CorsWebFilter corsWebFilter(WebProperties webProperties) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> allowedOrigins = webProperties.cors() != null
                ? webProperties.cors().allowedOrigins()
                : List.of();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type", "Authorization", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(List.of("Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
