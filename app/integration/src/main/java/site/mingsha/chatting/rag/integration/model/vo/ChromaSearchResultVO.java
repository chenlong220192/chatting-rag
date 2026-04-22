package site.mingsha.chatting.rag.integration.model.vo;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Value object representing a single ChromaDB vector search result.
 *
 * @param id                unique chunk identifier from ChromaDB
 * @param content           original text content of the retrieved chunk
 * @param distance          L2 (Euclidean) distance returned by ChromaDB
 * @param cosineSimilarity   computed cosine similarity score (client-side)
 * @param metadata           arbitrary JSON metadata attached to the chunk
 */
public record ChromaSearchResultVO(
        String id,
        String content,
        double distance,
        double cosineSimilarity,
        JsonNode metadata
) {}
