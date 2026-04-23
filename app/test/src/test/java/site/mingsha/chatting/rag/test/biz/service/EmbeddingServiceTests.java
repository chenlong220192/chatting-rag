package site.mingsha.chatting.rag.test.biz.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.mingsha.chatting.rag.integration.client.SpringAiEmbeddingClient;
import site.mingsha.chatting.rag.biz.service.EmbeddingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTests {

    @Mock
    private SpringAiEmbeddingClient springAiEmbeddingClient;

    @org.junit.jupiter.api.Test
    void embed_delegatesToSpringAiEmbeddingClient() {
        EmbeddingService service = new EmbeddingService(springAiEmbeddingClient);
        float[] expected = new float[]{0.1f, 0.2f, 0.3f};
        when(springAiEmbeddingClient.embed("some text")).thenReturn(expected);

        float[] result = service.embed("some text");

        assertThat(result).isEqualTo(expected);
        verify(springAiEmbeddingClient).embed("some text");
    }
}
