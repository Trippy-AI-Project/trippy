package pse.trippy.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.paymentservice.dto.request.AddPaymentMethodRequest;
import pse.trippy.paymentservice.dto.response.PaymentMethodResponse;
import pse.trippy.paymentservice.exception.PaymentMethodNotFoundException;
import pse.trippy.paymentservice.model.entity.PaymentMethod;
import pse.trippy.paymentservice.repository.PaymentMethodRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public PaymentMethodResponse addPaymentMethod(UUID userId, AddPaymentMethodRequest request) {
        if (request.setAsDefault()) {
            paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        paymentMethodRepository.save(existing);
                    });
        }

        PaymentMethod method = PaymentMethod.builder()
                .userId(userId)
                .type("card")
                .brand(request.brand())
                .last4(request.last4())
                .expiryMonth(request.expiryMonth())
                .expiryYear(request.expiryYear())
                .isDefault(request.setAsDefault())
                .build();
        method = paymentMethodRepository.save(method);

        return toResponse(method);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods(UUID userId) {
        return paymentMethodRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deletePaymentMethod(UUID userId, UUID paymentMethodId) {
        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "Payment method not found: " + paymentMethodId));
        paymentMethodRepository.delete(method);
    }

    @Transactional
    public PaymentMethodResponse setDefaultPaymentMethod(UUID userId, UUID paymentMethodId) {
        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "Payment method not found: " + paymentMethodId));

        paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    paymentMethodRepository.save(existing);
                });

        method.setDefault(true);
        method = paymentMethodRepository.save(method);

        return toResponse(method);
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getBrand(),
                method.getLast4(),
                method.getExpiryMonth(),
                method.getExpiryYear(),
                method.isDefault(),
                method.getCreatedAt()
        );
    }
}
