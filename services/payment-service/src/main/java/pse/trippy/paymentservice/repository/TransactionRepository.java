package pse.trippy.paymentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.TransactionStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status);

    List<Transaction> findByStatus(TransactionStatus status);
}
