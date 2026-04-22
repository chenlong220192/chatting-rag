package site.mingsha.chatting.rag.web;

import lombok.extern.slf4j.Slf4j;
import site.mingsha.chatting.rag.biz.model.dto.DocumentResponseDTO;
import site.mingsha.chatting.rag.biz.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller handling document upload and management operations.
 *
 * <p>Provides endpoints to upload documents for RAG indexing and to delete
 * previously indexed documents.</p>
 *
 * @see DocumentService
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Constructs the controller with the required service dependency.
     *
     * @param documentService document processing and indexing service
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Uploads a document and indexes it into the vector store.
     *
     * <p>The document is read, chunked, embedded, and stored in ChromaDB.
     * Supported formats depend on the {@link DocumentService} implementation.</p>
     *
     * @param file the multipart file to upload (max size enforced by Spring config)
     * @return a {@link DocumentResponseDTO} containing the assigned document ID and status
     * @throws Exception if file reading, chunking, or indexing fails
     */
    @PostMapping
    public DocumentResponseDTO upload(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("[Document] 收到上传请求，filename={}, size={}", file.getOriginalFilename(), file.getSize());
        try {
            DocumentResponseDTO resp = documentService.uploadAndIndex(file);
            log.info("[Document] 上传成功，filename={}, docId={}", resp.filename(), resp.id());
            return resp;
        } catch (Exception e) {
            log.error("[Document] 上传失败，filename={}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deletes a document and its associated vector entries from the store.
     *
     * @param id the unique document identifier returned during upload
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        log.info("[Document] 收到删除请求，docId={}", id);
        documentService.deleteDocument(id);
    }
}
