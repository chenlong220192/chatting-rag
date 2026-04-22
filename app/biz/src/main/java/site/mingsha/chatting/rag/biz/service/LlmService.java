package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.mingsha.chatting.rag.integration.client.LlmClient;

/**
 * Business-level service for LLM chat operations.
 *
 * <p>Delegates to {@link LlmClient} for both streaming and blocking
 * chat interactions with the configured LLM provider.</p>
 *
 * @see LlmClient
 */
@Slf4j
@Service
public class LlmService {

    private final LlmClient llmClient;

    /**
     * Constructs the service with the underlying LLM client.
     *
     * @param llmClient the LLM HTTP client
     */
    public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Initiates a streaming chat session with the LLM.
     *
     * <p>Invokes {@link LlmClient#chatStreamWithDone} to stream response
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
        llmClient.chatStreamWithDone(systemPrompt, userMessage, onChunk, onComplete);
    }

    /**
     * Sends a blocking (non-streaming) chat request to the LLM.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user's message
     * @return the complete LLM response as a string
     */
    public String chat(String systemPrompt, String userMessage) {
        return llmClient.chat(systemPrompt, userMessage);
    }
}
