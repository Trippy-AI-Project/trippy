package pse.trippy.paymentservice.model.enums;

import java.math.BigDecimal;
import java.util.List;

public enum PlanType {

    PREMIUM(
            new BigDecimal("9.99"),
            "EUR",
            "Premium Plan",
            List.of("Up to 10 trips", "AI itineraries", "Priority support")
    ),
    ENTERPRISE(
            new BigDecimal("29.99"),
            "EUR",
            "Enterprise Plan",
            List.of("Unlimited trips", "Advanced AI", "Team features")
    );

    private final BigDecimal price;
    private final String currency;
    private final String displayName;
    private final List<String> features;

    PlanType(BigDecimal price, String currency, String displayName, List<String> features) {
        this.price = price;
        this.currency = currency;
        this.displayName = displayName;
        this.features = features;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getFeatures() {
        return features;
    }
}
