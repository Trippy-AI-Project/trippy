package pse.trippy.paymentservice.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.PlanType;
import pse.trippy.paymentservice.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("TransactionRepository")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("save and retrieve transaction")
    void saveAndRetrieve() {
        UUID userId = UUID.randomUUID();
        Transaction txn = transactionRepository.save(Transaction.builder()
                .userId(userId)
                .planId(PlanType.PREMIUM)
                .amount(new BigDecimal("9.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .build());

        assertThat(txn.getId()).isNotNull();
        assertThat(txn.getCreatedAt()).isNotNull();
        assertThat(txn.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc returns transactions for user")
    void findByUserId() {
        UUID userId = UUID.randomUUID();
        transactionRepository.save(Transaction.builder()
                .userId(userId).planId(PlanType.PREMIUM)
                .amount(new BigDecimal("9.99")).currency("EUR")
                .status(TransactionStatus.COMPLETED).build());
        transactionRepository.save(Transaction.builder()
                .userId(userId).planId(PlanType.ENTERPRISE)
                .amount(new BigDecimal("29.99")).currency("EUR")
                .status(TransactionStatus.COMPLETED).build());
        // Different user
        transactionRepository.save(Transaction.builder()
                .userId(UUID.randomUUID()).planId(PlanType.PREMIUM)
                .amount(new BigDecimal("9.99")).currency("EUR")
                .status(TransactionStatus.COMPLETED).build());

        List<Transaction> results = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(t -> t.getUserId().equals(userId));
    }
}
