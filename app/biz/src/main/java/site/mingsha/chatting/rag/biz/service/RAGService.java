package site.mingsha.chatting.rag.biz.service;

import lombok.extern.slf4j.Slf4j;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;
import site.mingsha.chatting.rag.integration.client.LlmClient;
import site.mingsha.chatting.rag.integration.config.LlmProperties;
import site.mingsha.chatting.rag.integration.config.RagProperties;
import site.mingsha.chatting.rag.biz.model.dto.ChatMetaDTO;
import site.mingsha.chatting.rag.biz.model.dto.ChatResponseDTO;
import site.mingsha.chatting.rag.biz.model.dto.ChatResponseDTO.Reference;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Core RAG (Retrieval-Augmented Generation) orchestration service.
 *
 * <p>Coordinates the end-to-end RAG pipeline:</p>
 * <ol>
 *   <li>Embeds the user's query using {@link EmbeddingClient}</li>
 *   <li>Retrieves top-K similar document chunks from {@link ChromaClient}</li>
 *   <li>Filters results by minimum similarity score</li>
 *   <li>Builds a system prompt with the retrieved context</li>
 *   <li>Delegates to {@link LlmClient} for answer generation</li>
 * </ol>
 *
 * <p>This service exposes both blocking (for synchronous chat) and streaming
 * (for SSE-based) interfaces.</p>
 *
 * @see EmbeddingClient
 * @see ChromaClient
 * @see LlmClient
 */
@Slf4j
@Service
public class RAGService {

    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;
    private final ChromaClient chromaClient;
    private final RagProperties ragProperties;
    private final LlmProperties llmProperties;
    private final String systemPromptTemplate;

    /**
     * Constructs the RAG service with all required dependencies.
     *
     * @param embeddingClient      embedding service client
     * @param llmClient           LLM service client
     * @param chromaClient         ChromaDB client
     * @param ragProperties        RAG configuration (top-K, min-score, chunk params)
     * @param llmProperties        LLM configuration (model, context-limit, etc.)
     * @param systemPromptTemplate system prompt template with a {@code %s} placeholder for context
     */
    public RAGService(
            EmbeddingClient embeddingClient,
            LlmClient llmClient,
            ChromaClient chromaClient,
            RagProperties ragProperties,
            LlmProperties llmProperties,
            @Value("${prompt.template}") String systemPromptTemplate
    ) {
        this.embeddingClient = embeddingClient;
        this.llmClient = llmClient;
        this.chromaClient = chromaClient;
        this.ragProperties = ragProperties;
        this.llmProperties = llmProperties;
        this.systemPromptTemplate = systemPromptTemplate;
        log.info("[RAG] 初始化完成，topK={}, minScore={}, chunkSize={}, chunkOverlap={}",
                ragProperties.topK(), ragProperties.minScore(),
                ragProperties.chunk().size(), ragProperties.chunk().overlap());
    }

    /**
     * Processes a complete chat request synchronously.
     *
     * <p>Runs the full RAG pipeline and returns the answer along with
     * source document references.</p>
     *
     * @param userMessage the user's question
     * @return a {@link ChatResponseDTO} containing the answer and references
     */
    public ChatResponseDTO chat(String userMessage) {
        long start = System.currentTimeMillis();
        log.info("[RAG] ========== 处理对话请求 ==========");
        log.info("[RAG] 用户问题: [{}]", userMessage);

        log.info("[RAG] Step 1: 向量化用户问题...");
        float[] queryEmbedding = embeddingClient.embed(userMessage);
        log.debug("[RAG] 问题向量维度: {}", queryEmbedding.length);

        log.info("[RAG] Step 2: ChromaDB 向量检索，topK={}", ragProperties.topK());
        List<ChromaSearchResultVO> searchResults =
                chromaClient.query(queryEmbedding, ragProperties.topK());

        log.info("[RAG] Step 3: 过滤检索结果，minScore={}", ragProperties.minScore());
        List<ChromaSearchResultVO> filtered = searchResults.stream()
                .filter(r -> r.cosineSimilarity() >= ragProperties.minScore())
                .toList();
        log.info("[RAG] 过滤结果: 检索总数={}, 通过阈值={}, 丢弃={}",
                searchResults.size(), filtered.size(), searchResults.size() - filtered.size());

        log.info("[RAG] Step 4: 构建 system prompt...");
        String context = buildContext(filtered);
        String systemPrompt;
        if (filtered.isEmpty()) {
            systemPrompt = "你是一个助手。没有可用的参考文档，请直接回答用户的问题。";
            log.info("[RAG] 无可用参考文档，使用无文档 prompt");
        } else {
            systemPrompt = String.format(systemPromptTemplate, context);
            log.info("[RAG] 使用 RAG prompt，context 长度={}", context.length());
            log.debug("[RAG] 完整 system prompt:\n{}", systemPrompt);
        }

        log.info("[RAG] Step 5: 调用 LLM，model={}", llmProperties.model());
        String answer = llmClient.chat(systemPrompt, userMessage);
        log.info("[RAG] LLM 回复长度={}", answer.length());
        log.debug("[RAG] LLM 回复预览: [{}]",
                answer.length() > 150 ? answer.substring(0, 150) + "..." : answer);

        List<Reference> references = filtered.stream()
                .map(r -> new Reference(
                        truncate(r.content(), 200),
                        r.metadata() != null ? r.metadata().path("doc_id").asText("") : "",
                        r.cosineSimilarity()
                ))
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - start;
        log.info("[RAG] ========== 对话处理完成 ==========");
        log.info("[RAG] 回复长度={}, 引用数={}, 总耗时={}ms",
                answer.length(), references.size(), elapsed);

        return new ChatResponseDTO(answer, references);
    }

