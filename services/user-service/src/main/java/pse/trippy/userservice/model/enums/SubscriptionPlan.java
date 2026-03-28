package pse.trippy.userservice.model.enums;

/**
 * Subscription tier for a Trippy user.
 * Determines feature access and request quotas.
 */
public enum SubscriptionPlan {

    /** Free tier with limited AI trip generation quota. */
    FREE,

    /** Paid tier with increased quota and priority support. */
    PREMIUM,

    /** Enterprise tier with unlimited quota and dedicated support. */
    ENTERPRISE
}
