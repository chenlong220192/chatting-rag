package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;

import java.util.List;

/**
 * Business-level service for ChromaDB vector store operations.
 *
 * <p>Acts as a thin facade over {@link ChromaClient}, providing
 * document addition, similarity search, and deletion operations
 * for the RAG pipeline.</p>
 *
 * @see ChromaClient
 */
@Slf4j
@Service
public class ChromaService {

    private final ChromaClient chromaClient;

    /**
     * Constructs the service with the underlying ChromaDB client.
     *
     * @param chromaClient the ChromaDB HTTP client
     */
    public ChromaService(ChromaClient chromaClient) {
        this.chromaClient = chromaClient;
    }

    /**
     * Adds a batch of documents with their embeddings and metadata to ChromaDB.
     *
     * @param ids        unique chunk identifiers
     * @param texts      original text content for each chunk
     * @param embeddings float vector arrays for each chunk
     * @param metadatas  JSON metadata strings for each chunk
     */
    public void addDocuments(List<String> ids, List<String> texts,
                            List<float[]> embeddings, List<String> metadatas) {
        chromaClient.addDocuments(ids, texts, embeddings, metadatas);
    }

    /**
     * Performs a similarity search in ChromaDB using a query embedding.
     *
     * @param queryEmbedding the query vector
     * @param topK          maximum number of results to return
     * @return list of search results ordered by similarity
     */
    public List<ChromaSearchResultVO> query(float[] queryEmbedding, int topK) {
        return chromaClient.query(queryEmbedding, topK);
    }

    /**
     * Deletes all documents from the ChromaDB collection.
     */
    public void deleteAll() {
        chromaClient.deleteAll();
    }
}
