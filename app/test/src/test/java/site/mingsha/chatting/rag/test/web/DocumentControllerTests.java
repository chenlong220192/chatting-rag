package site.mingsha.chatting.rag.test.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.mingsha.chatting.rag.biz.model.dto.DocumentResponseDTO;
import site.mingsha.chatting.rag.biz.service.DocumentService;
import site.mingsha.chatting.rag.web.DocumentController;
import site.mingsha.chatting.rag.web.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTests {

    @Mock
    private DocumentService documentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocumentController controller = new DocumentController(documentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_success() throws Exception {
        DocumentResponseDTO dto = new DocumentResponseDTO("doc-123", "test-file.txt", 1024L, "indexed");
        given(documentService.uploadAndIndex(any())).willReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test-file.txt", MediaType.TEXT_PLAIN_VALUE, "Hello".getBytes());

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("\"id\":\"doc-123\"");
                    assertThat(json).contains("\"filename\":\"test-file.txt\"");
                    assertThat(json).contains("\"status\":\"indexed\"");
                });

        verify(documentService).uploadAndIndex(any());
    }

    @Test
    void upload_exception() throws Exception {
        given(documentService.uploadAndIndex(any()))
                .willThrow(new RuntimeException("file too large"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[1024]);

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/documents/doc-123"))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument("doc-123");
    }
}
