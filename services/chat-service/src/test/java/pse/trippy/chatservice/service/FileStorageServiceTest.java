package pse.trippy.chatservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;
import pse.trippy.chatservice.dto.response.FileStorageResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
        fileStorageService.init();
    }

    @Test
    @DisplayName("storeFile stores a valid image and returns correct metadata")
    void storeFileSuccess() throws IOException {
        UUID tripId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake-image-data".getBytes());

        FileStorageResult result = fileStorageService.storeFile(tripId, file);

        assertThat(result.fileName()).isEqualTo("photo.png");
        assertThat(result.fileSize()).isEqualTo("fake-image-data".getBytes().length);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.fileUrl()).startsWith(tripId.toString() + "/");
        assertThat(result.fileUrl()).endsWith(".png");

        // Verify the file can be retrieved
        Resource resource = fileStorageService.getFile(result.fileUrl());
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("storeFile rejects files with disallowed content type")
    void storeFileInvalidType() {
        UUID tripId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "bad-data".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(tripId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    @DisplayName("storeFile rejects files exceeding the 10MB size limit")
    void storeFileExceedsSizeLimit() {
        UUID tripId = UUID.randomUUID();
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", largeContent);

        assertThatThrownBy(() -> fileStorageService.storeFile(tripId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    @DisplayName("storeFile stores PDF files successfully")
    void storeFilePdf() throws IOException {
        UUID tripId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf-content".getBytes());

        FileStorageResult result = fileStorageService.storeFile(tripId, file);

        assertThat(result.fileName()).isEqualTo("document.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.fileUrl()).endsWith(".pdf");
    }

    @Test
    @DisplayName("getFile throws for non-existent file")
    void getFileNotFound() {
        assertThatThrownBy(() -> fileStorageService.getFile("nonexistent/file.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }
}
