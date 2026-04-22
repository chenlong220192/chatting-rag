package site.mingsha.chatting.rag.biz.model.dto;

/**
 * Response DTO returned after a successful document upload.
 *
 * @param id       unique document identifier assigned during indexing
 * @param filename original filename of the uploaded document
 * @param size     file size in bytes
 * @param status   indexing status (e.g., "indexed")
 */
public record DocumentResponseDTO(
        String id,
        String filename,
        long size,
        String status
) {}
