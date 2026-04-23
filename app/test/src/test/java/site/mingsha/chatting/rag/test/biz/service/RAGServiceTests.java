package site.mingsha.chatting.rag.test.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.config.RagProperties;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;
import site.mingsha.chatting.rag.biz.service.EmbeddingService;
import site.mingsha.chatting.rag.biz.service.LlmService;
import site.mingsha.chatting.rag.biz.service.RAGService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RAGService Tests")
class RAGServiceTests {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private LlmService llmService;
    @Mock
    private ChromaClient chromaClient;
    @Mock
    private RagProperties ragProperties;
    @Mock
    private RagProperties.ChunkConfig chunkConfig;

    private RAGService ragService;

    @BeforeEach
    void setUp() {
        when(ragProperties.topK()).thenReturn(5);
        when(ragProperties.minScore()).thenReturn(0.5);
        when(ragProperties.chunk()).thenReturn(chunkConfig);
        when(chunkConfig.size()).thenReturn(512);
        when(chunkConfig.overlap()).thenReturn(128);

        ragService = new RAGService(
                embeddingService, llmService, chromaClient,
                ragProperties,
                "gpt-4o",
                128000,
                "参考文档：%s\n\n请根据以上文档回答用户的问题。"
        );
    }

    @Test
    @DisplayName("chat with references builds prompt and calls LLM")
    void chat_withReferences_buildsPromptAndCallsLlm() {
        com.fasterxml.jackson.databind.node.ObjectNode meta = new ObjectMapper().createObjectNode();
        ChromaSearchResultVO result = new ChromaSearchResultVO(
                "chunk1", "Some document content", 0.1, 0.95, meta
        );
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of(result));
        when(llmService.chat(anyString(), eq("Hello"))).thenReturn("This is the answer.");

        var response = ragService.chat("Hello");

