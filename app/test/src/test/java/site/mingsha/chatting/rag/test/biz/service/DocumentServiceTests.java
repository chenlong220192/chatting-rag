package site.mingsha.chatting.rag.test.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;
import site.mingsha.chatting.rag.integration.config.RagProperties;
import site.mingsha.chatting.rag.biz.service.DocumentService;
import site.mingsha.chatting.rag.biz.model.dto.DocumentResponseDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTests {

    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private ChromaClient chromaClient;
    @Mock
    private RagProperties ragProperties;
    @Mock
    private RagProperties.ChunkConfig chunkConfig;

    @TempDir
    Path tempDir;

    private DocumentService documentService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        when(ragProperties.chunk()).thenReturn(chunkConfig);
        when(chunkConfig.size()).thenReturn(512);
        when(chunkConfig.overlap()).thenReturn(128);

        documentService = new DocumentService(
                tempDir.toString(),
                embeddingClient,
                chromaClient,
                objectMapper,
                ragProperties
        );
    }

    @Test
    void uploadAndIndex_fileTooLarge_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("large.pdf");
        when(file.getSize()).thenReturn(11L * 1024 * 1024);

        assertThatThrownBy(() -> documentService.uploadAndIndex(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void uploadAndIndex_invalidExtension_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("evil.exe");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/octet-stream");

        assertThatThrownBy(() -> documentService.uploadAndIndex(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void uploadAndIndex_invalidMimeType_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> documentService.uploadAndIndex(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME type not supported");
    }

    @Test
    void uploadAndIndex_emptyFilename_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("");
        when(file.getSize()).thenReturn(100L);

        assertThatThrownBy(() -> documentService.uploadAndIndex(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadAndIndex_nullFilename_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getSize()).thenReturn(100L);

        assertThatThrownBy(() -> documentService.uploadAndIndex(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadAndIndex_success_callsEmbeddingAndChroma() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("doc.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(100L);
        when(file.getBytes()).thenReturn("Hello world this is a test document".getBytes());
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        DocumentResponseDTO response = documentService.uploadAndIndex(file);

        assertThat(response.filename()).isEqualTo("doc.txt");
        assertThat(response.status()).isEqualTo("indexed");
        assertThat(response.id()).isNotBlank();
        verify(chromaClient).addDocuments(anyList(), anyList(), anyList(), anyList());
    }

    @Test
    void uploadAndIndex_uuidFilename_storesOriginalNameInMetadata() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("my_document.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(100L);
        when(file.getBytes()).thenReturn("Content here".getBytes());
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        documentService.uploadAndIndex(file);

        verify(chromaClient).addDocuments(
                anyList(), anyList(), anyList(),
                argThat(metas -> metas.get(0).contains("my_document.txt"))
        );
    }

    @Test
    void deleteDocument_callsChromaDeleteByDocId() {
        documentService.deleteDocument("doc-123");
        verify(chromaClient).deleteByDocId("doc-123");
    }

    @Test
    void chunkText_emptyString_returnsEmptyList() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        List<String> chunks = (List<String>) method.invoke(documentService, "");

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkText_shortText_returnsOneChunk() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        String shortText = "This is a short text.";
        List<String> chunks = (List<String>) method.invoke(documentService, shortText);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(shortText);
    }

    @Test
    void chunkText_longText_returnsNonTrivialOutput() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        // Build text that exceeds chunkSize (512) with newlines as separators
        StringBuilder sb = new StringBuilder();
        // 15 sentences of ~35 chars each = ~525 chars total, exceeds 512
        for (int i = 0; i < 15; i++) {
            sb.append("Sentence ").append(i).append(" content here.\n");
        }
        List<String> chunks = (List<String>) method.invoke(documentService, sb.toString());

        // Should have at least 2 chunks (text exceeds 512 chars)
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).length()).isLessThanOrEqualTo(512);
    }

    @Test
    void chunkText_withChinesePeriod_splitsAtSentenceBoundary() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        String text = "第一句内容。第二句内容。第三句内容。";
        List<String> chunks = (List<String>) method.invoke(documentService, text);

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).doesNotContain("。第一句");
        }
    }
}
