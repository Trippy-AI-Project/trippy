package pse.trippy.paymentservice.dto.request;

public record CancelSubscriptionRequest(
        boolean cancelImmediately
) {
}
