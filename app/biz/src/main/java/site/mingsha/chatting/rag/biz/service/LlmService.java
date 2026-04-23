package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.mingsha.chatting.rag.integration.client.LlmClient;
import site.mingsha.chatting.rag.integration.client.SpringAiLlmClient;

/**
 * Business-level service for LLM chat operations.
 *
 * <p>Delegates to {@link SpringAiLlmClient} for both streaming and blocking
 * chat interactions with the configured LLM provider.</p>
 *
 * @see SpringAiLlmClient
 */
@Slf4j
@Service
public class LlmService {

    private final SpringAiLlmClient springAiLlmClient;

    /**
     * Constructs the service with the underlying Spring AI LLM client.
     *
     * @param springAiLlmClient the Spring AI LLM HTTP client
     */
    public LlmService(SpringAiLlmClient springAiLlmClient) {
        this.springAiLlmClient = springAiLlmClient;
    }

    /**
     * Initiates a streaming chat session with the LLM.
     *
     * <p>Invokes {@link SpringAiLlmClient#chatStreamWithDone} to stream response
     * chunks via callbacks.</p>
     *
     * @param systemPrompt the system prompt (may include RAG context)
     * @param userMessage  the user's message
     * @param onChunk     callback for each streamed text fragment
     * @param onComplete  callback when the stream finishes
     */
    public void chatStreamWithDone(String systemPrompt, String userMessage,
                                   java.util.function.Consumer<String> onChunk,
                                   Runnable onComplete) {
        springAiLlmClient.chatStreamWithDone(systemPrompt, userMessage, onChunk, onComplete);
    }

    /**
     * Sends a blocking (non-streaming) chat request to the LLM.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user's message
     * @return the complete LLM response as a string
     */
    public String chat(String systemPrompt, String userMessage) {
        return springAiLlmClient.chat(systemPrompt, userMessage);
    }
}
