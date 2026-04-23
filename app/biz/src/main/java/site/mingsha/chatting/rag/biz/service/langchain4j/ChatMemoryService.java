package site.mingsha.chatting.rag.biz.service.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversation memory service using LangChain4j {@link MessageWindowChatMemory}.
 *
 * <p>Maintains per-session chat history by wrapping LangChain4j's sliding-window
 * chat memory. The memory is used to inject conversation context into the RAG prompt,
 * enabling the LLM to understand multi-turn dialogue.</p>
 *
 * <p>Each conversation has its own {@link MessageWindowChatMemory} instance keyed
 * by session ID. Old messages beyond the configured window are automatically evicted.</p>
 *
 * @see MessageWindowChatMemory
 */
@Slf4j
@Service
public class ChatMemoryService {

    /** Maximum number of messages to retain per conversation. */
    private final int maxMessages;

    /** Stores ChatMemory per conversation/session ID. */
    private final Map<String, MessageWindowChatMemory> memories = new ConcurrentHashMap<>();

    /**
     * Constructs the chat memory service.
     *
     * @param maxMessages max number of messages to keep per conversation (default 20)
     */
    public ChatMemoryService(@Value("${chat.memory.max-messages:20}") int maxMessages) {
        this.maxMessages = maxMessages;
        log.info("[ChatMemory] 初始化完成，maxMessages={}", maxMessages);
    }

    /**
     * Retrieves (or creates) the ChatMemory for the given conversation ID.
     */
    private MessageWindowChatMemory memoryFor(String conversationId) {
        return memories.computeIfAbsent(conversationId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(maxMessages)
                        .build()
        );
    }

    /**
     * Adds a user message to the conversation history.
     *
     * @param conversationId unique conversation/session identifier
     * @param text the user's message content
     */
    public void addUserMessage(String conversationId, String text) {
        log.debug("[ChatMemory] 添加用户消息，conversationId={}, text=[{}]",
                conversationId, text.length() > 50 ? text.substring(0, 50) + "..." : text);
        memoryFor(conversationId).add(new UserMessage(text));
    }

    /**
     * Adds an assistant (LLM) message to the conversation history.
     *
     * @param conversationId unique conversation/session identifier
     * @param text the assistant's response content
     */
    public void addAssistantMessage(String conversationId, String text) {
        log.debug("[ChatMemory] 添加助手消息，conversationId={}, text=[{}]",
                conversationId, text.length() > 50 ? text.substring(0, 50) + "..." : text);
        memoryFor(conversationId).add(new AiMessage(text));
    }

    /**
     * Returns the formatted conversation history as a string, suitable for
     * injection into a prompt.
     *
     * <p>Format:</p>
     * <pre>
     * 对话历史：
     * 用户：xxx
     * 助手：yyy
     * 用户：zzz
     * 助手：www
     * </pre>
     *
     * @param conversationId unique conversation/session identifier
     * @return formatted history string, or empty string if no history
     */
    public String getFormattedHistory(String conversationId) {
        List<ChatMessage> messages = memoryFor(conversationId).messages();
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("【对话历史】\n");
        for (ChatMessage msg : messages) {
            switch (msg.type()) {
                case USER -> {
                    String text = extractText((UserMessage) msg);
                    sb.append("用户：").append(text).append("\n");
                }
                case AI -> sb.append("助手：").append(((AiMessage) msg).text()).append("\n");
                case SYSTEM -> sb.append("系统：").append(((SystemMessage) msg).text()).append("\n");
                default -> {} // skip ToolExecutionResultMessage, etc.
            }
        }
        return sb.toString();
    }

    /**
     * Returns the number of messages currently stored for the conversation.
     *
     * @param conversationId unique conversation/session identifier
     * @return message count
     */
    public int getMessageCount(String conversationId) {
        return memoryFor(conversationId).messages().size();
    }

    /**
     * Clears all conversation history for the given conversation.
     *
     * @param conversationId unique conversation/session identifier
     */
    public void clearConversation(String conversationId) {
        log.info("[ChatMemory] 清空对话历史，conversationId={}", conversationId);
        memories.remove(conversationId);
    }

    /**
     * Clears all conversation histories.
     */
    public void clearAll() {
        log.info("[ChatMemory] 清空所有对话历史");
        memories.clear();
    }

    /**
     * Builds a system prompt that includes both the provided system prompt
     * and the conversation history.
     *
     * <p>If no history exists, returns the system prompt unchanged.</p>
     *
     * @param conversationId unique conversation/session identifier
     * @param systemPrompt the base system prompt (may include RAG context)
     * @return enriched system prompt with conversation history
     */
    public String buildSystemPromptWithHistory(String conversationId, String systemPrompt) {
        String history = getFormattedHistory(conversationId);
        if (history.isEmpty()) {
            return systemPrompt;
        }
        return systemPrompt + "\n\n" + history;
    }

    /**
     * Extracts the text content from a UserMessage by finding the first TextContent.
     */
    private static String extractText(UserMessage msg) {
        return msg.contents().stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .findFirst()
                .orElse("");
    }
}
