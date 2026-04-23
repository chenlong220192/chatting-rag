package site.mingsha.chatting.rag.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.mingsha.chatting.rag.biz.model.dto.DocumentResponseDTO;
import site.mingsha.chatting.rag.integration.client.ChromaClient;
import site.mingsha.chatting.rag.integration.config.RagProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for document upload, text chunking, and ChromaDB indexing.
 *
 * <p>Handles the document ingestion pipeline:</p>
 * <ol>
 *   <li>Persists the uploaded file to local storage</li>
 *   <li>Parses content using LangChain4j TikaDocumentParser (PDF/Word) or direct read (text files)</li>
 *   <li>Splits content into overlapping chunks using LangChain4j DocumentSplitters</li>
 *   <li>Generates embeddings for each chunk via {@link EmbeddingService}</li>
 *   <li>Stores chunks and embeddings in ChromaDB via {@link ChromaClient}</li>
 * </ol>
 *
 * @see ChromaClient
 * @see EmbeddingService
 */
@Slf4j
@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Text-based formats: read directly as string.
     */
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".txt", ".md", ".csv", ".json", ".xml", ".html", ".htm"
    );

    /**
     * Binary formats: parsed via LangChain4j ApacheTikaDocumentParser.
     */
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
    );

    private static final Set<String> ALL_EXTENSIONS = Set.copyOf(
            new java.util.HashSet<>() {{
                addAll(TEXT_EXTENSIONS);
                addAll(BINARY_EXTENSIONS);
            }}
    );

    private static final Set<String> TEXT_MIME_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "text/csv",
            "text/html",
            "application/json",
            "application/xml",
            "text/xml"
    );

    private static final Set<String> BINARY_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private static final Set<String> ALL_MIME_TYPES = Set.copyOf(
            new java.util.HashSet<>() {{
                addAll(TEXT_MIME_TYPES);
                addAll(BINARY_MIME_TYPES);
            }}
    );

    private final ChromaClient chromaClient;
    private final EmbeddingService embeddingService;
    private final ApacheTikaDocumentParser tikaParser;
    private final Path uploadDir;
    private final ObjectMapper objectMapper;
    private final int chunkSize;
    private final int chunkOverlap;

    /**
     * Constructs the document service with required dependencies and configuration.
     *
     * @param uploadDirPath     local directory path for storing uploaded files
     * @param embeddingService embedding service
     * @param chromaClient     ChromaDB client
     * @param objectMapper     Jackson ObjectMapper for metadata serialization
     * @param ragProperties    RAG config (chunk size/overlap from rag.chunk.*)
     * @throws IOException if the upload directory cannot be created
     */
    public DocumentService(
            @Value("${upload.dir}") String uploadDirPath,
            EmbeddingService embeddingService,
            ChromaClient chromaClient,
            ObjectMapper objectMapper,
            RagProperties ragProperties
    ) throws IOException {
        this.embeddingService = embeddingService;
        this.chromaClient = chromaClient;
        this.objectMapper = objectMapper;
        this.uploadDir = Paths.get(uploadDirPath);
        this.chunkSize = ragProperties.chunk().size();
        this.chunkOverlap = ragProperties.chunk().overlap();
        this.tikaParser = new ApacheTikaDocumentParser();
        Files.createDirectories(uploadDir);
        log.info("[Document] 初始化完成，uploadDir={}, chunkSize={}, chunkOverlap={}, 使用LangChain4j Tika解析器",
                uploadDirPath, chunkSize, chunkOverlap);
    }

    /**
     * Uploads a file, parses it, indexes it into ChromaDB, and returns the document metadata.
     *
     * @param file the uploaded multipart file
     * @return a {@link DocumentResponseDTO} with the assigned document ID and status
     * @throws IOException if file I/O operations fail
     */
    public DocumentResponseDTO uploadAndIndex(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds the maximum allowed size of 10 MB: " + file.getOriginalFilename());
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is empty or not provided.");
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        boolean hasAllowedExtension = ALL_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasAllowedExtension) {
            throw new IllegalArgumentException(
                    "File type not supported. Allowed extensions: " + ALL_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALL_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "MIME type not supported: " + contentType + ". Allowed types: " + ALL_MIME_TYPES);
        }

        String docId = UUID.randomUUID().toString();
        String ext = "";
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = filename.substring(dotIdx);
        }
        String sanitizedFilename = docId + "_" + UUID.randomUUID().toString() + ext;
        Path filePath = uploadDir.resolve(sanitizedFilename);
        log.info("[Document] 开始上传文件，filename={}, docId={}", filename, docId);

        Files.write(filePath, file.getBytes());

        String content = parseContent(filePath, lowerName);
        log.debug("[Document] 文件解析完成，filename={}, 内容长度={}", filename, content.length());

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
            embeddings.add(embeddingService.embed(chunk));
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
     * @param docId the unique document identifier
     */
    public void deleteDocument(String docId) {
        log.info("[Document] 删除文档，docId={}", docId);
        chromaClient.deleteByDocId(docId);
    }

    /**
     * Parses file content using the appropriate parser.
     * Text files are read directly; binary files are parsed via LangChain4j TikaDocumentParser.
     */
    private String parseContent(Path filePath, String lowerName) throws IOException {
        if (TEXT_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            log.debug("[Document] 使用直接读取解析文本文件: {}", filePath);
            return Files.readString(filePath);
        }
        if (BINARY_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
            log.debug("[Document] 使用LangChain4j TikaDocumentParser解析二进制文件: {}", filePath);
            try (var is = Files.newInputStream(filePath)) {
                Document document = tikaParser.parse(is);
                return document.text();
            }
        }
        // Fallback: try as text
        return Files.readString(filePath);
    }

    /**
     * Splits text into overlapping chunks using LangChain4j DocumentSplitters.
     *
     * <p>Uses {@code RecursiveCharacterTextSplitter} which respects natural
     * boundaries (newlines, sentences) for semantically coherent chunks.
     * Overlapping windows preserve context across chunk boundaries.</p>
     *
     * @param text the full document content
     * @return a list of text chunks
     */
    private List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Document document = Document.from(text);
        DocumentSplitter splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
        List<TextSegment> segments = splitter.split(document);

        log.debug("[Document] LangChain4j 智能分块完成，片段数={}", segments.size());
        return segments.stream()
                .map(TextSegment::text)
                .toList();
    }
}
