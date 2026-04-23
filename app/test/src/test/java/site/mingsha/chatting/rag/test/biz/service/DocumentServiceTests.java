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
import site.mingsha.chatting.rag.integration.config.RagProperties;
import site.mingsha.chatting.rag.biz.service.EmbeddingService;
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
    private EmbeddingService embeddingService;
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
                embeddingService,
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
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

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
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

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

        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) method.invoke(documentService, "");

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkText_shortText_returnsOneChunk() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        String shortText = "This is a short text.";
        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) method.invoke(documentService, shortText);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(shortText);
    }

    @Test
    void chunkText_longText_usesRecursiveSplitter() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        // Build text that clearly exceeds chunkSize (512 chars)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("Sentence ").append(i).append(" content here.\n");
        }
        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) method.invoke(documentService, sb.toString());

        // LangChain4j RecursiveCharacterTextSplitter should produce multiple chunks
        // when text exceeds chunkSize
        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void chunkText_multipleChunks_respectChunkSize() throws Exception {
        var method = DocumentService.class.getDeclaredMethod("chunkText", String.class);
        method.setAccessible(true);

        // With chunkSize=512 and overlap=128, no single chunk should exceed 512 chars
        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) method.invoke(documentService,
                "X".repeat(1000));

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(512);
        }
    }
}
