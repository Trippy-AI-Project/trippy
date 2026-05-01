package pse.trippy.chatservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for chat messages broadcast to subscribers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private UUID id;
    private UUID senderId;
    private String senderDisplayName;
    private String content;
    private String type;
    private Instant createdAt;
    private boolean edited;
    private MessageAttachmentResponse attachment;
}
