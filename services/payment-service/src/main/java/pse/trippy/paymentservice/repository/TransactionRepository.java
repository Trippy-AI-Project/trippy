package pse.trippy.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.TransactionStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction entity.
 * Provides database operations for transactions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all transactions for a specific user.
     *
     * @param userId the user ID
     * @return list of transactions for the user
     */
    List<Transaction> findByUserId(UUID userId);

    /**
     * Find transactions by user and status.
     *
     * @param userId the user ID
     * @param status the transaction status
     * @return list of transactions matching criteria
     */
    List<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status);

    /**
     * Find transactions by plan ID.
     *
     * @param planId the plan ID
     * @return list of transactions for the plan
     */
    List<Transaction> findByPlanId(String planId);
}
