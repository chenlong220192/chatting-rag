package site.mingsha.chatting.rag.biz.model.dto;

import java.util.List;

/**
 * Response DTO for the chat endpoint (non-streaming).
 *
 * @param answer     the LLM-generated answer text
 * @param references list of source document references used in the answer
 */
public record ChatResponseDTO(
        String answer,
        List<Reference> references
) {
    /**
     * A single source document reference returned alongside the LLM answer.
     *
     * @param content     truncated text content of the retrieved chunk
     * @param documentId  identifier of the source document
     * @param score       cosine similarity score of this reference
     */
    public record Reference(
            /** Truncated content of the retrieved chunk (max 200 chars). */
            String content,
            /** Identifier of the document this chunk belongs to. */
            String documentId,
            /** Cosine similarity score of the retrieval. */
            double score
    ) {}

    /**
     * Wrapper used by the SSE endpoint to carry references alongside
     * the streaming response metadata.
     *
     * @param references list of document references
     */
    public record StreamingResult(
            List<Reference> references
    ) {}
}
