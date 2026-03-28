package pse.trippy.paymentservice.model.enums;

import java.math.BigDecimal;

/**
 * Enum for subscription plans with pricing and features.
 */
public enum PlanDetails {
    PREMIUM("PREMIUM", new BigDecimal("9.99"), "Up to 10 trips, AI itineraries, priority support"),
    ENTERPRISE("ENTERPRISE", new BigDecimal("29.99"), "Unlimited trips, advanced AI, team features");

    private final String planId;
    private final BigDecimal price;
    private final String features;

    PlanDetails(String planId, BigDecimal price, String features) {
        this.planId = planId;
        this.price = price;
        this.features = features;
    }

    public String getPlanId() {
        return planId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getFeatures() {
        return features;
    }

    /**
     * Get plan by ID.
     *
     * @param planId the plan ID
     * @return PlanDetails if found, null otherwise
     */
    public static PlanDetails fromId(String planId) {
        for (PlanDetails plan : PlanDetails.values()) {
            if (plan.planId.equals(planId)) {
                return plan;
            }
        }
        return null;
    }
}
