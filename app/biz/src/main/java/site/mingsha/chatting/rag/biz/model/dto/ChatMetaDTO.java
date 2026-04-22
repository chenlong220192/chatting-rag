package site.mingsha.chatting.rag.biz.model.dto;

/**
 * Metadata DTO sent as the SSE {@code meta} event during a streaming chat session.
 *
 * <p>Provides the frontend with model information and estimated context usage
 * for rendering a progress indicator.</p>
 *
 * @param model        the LLM model name (e.g., gpt-4o)
 * @param contextUsed  estimated token count used in the current context window
 * @param contextLimit the model's maximum context window size
 */
public record ChatMetaDTO(
        String model,
        int contextUsed,
        int contextLimit
) {}
