package site.mingsha.chatting.rag.integration.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * Spring AI-based LLM client wrapping OpenAI-compatible or Anthropic API.
 *
 * <p>Replaces {@link LlmClient} which uses manual WebClient SSE parsing.
 * This implementation uses Spring AI's {@link ChatClient} for both streaming
 * and non-streaming interactions.</p>
 *
 * <p>The method signature ({@code Consumer<String> onChunk, Runnable onComplete} callbacks)
 * is intentionally kept the same as the original {@link LlmClient} to avoid
 * breaking the caller ({@link site.mingsha.chatting.rag.biz.service.LlmService}).</p>
 */
@Slf4j
@Component
public class SpringAiLlmClient {

    private final ChatClient chatClient;

    /**
     * Constructs the Spring AI LLM client.
     *
     * @param chatClient Spring AI ChatClient (created from OpenAiChatModel)
     */
    public SpringAiLlmClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        log.info("[SpringAiLlmClient] 初始化完成");
    }

    /**
     * Streaming chat with callbacks. Each text chunk triggers onChunk.
     * When the stream ends, onComplete is called.
     *
     * <p>Translates Spring AI's {@code Flux<String>} (from {@link StreamResponseSpec#content()})
     * into the callback-based interface expected by the caller.</p>
     */
    public void chatStreamWithDone(String systemPrompt, String userMessage,
                                   Consumer<String> onChunk, Runnable onComplete) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiLlmClient] 流式对话开始，systemPrompt长度={}, userMessage=[{}]",
                systemPrompt.length(), userMessage);

        Flux<String> contentFlux = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content();

        contentFlux.subscribe(
                (String chunk) -> {
                    log.trace("[SpringAiLlmClient] 流式片段: [{}]", chunk);
                    onChunk.accept(chunk);
                },
                (Throwable error) -> {
                    long elapsed = System.currentTimeMillis() - start;
                    log.error("[SpringAiLlmClient] 流式对话异常，耗时={}ms，错误: {}",
                            elapsed, error.getMessage(), error);
                    onChunk.accept("[DONE]");
                },
                () -> {
                    long elapsed = System.currentTimeMillis() - start;
                    log.info("[SpringAiLlmClient] 流式对话完成，耗时={}ms", elapsed);
                    onComplete.run();
                }
        );
    }

    /**
     * Blocking (non-streaming) chat. Returns the complete response.
     */
    public String chat(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        log.info("[SpringAiLlmClient] 非流式对话开始");

        try {
            CallResponseSpec responseSpec = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call();

            String response = responseSpec.content();

            long elapsed = System.currentTimeMillis() - start;
            log.info("[SpringAiLlmClient] 非流式对话完成，回复长度={}, 耗时={}ms",
                    response != null ? response.length() : 0, elapsed);
            return response != null ? response : "";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[SpringAiLlmClient] 非流式对话失败，耗时={}ms，错误: {}",
                    elapsed, e.getMessage(), e);
            throw new RuntimeException("LLM 调用失败", e);
        }
    }
}
