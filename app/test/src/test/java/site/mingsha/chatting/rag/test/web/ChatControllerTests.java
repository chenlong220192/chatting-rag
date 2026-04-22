package site.mingsha.chatting.rag.test.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.mingsha.chatting.rag.biz.model.dto.ChatMetaDTO;
import site.mingsha.chatting.rag.biz.model.dto.ChatResponseDTO;
import site.mingsha.chatting.rag.biz.service.RAGService;
import site.mingsha.chatting.rag.web.ChatController;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTests {

    @Mock
    private RAGService ragService;

    @Mock
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatController controller = new ChatController(ragService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void chatStream_success_invokesRagPipelineInOrder() throws Exception {
        List<ChatResponseDTO.Reference> refs = List.of(
                new ChatResponseDTO.Reference("chunk one", "doc-1", 0.95),
                new ChatResponseDTO.Reference("chunk two", "doc-2", 0.88)
        );
        ChatMetaDTO meta = new ChatMetaDTO("gpt-4o", 512, 4096);

        given(ragService.getReferences("hello")).willReturn(refs);
        given(ragService.buildSystemPrompt("hello")).willReturn("system prompt");
        given(ragService.buildMeta(eq("system prompt"), eq("hello"))).willReturn(meta);
        given(objectMapper.writeValueAsString(refs)).willReturn("[{\"content\":\"chunk one\"}]");
        given(objectMapper.writeValueAsString(meta)).willReturn("{\"model\":\"gpt-4o\"}");

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onComplete = invocation.getArgument(3);
            onChunk.accept("Hello from LLM");
            onComplete.run();
            latch.countDown();
            return null;
        }).when(ragService).chatStream(anyString(), anyString(), any(), any());

        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        result.getAsyncResult(3000);

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        verify(ragService).getReferences("hello");
        verify(ragService).buildSystemPrompt("hello");
        verify(ragService).buildMeta(eq("system prompt"), eq("hello"));
        verify(ragService).chatStream(anyString(), anyString(), any(), any());
        verify(objectMapper).writeValueAsString(refs);
        verify(objectMapper).writeValueAsString(meta);
    }

    @Test
    void chatStream_getReferencesException_doesNotCallBuildSystemPromptOrChatStream() throws Exception {
        given(ragService.getReferences("hello"))
                .willThrow(new RuntimeException("ChromaDB unavailable"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(request().asyncStarted());

        verify(ragService).getReferences("hello");
    }

    @Test
    void chatStream_buildSystemPromptException_doesNotCallChatStream() throws Exception {
        given(ragService.getReferences("hello")).willReturn(List.of());
        given(objectMapper.writeValueAsString(List.of())).willReturn("[]");
        given(ragService.buildSystemPrompt("hello"))
                .willThrow(new RuntimeException("Embedding timeout"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(request().asyncStarted());

        verify(ragService).getReferences("hello");
        verify(ragService).buildSystemPrompt("hello");
    }
}
