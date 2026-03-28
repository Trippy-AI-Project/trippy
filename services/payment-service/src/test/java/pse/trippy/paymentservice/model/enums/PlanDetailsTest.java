package pse.trippy.paymentservice.model.enums;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlanDetails enum.
 */
class PlanDetailsTest {

    @Test
    void testPremiumPlan() {
        PlanDetails plan = PlanDetails.PREMIUM;
        
        assertEquals("PREMIUM", plan.getPlanId());
        assertEquals(new BigDecimal("9.99"), plan.getPrice());
        assertTrue(plan.getFeatures().contains("10 trips"));
        assertTrue(plan.getFeatures().contains("AI itineraries"));
        assertTrue(plan.getFeatures().contains("priority support"));
    }

    @Test
    void testEnterprisePlan() {
        PlanDetails plan = PlanDetails.ENTERPRISE;
        
        assertEquals("ENTERPRISE", plan.getPlanId());
        assertEquals(new BigDecimal("29.99"), plan.getPrice());
        assertTrue(plan.getFeatures().contains("Unlimited trips"));
        assertTrue(plan.getFeatures().contains("advanced AI"));
        assertTrue(plan.getFeatures().contains("team features"));
    }

    @Test
    void testFromIdWithValidPlanId() {
        PlanDetails premiumPlan = PlanDetails.fromId("PREMIUM");
        assertNotNull(premiumPlan);
        assertEquals(PlanDetails.PREMIUM, premiumPlan);

        PlanDetails enterprisePlan = PlanDetails.fromId("ENTERPRISE");
        assertNotNull(enterprisePlan);
        assertEquals(PlanDetails.ENTERPRISE, enterprisePlan);
    }

    @Test
    void testFromIdWithInvalidPlanId() {
        PlanDetails plan = PlanDetails.fromId("INVALID_PLAN");
        assertNull(plan);
    }

    @Test
    void testFromIdWithNullPlanId() {
        PlanDetails plan = PlanDetails.fromId(null);
        assertNull(plan);
    }

    @Test
    void testAllPlansHavePrices() {
        for (PlanDetails plan : PlanDetails.values()) {
            assertNotNull(plan.getPrice());
            assertTrue(plan.getPrice().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void testAllPlansHaveFeatures() {
        for (PlanDetails plan : PlanDetails.values()) {
            assertNotNull(plan.getFeatures());
            assertFalse(plan.getFeatures().isEmpty());
        }
    }
}
