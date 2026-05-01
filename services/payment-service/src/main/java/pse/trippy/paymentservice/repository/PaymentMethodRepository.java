package pse.trippy.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.paymentservice.model.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PaymentMethod entity.
 * Provides database operations for payment methods.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Find all payment methods for a specific user.
     *
     * @param userId the user ID
     * @return list of payment methods for the user
     */
    List<PaymentMethod> findByUserId(UUID userId);

    /**
     * Find the default payment method for a user.
     *
     * @param userId the user ID
     * @return optional containing the default payment method
     */
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Find payment methods by user and brand.
     *
     * @param userId the user ID
     * @param brand the card brand (VISA, MASTERCARD, etc.)
     * @return list of payment methods matching criteria
     */
    List<PaymentMethod> findByUserIdAndBrand(UUID userId, String brand);
}
