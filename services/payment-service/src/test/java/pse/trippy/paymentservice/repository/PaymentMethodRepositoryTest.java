package pse.trippy.paymentservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.paymentservice.model.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentMethodRepositoryTest {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        paymentMethodRepository.deleteAll();
        userId = UUID.randomUUID();

        paymentMethodRepository.save(PaymentMethod.builder()
                .userId(userId)
                .type("card")
                .last4("4242")
                .brand("Visa")
                .isDefault(true)
                .build());

        paymentMethodRepository.save(PaymentMethod.builder()
                .userId(userId)
                .type("card")
                .last4("1234")
                .brand("Mastercard")
                .isDefault(false)
                .build());

        paymentMethodRepository.save(PaymentMethod.builder()
                .userId(UUID.randomUUID())
                .type("sepa_debit")
                .last4("5678")
                .brand(null)
                .isDefault(true)
                .build());
    }

    @Test
    void findByUserId_returnsUserPaymentMethods() {
        List<PaymentMethod> methods = paymentMethodRepository.findByUserId(userId);

        assertThat(methods).hasSize(2);
        assertThat(methods).allMatch(m -> m.getUserId().equals(userId));
    }

    @Test
    void findByUserIdAndIsDefaultTrue_returnsDefaultMethod() {
        Optional<PaymentMethod> defaultMethod = paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId);

        assertThat(defaultMethod).isPresent();
        assertThat(defaultMethod.get().getLast4()).isEqualTo("4242");
        assertThat(defaultMethod.get().isDefault()).isTrue();
    }

    @Test
    void findByUserIdAndIsDefaultTrue_returnsEmptyWhenNoDefault() {
        UUID noDefaultUser = UUID.randomUUID();
        paymentMethodRepository.save(PaymentMethod.builder()
                .userId(noDefaultUser)
                .type("card")
                .last4("9999")
                .brand("Amex")
                .isDefault(false)
                .build());

        Optional<PaymentMethod> result = paymentMethodRepository.findByUserIdAndIsDefaultTrue(noDefaultUser);

        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsPaymentMethodWithDefaults() {
        PaymentMethod method = PaymentMethod.builder()
                .userId(UUID.randomUUID())
                .type("card")
                .last4("0000")
                .brand("Visa")
                .build();

        PaymentMethod saved = paymentMethodRepository.save(method);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isDefault()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
