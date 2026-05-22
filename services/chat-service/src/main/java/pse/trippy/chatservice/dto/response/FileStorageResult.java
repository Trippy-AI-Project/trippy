package pse.trippy.chatservice.dto.response;

/**
 * Result of storing a file via {@link pse.trippy.chatservice.service.FileStorageService}.
 */
public record FileStorageResult(
        String fileUrl,
        String fileName,
        long fileSize,
        String contentType,
        String thumbnailUrl
) {
}
