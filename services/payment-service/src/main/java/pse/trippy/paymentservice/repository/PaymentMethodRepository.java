package pse.trippy.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pse.trippy.paymentservice.model.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByUserId(UUID userId);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);
}
