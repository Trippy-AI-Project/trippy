package pse.trippy.chatservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pse.trippy.chatservice.dto.response.FileStorageResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for storing and retrieving chat file/image attachments on the local filesystem.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final Path uploadDir;

    public FileStorageService(@Value("${trippy.chat.upload-dir:./uploads/chat}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialised at {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Stores an uploaded file under {@code {tripId}/{uuid}.{ext}}.
     *
     * @return metadata about the stored file
     * @throws IllegalArgumentException if the file type is not allowed or the file exceeds the size limit
     * @throws IOException              if an I/O error occurs while storing the file
     */
    public FileStorageResult storeFile(UUID tripId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        String extension = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;

        Path tripDir = uploadDir.resolve(tripId.toString());
        Files.createDirectories(tripDir);

        Path target = tripDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = tripId + "/" + storedName;

        log.info("Stored file {} ({} bytes, {}) at {}", file.getOriginalFilename(), file.getSize(), contentType, fileUrl);

        return new FileStorageResult(fileUrl, file.getOriginalFilename(), file.getSize(), contentType);
    }

    /**
     * Loads a previously stored file as a Spring {@link Resource}.
     *
     * @param filePath relative path in the form {@code {tripId}/{filename}}
     */
    public Resource getFile(String filePath) {
        try {
            Path file = uploadDir.resolve(filePath).normalize();
            if (!file.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new IllegalArgumentException("File not found: " + filePath);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("File not found: " + filePath, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
