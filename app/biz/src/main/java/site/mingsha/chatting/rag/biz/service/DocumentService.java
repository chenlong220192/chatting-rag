package site.mingsha.chatting.rag.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.client.EmbeddingClient;
import site.mingsha.chatting.rag.integration.config.RagProperties;
import site.mingsha.chatting.rag.biz.model.dto.DocumentResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for document upload, text chunking, and ChromaDB indexing.
 *
 * <p>Handles the document ingestion pipeline:</p>
 * <ol>
 *   <li>Persists the uploaded file to local storage</li>
 *   <li>Reads and splits the content into overlapping chunks</li>
 *   <li>Generates embeddings for each chunk via {@link EmbeddingClient}</li>
 *   <li>Stores chunks and embeddings in ChromaDB via {@link ChromaClient}</li>
 * </ol>
 *
 * @see ChromaClient
 * @see EmbeddingClient
 */
@Slf4j
@Service
public class DocumentService {

    private final EmbeddingClient embeddingClient;
    private final ChromaClient chromaClient;
    private final Path uploadDir;
    private final ObjectMapper objectMapper;
    private final int chunkSize;
    private final int chunkOverlap;

    /**
     * Constructs the document service with required dependencies and configuration.
     *
     * @param uploadDirPath       local directory path for storing uploaded files
     * @param embeddingClient     embedding service client
     * @param chromaClient        ChromaDB client
     * @param objectMapper        Jackson ObjectMapper for metadata serialization
     * @param ragProperties       RAG configuration containing chunk size/overlap settings
     * @throws IOException if the upload directory cannot be created
     */
    public DocumentService(
            @Value("${upload.dir}") String uploadDirPath,
            EmbeddingClient embeddingClient,
            ChromaClient chromaClient,
            ObjectMapper objectMapper,
            RagProperties ragProperties
    ) throws IOException {
        this.embeddingClient = embeddingClient;
        this.chromaClient = chromaClient;
        this.objectMapper = objectMapper;
        this.uploadDir = Paths.get(uploadDirPath);
        Files.createDirectories(uploadDir);
        this.chunkSize = ragProperties.chunk().size();
        this.chunkOverlap = ragProperties.chunk().overlap();
        log.info("[Document] 初始化完成，uploadDir={}, chunkSize={}, chunkOverlap={}",
                uploadDirPath, chunkSize, chunkOverlap);
    }

    /**
     * Uploads a file, indexes it into ChromaDB, and returns the document metadata.
     *
     * <p>The file content is read, split into overlapping chunks, embedded,
     * and stored in the vector database with metadata tracking document ID
     * and chunk index.</p>
     *
     * @param file the uploaded multipart file
     * @return a {@link DocumentResponseDTO} with the assigned document ID and status
     * @throws IOException if file I/O operations fail
     */
    public DocumentResponseDTO uploadAndIndex(MultipartFile file) throws IOException {
        String docId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename();
        Path filePath = uploadDir.resolve(docId + "_" + filename);
        log.info("[Document] 开始上传文件，filename={}, docId={}", filename, docId);

        Files.write(filePath, file.getBytes());

        String contentType = file.getContentType();
        String content = Files.readString(filePath);

        log.debug("[Document] 文件读取完成，filename={}, 内容长度={}", filename, content.length());

        List<String> chunks = chunkText(content);
        log.debug("[Document] 切片完成，filename={}, 片段数={}", filename, chunks.size());

        List<String> ids = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<float[]> embeddings = new ArrayList<>();
        List<String> metadatas = new ArrayList<>();

        for (String chunk : chunks) {
            if (chunk.trim().isEmpty()) continue;

            ids.add(docId + "_chunk_" + ids.size());
            texts.add(chunk);
            embeddings.add(embeddingClient.embed(chunk));
            metadatas.add(objectMapper.writeValueAsString(
                    java.util.Map.of("doc_id", docId, "filename", filename, "chunk_index", texts.size() - 1)
            ));
        }

        if (!ids.isEmpty()) {
            chromaClient.addDocuments(ids, texts, embeddings, metadatas);
        }

        log.info("[Document] 文件索引完成，filename={}, docId={}, 片段数={}",
                filename, docId, ids.size());
        return new DocumentResponseDTO(docId, filename, file.getSize(), "indexed");
    }

    /**
     * Deletes a document and all its associated vector entries from ChromaDB.
     *
     * <p>Currently performs a full database wipe. Implement collection-scoped
     * deletion for per-document removal.</p>
     *
     * @param docId the unique document identifier
     */
    public void deleteDocument(String docId) {
        log.info("[Document] 删除文档，docId={}", docId);
        chromaClient.deleteAll();
    }

    /**
     * Splits text into overlapping chunks of configured size.
     *
     * <p>Attempts to break at natural sentence boundaries (newline or full-width
     * Chinese period) to keep chunks semantically coherent. Overlapping windows
     * are used to preserve context across chunk boundaries.</p>
     *
     * @param text the full document content
     * @return a list of text chunks
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            if (end < text.length()) {
                int lastSeparator = Math.max(
                        text.lastIndexOf('\n', end),
                        text.lastIndexOf('。', end)
                );
                if (lastSeparator > start + chunkSize / 2) {
                    end = lastSeparator + 1;
                }
            }

            chunks.add(text.substring(start, end));
            start = end - chunkOverlap;
            int prevLen = chunks.isEmpty() ? 0 : chunks.get(chunks.size() - 1).length();
            if (start <= prevLen - chunkOverlap) {
                break;
            }
        }

        return chunks;
    }
}
