package pse.trippy.chatservice.dto.response;

import java.util.UUID;

/**
 * Response DTO for a file/image attachment on a chat message.
 */
public record MessageAttachmentResponse(
        UUID id,
        String fileName,
        String fileUrl,
        long fileSize,
        String contentType
) {
}
