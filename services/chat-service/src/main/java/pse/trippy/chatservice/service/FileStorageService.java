package pse.trippy.chatservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pse.trippy.chatservice.dto.response.FileStorageResult;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
 *
 * <p>For image uploads, a 200x200 (max bounding box, aspect preserved) JPEG thumbnail is
 * generated alongside the original under a {@code thumbs/} sub-directory.</p>
 */
@Service
@Slf4j
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    /** Maximum side length (in pixels) for generated thumbnails. */
    static final int THUMBNAIL_MAX_DIMENSION = 200;

    /** Thumbnails are written here, relative to each trip directory. */
    static final String THUMBNAIL_SUBDIR = "thumbs";

    /** Format used for generated thumbnails (always JPEG for predictable size). */
    private static final String THUMBNAIL_FORMAT = "jpg";

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
     * Stores an uploaded file under {@code {tripId}/{uuid}.{ext}}. For image content types
     * a {@value #THUMBNAIL_MAX_DIMENSION}x{@value #THUMBNAIL_MAX_DIMENSION} JPEG thumbnail is
     * additionally written under {@code {tripId}/thumbs/{uuid}_thumb.jpg}.
     *
     * @return metadata about the stored file (and thumbnail, if generated)
     * @throws IllegalArgumentException if the file type is not allowed or the file exceeds the size limit
     * @throws IOException              if an I/O error occurs while storing the file
     */
    public FileStorageResult storeFile(UUID tripId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new pse.trippy.chatservice.exception.FileTooLargeException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new pse.trippy.chatservice.exception.UnsupportedFileTypeException(
                    "File type not allowed: " + contentType);
        }

        String extension = getExtension(file.getOriginalFilename());
        String baseName = UUID.randomUUID().toString();
        String storedName = baseName + "." + extension;

        Path tripDir = uploadDir.resolve(tripId.toString());
        Files.createDirectories(tripDir);

        Path target = tripDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = tripId + "/" + storedName;
        String thumbnailUrl = null;
        if (isImageContentType(contentType)) {
            thumbnailUrl = generateThumbnail(tripDir, tripId, baseName, target);
        }

        log.info("Stored file {} ({} bytes, {}) at {}{}", file.getOriginalFilename(),
                file.getSize(), contentType, fileUrl,
                thumbnailUrl != null ? " (thumb: " + thumbnailUrl + ")" : "");

        return new FileStorageResult(fileUrl, file.getOriginalFilename(), file.getSize(),
                contentType, thumbnailUrl);
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

    /**
     * Generates a thumbnail for the given image file. Failures are logged and swallowed
     * so they never block the original upload.
     *
     * @return the relative thumbnail URL, or {@code null} if generation failed
     */
    private String generateThumbnail(Path tripDir, UUID tripId, String baseName, Path source) {
        try {
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null) {
                // No ImageIO reader available (e.g. webp on a stock JDK).
                log.debug("No ImageIO reader available for {} - skipping thumbnail", source);
                return null;
            }

            int srcW = original.getWidth();
            int srcH = original.getHeight();
            double scale = Math.min(
                    (double) THUMBNAIL_MAX_DIMENSION / srcW,
                    (double) THUMBNAIL_MAX_DIMENSION / srcH);
            if (scale > 1.0) {
                scale = 1.0; // never up-scale
            }
            int dstW = Math.max(1, (int) Math.round(srcW * scale));
            int dstH = Math.max(1, (int) Math.round(srcH * scale));

            BufferedImage thumb = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(original, 0, 0, dstW, dstH, null);
            } finally {
                g.dispose();
            }

            Path thumbDir = tripDir.resolve(THUMBNAIL_SUBDIR);
            Files.createDirectories(thumbDir);
            String thumbName = baseName + "_thumb." + THUMBNAIL_FORMAT;
            Path thumbPath = thumbDir.resolve(thumbName);
            if (!ImageIO.write(thumb, THUMBNAIL_FORMAT, thumbPath.toFile())) {
                log.warn("ImageIO had no writer for format {} - thumbnail not saved", THUMBNAIL_FORMAT);
                return null;
            }
            return tripId + "/" + THUMBNAIL_SUBDIR + "/" + thumbName;
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to generate thumbnail for {}", source, e);
            return null;
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
