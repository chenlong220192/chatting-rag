package site.mingsha.chatting.rag.integration.config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring AI configuration for LLM and Embedding.
 *
 * <p>LLM provider is selected via {@code spring.ai.provider}:
 * <ul>
 *   <li>{@code openai-compatible} — OpenAI-compatible API (base-url + api-key)</li>
 *   <li>{@code anthropic} — Anthropic API (api-key only, defaults to api.anthropic.com)</li>
 * </ul>
 *
 * <p>Embedding uses Ollama locally (fixed, not swappable via config).</p>
 *
 * <p>Auto-configuration starters are excluded via {@code @SpringBootApplication};
 * all beans are defined explicitly here for predictability.</p>
 */
@Slf4j
@Configuration
public class SpringAiConfig {

    @Value("${llm.base-url:#{null}}")
    private String llmBaseUrl;

    @Value("${llm.api-key}")
    private String llmApiKey;

    @Value("${llm.model}")
    private String llmModel;

    @Value("${embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${embedding.model}")
    private String embeddingModel;

    // ─────────────────────────────────────────────────────────────────────────
    // OpenAI-compatible LLM
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public OpenAiApi openAiApi() {
        long start = System.currentTimeMillis();
        String baseUrl = llmBaseUrl != null ? llmBaseUrl : "https://api.openai.com";
        log.info("[SpringAiConfig] 初始化 OpenAI-compatible API，baseUrl={}", baseUrl);
        log.debug("[SpringAiConfig] OpenAI API 配置，apiKey长度={}", llmApiKey.length());

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(llmApiKey)
                .build();

        log.info("[SpringAiConfig] OpenAI-compatible API 初始化完成，耗时={}ms", System.currentTimeMillis() - start);
        return api;
    }

    @Bean
    public OpenAiChatModel openAiCompatibleChatModel(OpenAiApi openAiApi) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiConfig] 初始化 OpenAI-compatible ChatModel，model={}", llmModel);

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(llmModel)
                        .build())
                .build();

        log.info("[SpringAiConfig] OpenAI-compatible ChatModel 初始化完成，model={}, 耗时={}ms",
                llmModel, System.currentTimeMillis() - start);
        return model;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Anthropic LLM
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public com.anthropic.client.AnthropicClient anthropicClient() {
        long start = System.currentTimeMillis();
        log.info("[SpringAiConfig] 初始化 Anthropic HTTP 客户端");
        log.debug("[SpringAiConfig] Anthropic 客户端配置，apiKey长度={}", llmApiKey.length());

        com.anthropic.client.AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(llmApiKey)
                .build();

        log.info("[SpringAiConfig] Anthropic HTTP 客户端初始化完成，baseUrl=https://api.anthropic.com, 耗时={}ms",
                System.currentTimeMillis() - start);
        return client;
    }

    @Bean
    public org.springframework.ai.anthropic.AnthropicChatModel anthropicChatModel(
            com.anthropic.client.AnthropicClient anthropicClient) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiConfig] 初始化 Anthropic ChatModel，model={}", llmModel);

        org.springframework.ai.anthropic.AnthropicChatModel model =
                org.springframework.ai.anthropic.AnthropicChatModel.builder()
                        .anthropicClient(anthropicClient)
                        .options(org.springframework.ai.anthropic.AnthropicChatOptions.builder()
                                .model(llmModel)
                                .build())
                        .build();

        log.info("[SpringAiConfig] Anthropic ChatModel 初始化完成，model={}, 耗时={}ms",
                llmModel, System.currentTimeMillis() - start);
        return model;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unified ChatClient — resolves whichever ChatModel is configured
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public ChatClient chatClient(
            @Autowired(required = false) OpenAiChatModel openAi,
            @Autowired(required = false) org.springframework.ai.anthropic.AnthropicChatModel anthropic) {
        if (openAi != null) {
            log.info("[SpringAiConfig] ChatClient 绑定至 OpenAI-compatible，model={}", llmModel);
            return ChatClient.create(openAi);
        }
        if (anthropic != null) {
            log.info("[SpringAiConfig] ChatClient 绑定至 Anthropic，model={}", llmModel);
            return ChatClient.create(anthropic);
        }
        log.error("[SpringAiConfig] 未检测到任何 LLM Provider，请检查 spring.ai.provider 配置");
        throw new IllegalStateException(
                "No LLM provider configured. Set spring.ai.provider to 'openai-compatible' or 'anthropic'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Embedding (Ollama — local, not swappable)
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public WebClient.Builder webClientBuilder() {
        log.debug("[SpringAiConfig] 初始化 WebClient.Builder（用于 Ollama）");
        return WebClient.builder();
    }

    @Bean
    public OllamaApi ollamaApi(WebClient.Builder webClientBuilder) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiConfig] 初始化 Ollama API 客户端，baseUrl={}", embeddingBaseUrl);

        OllamaApi api = OllamaApi.builder()
                .baseUrl(embeddingBaseUrl)
                .webClientBuilder(webClientBuilder)
                .build();

        log.info("[SpringAiConfig] Ollama API 客户端初始化完成，baseUrl={}, 耗时={}ms",
                embeddingBaseUrl, System.currentTimeMillis() - start);
        return api;
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiConfig] 初始化 Ollama EmbeddingModel，model={}", embeddingModel);

        OllamaEmbeddingModel model = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        org.springframework.ai.ollama.api.OllamaEmbeddingOptions.builder()
                                .model(embeddingModel)
                                .build())
                .build();

        log.info("[SpringAiConfig] Ollama EmbeddingModel 初始化完成，model={}, 耗时={}ms",
                embeddingModel, System.currentTimeMillis() - start);
        return model;
    }

    @Bean
    public EmbeddingModel embeddingModel(OllamaEmbeddingModel ollamaEmbeddingModel) {
        log.info("[SpringAiConfig] EmbeddingModel Bean 注册完成（委托至 OllamaEmbeddingModel）");
        return ollamaEmbeddingModel;
    }
}
