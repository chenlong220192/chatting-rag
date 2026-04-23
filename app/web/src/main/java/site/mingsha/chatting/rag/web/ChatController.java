package site.mingsha.chatting.rag.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import site.mingsha.chatting.rag.biz.model.dto.ChatRequestDTO;
import site.mingsha.chatting.rag.biz.model.dto.ChatResponseDTO;
import site.mingsha.chatting.rag.biz.service.RAGService;
import site.mingsha.chatting.rag.biz.service.langchain4j.ChatMemoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller handling chat-related HTTP endpoints.
 *
 * <p>Provides a streaming SSE endpoint for RAG-powered conversational responses
 * with multi-turn conversation memory (via {@link ChatMemoryService}).</p>
 *
 * @see RAGService
 * @see ChatMemoryService
 * @see SseEmitter
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/api/chat")
public class ChatController {

    private final RAGService ragService;
    private final ChatMemoryService chatMemoryService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param ragService         RAG orchestration service
     * @param chatMemoryService LangChain4j chat memory service
     * @param objectMapper       Jackson ObjectMapper for JSON serialization
     */
    public ChatController(RAGService ragService,
                           ChatMemoryService chatMemoryService,
                           ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.chatMemoryService = chatMemoryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles a streaming chat request via Server-Sent Events (SSE).
     *
     * <p>Emits sequential SSE events:</p>
     * <ol>
     *   <li>{@code references} — JSON array of retrieved document chunks</li>
     *   <li>{@code meta} — model name and context usage metadata</li>
     *   <li>{@code data} — individual LLM response chunks (streamed)</li>
     *   <li>{@code done} — signals end of stream</li>
     * </ol>
     *
     * <p>Conversation history is maintained via {@link ChatMemoryService} using
     * the {@code conversationId} from the request. The user message is added
     * to memory before the LLM call, and the full assistant response is added
     * after the stream completes.</p>
     *
     * @param request the chat request containing user message and optional session ID
     * @return an {@link SseEmitter} for the streaming response
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO request) {
        String conversationId = resolveConversationId(request.conversationId());
        log.info("[Chat] =========================================");
        log.info("[Chat] 收到聊天请求，message=[{}], conversationId=[{}]",
                request.message().length() > 60
                        ? request.message().substring(0, 60) + "..."
                        : request.message(),
                conversationId);
        log.info("[Chat] =========================================");

        // Add user message to conversation memory before LLM call
        chatMemoryService.addUserMessage(conversationId, request.message());

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        StringBuilder assistantResponse = new StringBuilder();

        try {
            log.info("[Chat] Step 1: 检索文档引用...");
            var refs = ragService.getReferences(request.message());
            String refsJson = objectMapper.writeValueAsString(refs);
            log.info("[Chat] Step 1 完成，引用数={}, JSON长度={}", refs.size(), refsJson.length());
            emitter.send(SseEmitter.event().name("references").data(refsJson));
            log.info("[Chat] 已发送 event:references");

            log.info("[Chat] Step 2: 构建 system prompt（包含对话历史）...");
            String baseSystemPrompt = ragService.buildSystemPrompt(request.message());
            String systemPromptWithHistory =
                    chatMemoryService.buildSystemPromptWithHistory(conversationId, baseSystemPrompt);
            log.info("[Chat] Step 2 完成，base systemPrompt长度={}, 含历史后={}",
                    baseSystemPrompt.length(), systemPromptWithHistory.length());

            var meta = ragService.buildMeta(systemPromptWithHistory, request.message());
            String metaJson = objectMapper.writeValueAsString(meta);
            log.info("[Chat] Step 3: 发送 meta，model={}, contextUsed={}, contextLimit={}",
                    meta.model(), meta.contextUsed(), meta.contextLimit());
            emitter.send(SseEmitter.event().name("meta").data(metaJson));
            log.info("[Chat] 已发送 event:meta");

            log.info("[Chat] Step 4: 开始流式 LLM 对话...");
            log.info("[Chat] =========================================");

            AtomicInteger counter = new AtomicInteger(0);
            ragService.chatStream(systemPromptWithHistory, request.message(),
                    chunk -> {
                        assistantResponse.append(chunk);
                        int count = counter.incrementAndGet();
                        log.trace("[Chat] 收到 LLM chunk[{}]: [{}]", count, chunk);
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (Exception e) {
                            log.warn("[Chat] 发送 chunk 失败: {}", e.getMessage());
                        }
                    },
                    () -> {
                        try {
                            log.info("[Chat] LLM 流结束，共发送 {} 个 chunk", counter.get());
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();

                            // Add assistant response to conversation memory after stream completes
                            String fullResponse = assistantResponse.toString();
                            if (!fullResponse.isEmpty()) {
                                chatMemoryService.addAssistantMessage(conversationId, fullResponse);
                                log.debug("[Chat] 已将助手回复添加到对话记忆，长度={}", fullResponse.length());
                            }
                            log.info("[Chat] SSE 连接正常关闭");
                        } catch (Exception e) {
                            log.warn("[Chat] 关闭连接失败: {}", e.getMessage());
                        }
                    }
            );

            log.info("[Chat] SSE 订阅已发起，等待 LLM 响应...");

        } catch (Exception e) {
            log.error("[Chat] SSE 流异常: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data("An error occurred. Please try again."));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Resolves a conversation ID from the provided string.
     * Uses a blank string as the default (meaning anonymous/default conversation).
     */
    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "default-session";
        }
        return conversationId;
    }
}
