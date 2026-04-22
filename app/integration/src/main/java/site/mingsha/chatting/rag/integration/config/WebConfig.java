package site.mingsha.chatting.rag.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 *   <li>A CORS filter allowing all origins with credentials support</li>
 * </ul>
 *
 * @see ObjectMapper
 * @see CorsWebFilter
 */
@Configuration
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
     * Creates a CORS filter that permits cross-origin requests from any origin,
     * supporting all standard HTTP methods and headers with credentials enabled.
     *
     * @return a {@link CorsWebFilter} instance covering all paths
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
