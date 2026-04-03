package pse.trippy.paymentservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_methods",
        schema = "payment_schema",
        indexes = @Index(name = "idx_payment_methods_user_id", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Size(max = 4)
    @Column(name = "last4", length = 4)
    private String last4;

    @Size(max = 20)
    @Column(name = "brand", length = 20)
    private String brand;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