        assertThat(response.answer()).isEqualTo("This is the answer.");
        assertThat(response.references()).hasSize(1);
        assertThat(response.references().get(0).score()).isEqualTo(0.95);
        verify(llmService).chat(anyString(), eq("Hello"));
    }

    @Test
    @DisplayName("chat no references uses fallback prompt")
    void chat_noReferences_usesFallbackPrompt() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of());
        when(llmService.chat(eq("你是一个助手。没有可用的参考文档，请直接回答用户的问题。"), eq("Hello")))
                .thenReturn("Fallback answer.");

        var response = ragService.chat("Hello");

        assertThat(response.answer()).isEqualTo("Fallback answer.");
        assertThat(response.references()).isEmpty();
    }

    @Test
    @DisplayName("chat no references after filter uses fallback prompt")
    void chat_noReferencesAfterFilter_usesFallbackPrompt() {
        com.fasterxml.jackson.databind.node.ObjectNode meta = new ObjectMapper().createObjectNode();
        ChromaSearchResultVO belowThreshold = new ChromaSearchResultVO(
                "chunk1", "Low relevance", 0.9, 0.1, meta
        );
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of(belowThreshold));
        when(llmService.chat(anyString(), anyString())).thenReturn("Fallback answer.");

        var response = ragService.chat("Hello");

        assertThat(response.answer()).isNotNull();
        assertThat(response.references()).isEmpty();
    }

    @Test
    @DisplayName("getReferences returns filtered references")
    void getReferences_returnsFilteredReferences() {
        com.fasterxml.jackson.databind.node.ObjectNode meta = new ObjectMapper().createObjectNode();
        ChromaSearchResultVO r1 = new ChromaSearchResultVO("id1", "content1", 0.1, 0.9, meta);
        ChromaSearchResultVO r2 = new ChromaSearchResultVO("id2", "content2", 0.2, 0.3, meta);
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of(r1, r2));

        var refs = ragService.getReferences("Hello");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).score()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("getReferences empty result returns empty list")
    void getReferences_emptyResult_returnsEmptyList() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of());

        var refs = ragService.getReferences("Hello");

        assertThat(refs).isEmpty();
    }

    @Test
    @DisplayName("buildSystemPrompt with context injects context")
    void buildSystemPrompt_withContext_injectsContext() {
        com.fasterxml.jackson.databind.node.ObjectNode meta = new ObjectMapper().createObjectNode();
        ChromaSearchResultVO result = new ChromaSearchResultVO("id1", "Doc content", 0.1, 0.9, meta);
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of(result));

        String prompt = ragService.buildSystemPrompt("Hello");

        assertThat(prompt).contains("Doc content");
        assertThat(prompt).contains("【文档 1】");
    }

    @Test
    @DisplayName("buildSystemPrompt empty returns fallback")
    void buildSystemPrompt_empty_returnsFallback() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chromaClient.query(any(float[].class), eq(5))).thenReturn(List.of());

        String prompt = ragService.buildSystemPrompt("Hello");

        assertThat(prompt).isEqualTo("你是一个助手。没有可用的参考文档，请直接回答用户的问题。");
    }

    @Test
    @DisplayName("buildMeta returns correct meta")
    void buildMeta_returnsCorrectMeta() {
        var meta = ragService.buildMeta("system prompt here", "user question");

        assertThat(meta.model()).isEqualTo("gpt-4o");
        assertThat(meta.contextLimit()).isEqualTo(128000);
        assertThat(meta.contextUsed()).isGreaterThan(0);
    }

    @Test
    @DisplayName("chatStream delegates to LlmService")
    void chatStream_delegatesToLlmService() {
        ragService.chatStream("system", "user", chunk -> {}, () -> {});
        verify(llmService).chatStreamWithDone(eq("system"), eq("user"), any(), any());
    }

    @Test
    @DisplayName("buildContext empty returns placeholder")
    void buildContext_empty_returnsPlaceholder() throws Exception {
        var method = RAGService.class.getDeclaredMethod("buildContext", List.class);
        method.setAccessible(true);
        String result = (String) method.invoke(ragService, List.of());
        assertThat(result).isEqualTo("（暂无参考文档）");
    }

    @Test
    @DisplayName("buildContext multiple results formats correctly")
    void buildContext_multipleResults_formatsCorrectly() throws Exception {
        var method = RAGService.class.getDeclaredMethod("buildContext", List.class);
        method.setAccessible(true);
        com.fasterxml.jackson.databind.node.ObjectNode meta = new ObjectMapper().createObjectNode();
        List<ChromaSearchResultVO> results = List.of(
                new ChromaSearchResultVO("id1", "First doc", 0.1, 0.9, meta),
                new ChromaSearchResultVO("id2", "Second doc", 0.2, 0.8, meta)
        );
        String context = (String) method.invoke(ragService, results);
        assertThat(context).contains("【文档 1】");
        assertThat(context).contains("First doc");
        assertThat(context).contains("【文档 2】");
        assertThat(context).contains("Second doc");
    }

    @Test
    @DisplayName("truncate null returns empty")
    void truncate_null_returnsEmpty() throws Exception {
        var method = RAGService.class.getDeclaredMethod("truncate", String.class, int.class);
        method.setAccessible(true);
        String result = (String) method.invoke(ragService, null, 10);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("truncate short text returns unchanged")
    void truncate_shortText_returnsUnchanged() throws Exception {
        var method = RAGService.class.getDeclaredMethod("truncate", String.class, int.class);
        method.setAccessible(true);
        String result = (String) method.invoke(ragService, "short", 10);
        assertThat(result).isEqualTo("short");
    }

    @Test
    @DisplayName("truncate long text truncates with ellipsis")
    void truncate_longText_truncatesWithEllipsis() throws Exception {
        var method = RAGService.class.getDeclaredMethod("truncate", String.class, int.class);
        method.setAccessible(true);
        String result = (String) method.invoke(ragService, "This is a very long text that exceeds the limit", 10);
        assertThat(result).hasSize(13);
        assertThat(result).endsWith("...");
    }
}
