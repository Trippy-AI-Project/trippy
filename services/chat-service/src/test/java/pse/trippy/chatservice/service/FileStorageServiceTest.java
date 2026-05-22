package pse.trippy.chatservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;
import pse.trippy.chatservice.dto.response.FileStorageResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
                .isInstanceOf(pse.trippy.chatservice.exception.UnsupportedFileTypeException.class)
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
                .isInstanceOf(pse.trippy.chatservice.exception.FileTooLargeException.class)
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

    @Test
    @DisplayName("storeFile generates a thumbnail for valid PNG images")
    void storeFileGeneratesThumbnailForImage() throws IOException {
        UUID tripId = UUID.randomUUID();
        byte[] png = createPng(400, 300);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", png);

        FileStorageResult result = fileStorageService.storeFile(tripId, file);

        assertThat(result.thumbnailUrl())
                .as("thumbnail URL should be populated for image uploads")
                .isNotNull()
                .startsWith(tripId + "/thumbs/")
                .endsWith("_thumb.jpg");

        Resource thumb = fileStorageService.getFile(result.thumbnailUrl());
        assertThat(thumb.exists()).isTrue();

        BufferedImage decoded = ImageIO.read(thumb.getFile());
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(200);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(200);
        // aspect ratio (4:3) preserved -> 200x150
        assertThat(decoded.getWidth()).isEqualTo(200);
        assertThat(decoded.getHeight()).isEqualTo(150);
    }

    @Test
    @DisplayName("storeFile does not generate a thumbnail for non-image files")
    void storeFileNoThumbnailForPdf() throws IOException {
        UUID tripId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "fake-pdf".getBytes());

        FileStorageResult result = fileStorageService.storeFile(tripId, file);

        assertThat(result.thumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("storeFile gracefully handles undecodable image bytes")
    void storeFileGracefulOnUndecodableImage() throws IOException {
        UUID tripId = UUID.randomUUID();
        // valid declared content type but garbage bytes -> ImageIO.read returns null
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.png", "image/png", "not-really-an-image".getBytes());

        FileStorageResult result = fileStorageService.storeFile(tripId, file);

        // upload still succeeds; thumbnail is just absent
        assertThat(result.fileUrl()).isNotNull();
        assertThat(result.thumbnailUrl()).isNull();
    }

    private static byte[] createPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
