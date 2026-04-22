package site.mingsha.chatting.rag.test.biz.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.model.vo.ChromaSearchResultVO;
import site.mingsha.chatting.rag.biz.service.ChromaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChromaServiceTests {

    @Mock
    private ChromaClient chromaClient;

    @org.junit.jupiter.api.Test
    void addDocuments_delegatesToChromaClient() {
        ChromaService service = new ChromaService(chromaClient);

        service.addDocuments(
                List.of("id1"),
                List.of("text1"),
                List.of(new float[]{0.1f}),
                List.of("{}")
        );

        verify(chromaClient).addDocuments(
                eq(List.of("id1")),
                eq(List.of("text1")),
                anyList(),
                eq(List.of("{}"))
        );
    }

    @org.junit.jupiter.api.Test
    void query_delegatesToChromaClient() {
        ChromaService service = new ChromaService(chromaClient);
        ChromaSearchResultVO expected = new ChromaSearchResultVO(
                "id1", "content", 0.1, 0.9, null
        );
        when(chromaClient.query(new float[]{0.1f}, 5))
                .thenReturn(List.of(expected));

        var results = service.query(new float[]{0.1f}, 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo("id1");
    }

    @org.junit.jupiter.api.Test
    void deleteAll_delegatesToChromaClient() {
        ChromaService service = new ChromaService(chromaClient);

        service.deleteAll();

        verify(chromaClient).deleteAll();
    }
}