    /**
     * Retrieves document references for a given user query.
     *
     * <p>Performs vector search only, without calling the LLM.
     * Used by the SSE endpoint to send references before streaming starts.</p>
     *
     * @param userMessage the user's question
     * @return a list of {@link Reference} objects above the minimum score threshold
     */
    public List<Reference> getReferences(String userMessage) {
        log.info("[RAG] 获取引用，userMessage=[{}]", userMessage);

        float[] queryEmbedding = embeddingClient.embed(userMessage);
        List<ChromaSearchResultVO> searchResults =
                chromaClient.query(queryEmbedding, ragProperties.topK());

        log.info("[RAG] ChromaDB 检索完成，总数={}, minScore={}", searchResults.size(), ragProperties.minScore());

        List<Reference> references = searchResults.stream()
                .filter(r -> r.cosineSimilarity() >= ragProperties.minScore())
                .map(r -> new Reference(
                        truncate(r.content(), 200),
                        r.metadata() != null ? r.metadata().path("doc_id").asText("") : "",
                        r.cosineSimilarity()
                ))
                .collect(Collectors.toList());

        log.info("[RAG] 过滤后可用引用数={}", references.size());
        references.forEach(r ->
                log.info("[RAG]   引用 content=[{}], score={:.4f}",
                        r.content().length() > 60 ? r.content().substring(0, 60) + "..." : r.content(),
                        r.score()));

        return references;
    }

    /**
     * Builds the system prompt for the LLM based on the current query.
     *
     * <p>Retrieves relevant document chunks, filters by score threshold,
     * and formats them into the system prompt template.</p>
     *
     * @param userMessage the user's question
     * @return the formatted system prompt string
     */
    public String buildSystemPrompt(String userMessage) {
        log.info("[RAG] 构建 system prompt，userMessage=[{}]", userMessage);

        float[] queryEmbedding = embeddingClient.embed(userMessage);
        List<ChromaSearchResultVO> searchResults =
                chromaClient.query(queryEmbedding, ragProperties.topK());

        List<ChromaSearchResultVO> filtered = searchResults.stream()
                .filter(r -> r.cosineSimilarity() >= ragProperties.minScore())
                .toList();

        log.info("[RAG] 检索结果: 总数={}, 通过阈值={}, minScore={}",
                searchResults.size(), filtered.size(), ragProperties.minScore());

        String context = buildContext(filtered);
        if (filtered.isEmpty()) {
            log.info("[RAG] 无可用参考文档，返回无文档 prompt");
            return "你是一个助手。没有可用的参考文档，请直接回答用户的问题。";
        }

        String prompt = String.format(systemPromptTemplate, context);
        log.info("[RAG] system prompt 构建完成，长度={}", prompt.length());
        log.debug("[RAG] system prompt 内容:\n{}", prompt);
        return prompt;
    }

    /**
     * Builds metadata about the current chat context for the frontend.
     *
     * <p>Estimates token usage based on string length (÷4) and includes
     * the model name and its context window limit.</p>
     *
     * @param systemPrompt the built system prompt
     * @param userMessage  the user's original message
     * @return a {@link ChatMetaDTO} with model and context information
     */
    public ChatMetaDTO buildMeta(String systemPrompt, String userMessage) {
        int used = (systemPrompt.length() + userMessage.length()) / 4;
        log.debug("[RAG] 构建 meta，systemPrompt长度={}, userMessage长度={}, 估算token={}, contextLimit={}",
                systemPrompt.length(), userMessage.length(), used, llmProperties.contextLimit());
        return new ChatMetaDTO(llmProperties.model(), used, llmProperties.contextLimit());
    }

    /**
     * Initiates a streaming chat session with the LLM.
     *
     * <p>The provided {@code onChunk} callback is invoked for each streamed
     * content fragment. {@code onComplete} is called when the stream finishes.</p>
     *
     * @param systemPrompt the system prompt containing retrieved context
     * @param userMessage  the user's question
     * @param onChunk     callback invoked for each streamed text fragment
     * @param onComplete   callback invoked when the stream finishes
     */
    public void chatStream(String systemPrompt, String userMessage,
                           Consumer<String> onChunk, Runnable onComplete) {
        log.info("[RAG] ========== 流式对话开始 ==========");
        log.info("[RAG] systemPrompt长度={}, userMessage=[{}]", systemPrompt.length(), userMessage);
        llmClient.chatStreamWithDone(systemPrompt, userMessage, onChunk, onComplete);
    }

    /**
     * Builds a concatenated context string from a list of search results.
     *
     * <p>Each result is prefixed with a document number label.
     * Returns a placeholder string if the result list is empty.</p>
     *
     * @param results list of ChromaDB search results
     * @return formatted context string for prompt injection
     */
    private String buildContext(List<ChromaSearchResultVO> results) {
        if (results.isEmpty()) {
            return "（暂无参考文档）";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            ChromaSearchResultVO r = results.get(i);
            double score = r.cosineSimilarity();
            String contentPreview = r.content().length() > 80
                    ? r.content().substring(0, 80) + "..."
                    : r.content();
            log.debug("[RAG] 拼入上下文[{}]: score={:.4f}, content=[{}]", i, score, contentPreview);
            sb.append("【文档 ").append(i + 1).append("】\n")
              .append(r.content())
              .append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Truncates a string to a maximum length, appending "..." if truncated.
     *
     * @param text    the input string
     * @param maxLen  maximum allowed length before truncation
     * @return truncated string or the original if within limit
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
