package site.mingsha.chatting.rag.test.biz.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;
import site.mingsha.chatting.rag.biz.service.EmbeddingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTests {

    @Mock
    private EmbeddingClient embeddingClient;

    @org.junit.jupiter.api.Test
    void embed_delegatesToEmbeddingClient() {
        EmbeddingService service = new EmbeddingService(embeddingClient);
        float[] expected = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingClient.embed("some text")).thenReturn(expected);

        float[] result = service.embed("some text");

        assertThat(result).isEqualTo(expected);
        verify(embeddingClient).embed("some text");
    }
}
