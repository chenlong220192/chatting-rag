package site.mingsha.chatting.rag.integration.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI configuration for LLM (MiniMax) and Embedding (Ollama).
 *
 * <p>This configuration replaces the manual WebClient setup in {@link LlmClient}
 * and {@link EmbeddingClient}. It creates Spring AI model beans directly
 * using the MiniMax and Ollama API classes.</p>
 *
 * <p>The {@code spring-ai-*} starters are included as dependencies to bring in
 * the required model API jars. Auto-configuration is bypassed in favor of
 * explicit bean definitions for predictability.</p>
 */
@Configuration
public class SpringAiConfig {

    @Value("${llm.base-url}")
    private String llmBaseUrl;

    @Value("${llm.api-key}")
    private String llmApiKey;

    @Value("${llm.model}")
    private String llmModel;

    @Value("${embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${embedding.model}")
    private String embeddingModel;

    // --- MiniMax LLM ---

    /**
     * MiniMax API client for LLM chat.
     */
    @Bean
    public MiniMaxApi miniMaxApi() {
        return new MiniMaxApi(llmBaseUrl, llmApiKey);
    }

    /**
     * MiniMax ChatModel for LLM (MiniMax).
     */
    @Bean
    @Primary
    public MiniMaxChatModel miniMaxChatModel(MiniMaxApi miniMaxApi) {
        return new MiniMaxChatModel(miniMaxApi,
                org.springframework.ai.minimax.MiniMaxChatOptions.builder()
                        .model(llmModel)
                        .build());
    }

    /**
     * ChatClient wrapping the MiniMax chat model.
     */
    @Bean
    public ChatClient chatClient(ChatModel miniMaxChatModel) {
        return ChatClient.create(miniMaxChatModel);
    }

    // --- Ollama Embedding ---

    /**
     * Ollama API client for Embedding.
     */
    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi(embeddingBaseUrl);
    }

    /**
     * Ollama EmbeddingModel for vector generation.
     */
    @Bean
    @Primary
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(org.springframework.ai.ollama.api.OllamaOptions.builder()
                        .model(embeddingModel)
                        .build())
                .build();
    }

    /**
     * Generic EmbeddingModel alias — points to the Ollama implementation.
     * Callers (e.g. {@link site.mingsha.chatting.rag.integration.client.SpringAiEmbeddingClient})
     * can inject this as {@code EmbeddingModel} without coupling to Ollama.
     */
    @Bean
    public EmbeddingModel embeddingModel(OllamaEmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }
}
