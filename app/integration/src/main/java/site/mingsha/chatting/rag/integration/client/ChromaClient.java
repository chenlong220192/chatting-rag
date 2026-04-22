package site.mingsha.chatting.rag.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for interacting with ChromaDB's REST API (v2).
 *
 * <p>Provides document storage and vector similarity search operations
 * against a remote ChromaDB server. Handles collection creation/lookup,
 * document ingestion, and nearest-neighbor query with cosine similarity
 * computation on the client side.</p>
 *
 * <p>All requests target the {@code /api/v2} endpoint and use the
 * {@code default_tenant/default_database} namespace.</p>
 *
 * @see ChromaSearchResultVO
 */
@Slf4j
@Component
public class ChromaClient {

    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";
    private static final String COLLECTION_NAME = "documents";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    /**
     * Cached collection ID for the "documents" collection.
     *
     * <p>Marked {@code volatile} because it is written by {@link #getCollectionId()}
     * (after creating or fetching the collection) and by {@link #deleteAll()} (which
     * nulls it out), and read by multiple threads concurrently.  A plain field would
     * suffer a race condition where one thread nulls the value while another is
     * reading it, producing a spurious NPE.  {@code volatile} guarantees that all
     * threads see the most recent write immediately, which is sufficient here
     * because String assignment is atomic and no compound read-modify-write
     * operations are performed on this field.</p>
     */
    private volatile String collectionId;

    /**
     * Constructs the ChromaDB client.
     *
     * @param props        ChromaDB configuration properties (service URL)
     * @param objectMapper Jackson ObjectMapper for JSON parsing
     */
    public ChromaClient(site.mingsha.chatting.rag.integration.config.ChromaProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(props.serviceUrl())
                .build();
        log.info("[ChromaClient] 初始化完成，serviceUrl={}", props.serviceUrl());
    }

    /**
     * Retrieves (or creates) the target collection ID, caching it in memory.
     *
     * @return the collection ID string
     */
    private String getCollectionId() {
        if (collectionId != null) {
            log.trace("[ChromaClient] 使用缓存的 Collection ID: {}", collectionId);
            return collectionId;
        }

        log.info("[ChromaClient] 获取 Collection ID，name={}, tenant={}, db={}",
                COLLECTION_NAME, TENANT, DATABASE);

        try {
            String response = webClient.get()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections/{name}",
                            TENANT, DATABASE, COLLECTION_NAME)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.trace("[ChromaClient] Collection 查询响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            this.collectionId = root.path("id").asText();
            log.info("[ChromaClient] Collection ID 获取成功: {}", this.collectionId);
            return collectionId;
        } catch (Exception e) {
            log.warn("[ChromaClient] Collection 不存在或获取失败（404），将创建新集合: {}", e.getMessage());
            return createCollection();
        }
    }

