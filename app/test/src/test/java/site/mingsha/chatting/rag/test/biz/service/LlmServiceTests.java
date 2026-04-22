package site.mingsha.chatting.rag.test.biz.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.mingsha.chatting.rag.integration.client.LlmClient;
import site.mingsha.chatting.rag.biz.service.LlmService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceTests {

    @Mock
    private LlmClient llmClient;

    @org.junit.jupiter.api.Test
    void chat_delegatesToLlmClient() {
        LlmService service = new LlmService(llmClient);
        when(llmClient.chat("system", "user")).thenReturn("LLM response");

        String result = service.chat("system", "user");

        assertThat(result).isEqualTo("LLM response");
        verify(llmClient).chat("system", "user");
    }

    @org.junit.jupiter.api.Test
    void chatStreamWithDone_delegatesToLlmClient() {
        LlmService service = new LlmService(llmClient);

        service.chatStreamWithDone("system", "user", chunk -> {}, () -> {});

        verify(llmClient).chatStreamWithDone(eq("system"), eq("user"), any(), any());
    }
}
