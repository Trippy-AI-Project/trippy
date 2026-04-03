package pse.trippy.paymentservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userId = UUID.randomUUID();

        transactionRepository.save(Transaction.builder()
                .userId(userId)
                .planId("premium_monthly")
                .amount(new BigDecimal("9.99"))
                .type(TransactionType.SUBSCRIPTION)
                .status(TransactionStatus.COMPLETED)
                .description("Monthly subscription")
                .build());

        transactionRepository.save(Transaction.builder()
                .userId(userId)
                .planId("premium_monthly")
                .amount(new BigDecimal("9.99"))
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .description("Refund request")
                .build());

        transactionRepository.save(Transaction.builder()
                .userId(UUID.randomUUID())
                .planId("premium_yearly")
                .amount(new BigDecimal("99.99"))
                .type(TransactionType.SUBSCRIPTION)
                .status(TransactionStatus.COMPLETED)
                .description("Another user subscription")
                .build());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsUserTransactions() {
        Page<Transaction> page = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(t -> t.getUserId().equals(userId));
    }

    @Test
    void findByUserIdAndStatus_returnsMatchingTransactions() {
        List<Transaction> completed = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.COMPLETED);

        assertThat(completed).hasSize(1);
        assertThat(completed.get(0).getType()).isEqualTo(TransactionType.SUBSCRIPTION);
    }

    @Test
    void findByStatus_returnsAllWithStatus() {
        List<Transaction> completed = transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        assertThat(completed).hasSize(2);
    }

    @Test
    void findByUserIdAndStatus_returnsEmptyForNoMatch() {
        List<Transaction> failed = transactionRepository.findByUserIdAndStatus(userId, TransactionStatus.FAILED);

        assertThat(failed).isEmpty();
    }

    @Test
    void save_persistsTransactionWithDefaults() {
        Transaction txn = Transaction.builder()
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("19.99"))
                .type(TransactionType.SUBSCRIPTION)
                .build();

        Transaction saved = transactionRepository.save(txn);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
