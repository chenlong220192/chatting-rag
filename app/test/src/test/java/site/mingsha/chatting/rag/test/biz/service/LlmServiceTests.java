package site.mingsha.chatting.rag.test.biz.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.mingsha.chatting.rag.integration.client.SpringAiLlmClient;
import site.mingsha.chatting.rag.biz.service.LlmService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmServiceTests {

    @Mock
    private SpringAiLlmClient springAiLlmClient;

    @org.junit.jupiter.api.Test
    void chat_delegatesToSpringAiLlmClient() {
        LlmService service = new LlmService(springAiLlmClient);
        when(springAiLlmClient.chat("system", "user")).thenReturn("LLM response");

        String result = service.chat("system", "user");

        assertThat(result).isEqualTo("LLM response");
        verify(springAiLlmClient).chat("system", "user");
    }

    @org.junit.jupiter.api.Test
    void chatStreamWithDone_delegatesToSpringAiLlmClient() {
        LlmService service = new LlmService(springAiLlmClient);

        service.chatStreamWithDone("system", "user", chunk -> {}, () -> {});

        verify(springAiLlmClient).chatStreamWithDone(eq("system"), eq("user"), any(), any());
    }
}
