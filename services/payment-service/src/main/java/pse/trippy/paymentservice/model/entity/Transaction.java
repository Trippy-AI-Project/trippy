package pse.trippy.paymentservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        schema = "payment_schema",
        indexes = @Index(name = "idx_transactions_user_id", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Size(max = 50)
    @Column(name = "plan_id", length = 50)
    private String planId;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
