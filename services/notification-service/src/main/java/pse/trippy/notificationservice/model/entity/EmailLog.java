package pse.trippy.notificationservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pse.trippy.notificationservice.model.enums.EmailStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_logs", schema = "notification_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "recipient", nullable = false)
    private String recipient;

    @NotBlank
    @Size(max = 500)
    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Size(max = 100)
    @Column(name = "template_name", length = 100)
    private String templateName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private EmailStatus status;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Size(max = 2000)
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        this.sentAt = Instant.now();
    }
}
