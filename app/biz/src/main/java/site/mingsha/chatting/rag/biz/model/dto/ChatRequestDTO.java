package site.mingsha.chatting.rag.biz.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the chat endpoint.
 *
 * @param message the user's text message
 */
public record ChatRequestDTO(
        @NotBlank(message = "message cannot be blank")
        @Size(max = 8192, message = "message exceeds maximum length of 8192 characters")
        String message
) {}
