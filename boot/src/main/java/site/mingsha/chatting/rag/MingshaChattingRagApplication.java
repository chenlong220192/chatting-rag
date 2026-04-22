package site.mingsha.chatting.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RAG Chat System application entry point.
 *
 * <p>Bootstraps the Spring Boot application with component scanning and
 * configuration properties binding enabled.</p>
 *
 * @see <a href="https://spring.io/projects/spring-boot">Spring Boot</a>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MingshaChattingRagApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String... args) {
        SpringApplication.run(MingshaChattingRagApplication.class, args);
    }
}
