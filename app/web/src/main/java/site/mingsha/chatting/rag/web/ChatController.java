package site.mingsha.chatting.rag.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import site.mingsha.chatting.rag.biz.model.dto.ChatRequestDTO;
import site.mingsha.chatting.rag.biz.service.RAGService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller handling chat-related HTTP endpoints.
 *
 * <p>Provides a streaming SSE endpoint for RAG-powered conversational responses.
 * The controller orchestrates three sequential steps: retrieving document references,
 * building the system prompt, and streaming LLM output to the client.</p>
 *
 * @see RAGService
 * @see SseEmitter
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/api/chat")
public class ChatController {

    private final RAGService ragService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param ragService    RAG orchestration service
     * @param objectMapper  Jackson ObjectMapper for JSON serialization
     */
    public ChatController(RAGService ragService, ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles a streaming chat request via Server-Sent Events (SSE).
     *
     * <p>Emits three sequential SSE events followed by streaming chunks:</p>
     * <ol>
     *   <li>{@code references} — JSON array of retrieved document chunks</li>
     *   <li>{@code meta} — model name and context usage metadata</li>
     *   <li>{@code data} — individual LLM response chunks (streamed)</li>
     *   <li>{@code done} — signals end of stream</li>
     * </ol>
     *
     * @param request the chat request containing user message and optional session ID
     * @return an {@link SseEmitter} for the streaming response
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO request) {
        log.info("[Chat] =========================================");
        log.info("[Chat] 收到聊天请求，message=[{}]", request.message());
        log.info("[Chat] =========================================");

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {
            log.info("[Chat] Step 1: 检索文档引用...");
            var refs = ragService.getReferences(request.message());
            String refsJson = objectMapper.writeValueAsString(refs);
            log.info("[Chat] Step 1 完成，引用数={}, JSON长度={}", refs.size(), refsJson.length());
            log.debug("[Chat] 引用 JSON: {}", refsJson);
            emitter.send(SseEmitter.event().name("references").data(refsJson));
            log.info("[Chat] 已发送 event:references");

            log.info("[Chat] Step 2: 构建 system prompt...");
            String systemPrompt = ragService.buildSystemPrompt(request.message());
            log.info("[Chat] Step 2 完成，systemPrompt长度={}", systemPrompt.length());

            var meta = ragService.buildMeta(systemPrompt, request.message());
            String metaJson = objectMapper.writeValueAsString(meta);
            log.info("[Chat] Step 3: 发送 meta，model={}, contextUsed={}, contextLimit={}",
                    meta.model(), meta.contextUsed(), meta.contextLimit());
            emitter.send(SseEmitter.event().name("meta").data(metaJson));
            log.info("[Chat] 已发送 event:meta");

            log.info("[Chat] Step 4: 开始流式 LLM 对话...");
            log.info("[Chat] =========================================");

            AtomicInteger counter = new AtomicInteger(0);
            ragService.chatStream(systemPrompt, request.message(),
                    chunk -> {
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
}
