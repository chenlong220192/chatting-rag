package site.mingsha.chatting.rag.biz.model.dto;

/**
 * Request DTO for the chat endpoint.
 *
 * @param message   the user's text message
 * @param sessionId optional session identifier for conversation continuity
 */
public record ChatRequestDTO(
        String message,
        String sessionId
) {}
