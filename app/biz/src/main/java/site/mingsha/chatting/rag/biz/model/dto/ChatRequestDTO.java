package site.mingsha.chatting.rag.biz.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the chat endpoint.
 *
 * @param message        the user's text message
 * @param conversationId optional conversation/session identifier for multi-turn chat memory.
 *                      If null or blank, a session ID will be derived from the request.
 */
public record ChatRequestDTO(
        @NotBlank(message = "message cannot be blank")
        @Size(max = 8192, message = "message exceeds maximum length of 8192 characters")
        String message,

        /** Optional conversation ID. If absent, the server derives a session ID. */
        String conversationId
) {
    public ChatRequestDTO {
        // Ensure conversationId is never null in practice
        if (conversationId == null) {
            conversationId = "";
        }
    }

    /** Convenience constructor for requests without conversation ID. */
    public ChatRequestDTO(String message) {
        this(message, "");
    }
}