    /**
     * Creates a new ChromaDB collection with the configured name.
     *
     * <p>Deletes the existing collection first if it exists (e.g., due to
     * embedding dimension mismatch), then creates a new one.</p>
     *
     * @return the newly created collection ID
     */
    private String createCollection() {
        log.info("[ChromaClient] 创建 Collection，name={}", COLLECTION_NAME);

        // 先尝试删除旧的（可能维度不匹配），避免 get_or_create 返回错误维度的旧 collection
        deleteAll();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("name", COLLECTION_NAME);
        requestBody.put("get_or_create", true);
        requestBody.put("embeddingFunction", "ollama");

        log.debug("[ChromaClient] 创建请求体: {}", requestBody);

        String reqBodyJson;
        try { reqBodyJson = objectMapper.writeValueAsString(requestBody); } catch (Exception ex) { reqBodyJson = requestBody.toString(); }
        log.info("[ChromaClient] 创建请求体: {}", reqBodyJson);

        try {
            String response = webClient.post()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections",
                            TENANT, DATABASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqBodyJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.trace("[ChromaClient] 创建响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            this.collectionId = root.path("id").asText();
            log.info("[ChromaClient] Collection 创建成功，name={}, id={}", COLLECTION_NAME, this.collectionId);
            return collectionId;
        } catch (Exception e) {
            log.error("[ChromaClient] 创建 Collection 失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建 ChromaDB collection 失败: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a batch of documents with their embeddings and metadata to ChromaDB.
     *
     * @param ids        list of unique chunk identifiers
     * @param texts      list of text contents
     * @param embeddings list of float[] embedding vectors
     * @param metadatas  list of JSON metadata strings
     */
    public void addDocuments(List<String> ids, List<String> texts, List<float[]> embeddings, List<String> metadatas) {
        String colId = getCollectionId();
        log.info("[ChromaClient] 添加文档，片段数={}, collectionId={}", ids.size(), colId);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("ids", objectMapper.valueToTree(ids));

        ArrayNode embeddingsArray = request.putArray("embeddings");
        for (float[] emb : embeddings) {
            ArrayNode embArray = embeddingsArray.addArray();
            for (float v : emb) {
                embArray.add(v);
            }
        }

        ArrayNode documentsArray = request.putArray("documents");
        texts.forEach(documentsArray::add);

        if (!metadatas.isEmpty()) {
            ArrayNode metadatasArray = request.putArray("metadatas");
            for (String meta : metadatas) {
                try {
                    metadatasArray.add(objectMapper.readTree(meta));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        for (int i = 0; i < Math.min(3, ids.size()); i++) {
            String contentPreview = texts.get(i).length() > 60
                    ? texts.get(i).substring(0, 60) + "..."
                    : texts.get(i);
            log.debug("[ChromaClient] 片段[{}] id={}, 内容=[{}]", i, ids.get(i), contentPreview);
        }
        if (ids.size() > 3) {
            log.debug("[ChromaClient] ... 共 {} 个片段", ids.size());
        }

        String reqJson;
        try { reqJson = objectMapper.writeValueAsString(request); } catch (Exception ex) { reqJson = request.toString(); }
        log.info("[ChromaClient] 请求体: {}", reqJson);

        try {
            String response = webClient.post()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections/{id}/add",
                            TENANT, DATABASE, colId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("[ChromaClient] 文档添加成功，片段数={}", ids.size());
        } catch (Exception e) {
            String body = e instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre
                    ? wcre.getResponseBodyAsString() : "N/A";
            log.error("[ChromaClient] 文档添加失败: {} | responseBody={}", e.getMessage(), body, e);
            throw e;
        }
    }

    /**
     * Performs a nearest-neighbor vector search in ChromaDB.
     *
     * <p>Queries the collection for the top-K most similar results,
     * computes cosine similarity client-side (since ChromaDB returns L2 distance),
     * and returns structured {@link ChromaSearchResultVO} objects.</p>
     *
     * @param queryEmbedding the query vector
     * @param topK           maximum number of results to return
     * @return list of search results, ordered by similarity (highest first)
     */
    public List<ChromaSearchResultVO> query(float[] queryEmbedding, int topK) {
        String colId = getCollectionId();
        long start = System.currentTimeMillis();

        log.info("[ChromaClient] 执行向量检索，topK={}, collectionId={}", topK, colId);
        log.debug("[ChromaClient] 查询向量维度={}, 前5维={}", queryEmbedding.length,
                java.util.Arrays.toString(java.util.Arrays.copyOf(queryEmbedding, Math.min(5, queryEmbedding.length))));

        ArrayNode embeddingsArray = objectMapper.createArrayNode();
        ArrayNode embArray = embeddingsArray.addArray();
        for (float v : queryEmbedding) {
            embArray.add(v);
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.set("query_embeddings", embeddingsArray);
        request.put("n_results", topK);
        request.putArray("include").add("documents").add("metadatas").add("distances").add("embeddings");

        String reqJson;
        try { reqJson = objectMapper.writeValueAsString(request); } catch (Exception ex) { reqJson = request.toString(); }
        log.info("[ChromaClient] 检索请求体: {}", reqJson);

        try {
            String response = webClient.post()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections/{id}/query",
                            TENANT, DATABASE, colId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.trace("[ChromaClient] 检索原始响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode ids = root.path("ids").path(0);
            JsonNode documents = root.path("documents").path(0);
            JsonNode distances = root.path("distances").path(0);
            JsonNode metadatas = root.path("metadatas").path(0);
            JsonNode storedEmbeddings = root.path("embeddings").path(0);

            List<ChromaSearchResultVO> results = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                double l2Dist = distances.get(i).asDouble();
                double cosineSim = computeCosineSimilarity(queryEmbedding, storedEmbeddings.get(i));
                String content = documents.get(i).asText();
                String contentPreview = content.length() > 50 ? content.substring(0, 50) + "..." : content;

                log.info("[ChromaClient] 结果[{}] id={}, L2距离={}, 余弦相似度={}, 内容=[{}]",
                        i, ids.get(i).asText(),
                        String.format("%.6f", l2Dist),
                        String.format("%.4f", cosineSim),
                        contentPreview);

                results.add(new ChromaSearchResultVO(
                        ids.get(i).asText(),
                        content,
                        l2Dist,
                        cosineSim,
                        metadatas.size() > 0 ? metadatas.get(i) : objectMapper.createObjectNode()
                ));
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("[ChromaClient] 检索完成，结果数={}, 耗时={}ms", results.size(), elapsed);
            return results;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String body = e instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre
                    ? wcre.getResponseBodyAsString() : "N/A";
            log.error("[ChromaClient] 检索失败，耗时={}ms: {} | responseBody={}", elapsed, e.getMessage(), body, e);
            throw new RuntimeException("解析 ChromaDB query 响应失败", e);
        }
    }

    /**
     * Deletes all documents by removing the entire ChromaDB collection.
     *
     * <p>The collection will be recreated on the next {@link #addDocuments} call.</p>
     */
    public void deleteAll() {
        log.warn("[ChromaClient] 清空所有文档（删除 Collection）");
        String colId;
        try {
            colId = getCollectionId();
        } catch (Exception e) {
            log.info("[ChromaClient] Collection 不存在，无需删除");
            return;
        }
        try {
            webClient.delete()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections/{id}",
                            TENANT, DATABASE, colId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("[ChromaClient] Collection 删除成功");
        } catch (Exception e) {
            log.warn("[ChromaClient] 删除 Collection 时出错: {}", e.getMessage());
        }
        this.collectionId = null;
    }

    /**
     * Deletes all vector entries belonging to a specific document by filtering
     * on the {@code doc_id} metadata field via ChromaDB's delete API.
     *
     * @param docId the unique document identifier
     */
    public void deleteByDocId(String docId) {
        String colId;
        try {
            colId = getCollectionId();
        } catch (Exception e) {
            log.info("[ChromaClient] Collection 不存在，无需删除，docId={}", docId);
            return;
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode whereClause = objectMapper.createObjectNode();
        whereClause.put("doc_id", docId);
        requestBody.set("where", whereClause);

        String reqJson;
        try { reqJson = objectMapper.writeValueAsString(requestBody); } catch (Exception ex) { reqJson = requestBody.toString(); }
        log.info("[ChromaClient] 删除文档，docId={}，请求体={}", docId, reqJson);

        try {
            String response = webClient.post()
                    .uri("/api/v2/tenants/{tenant}/databases/{db}/collections/{id}/delete",
                            TENANT, DATABASE, colId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("[ChromaClient] 文档删除成功，docId={}", docId);
        } catch (Exception e) {
            String body = e instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre
                    ? wcre.getResponseBodyAsString() : "N/A";
            log.error("[ChromaClient] 文档删除失败，docId={}: {} | responseBody={}", docId, e.getMessage(), body, e);
        }
    }

    /**
     * Computes cosine similarity between a query vector and a stored vector.
     *
     * @param a query vector (float array)
     * @param b stored vector (JsonNode of doubles)
     * @return cosine similarity score in range [-1, 1]
     */
    private double computeCosineSimilarity(float[] a, JsonNode b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            double ai = a[i];
            double bi = b.get(i).asDouble();
            dotProduct += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
