package pse.trippy.chatservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * File/image attachment associated with a chat message.
 * Designed for future use.
 */
@Entity
@Table(name = "message_attachments", schema = "chat_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 2048)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Relative URL of the auto-generated thumbnail (for image attachments).
     * {@code null} for non-image files or when thumbnail generation failed.
     */
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;
}
