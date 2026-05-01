"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  Check,
  Crown,
  Loader2,
  CreditCard,
  Plus,
  Trash2,
  Sparkles,
  Building2,
  Zap,
} from "lucide-react";
import { motion } from "framer-motion";
import { GlassCard, Button, Badge } from "@/components/ui";
import { paymentsApi, type SubscriptionInfo, type PaymentMethod } from "@/lib/api";
import { useToast } from "@/lib/toast";
import { cn } from "@/lib/utils";

interface PlanInfo {
  id: string;
  planId: string;
  name: string;
  price: number;
  interval: string;
  icon: typeof Crown;
  features: string[];
  tripsLimit: number;
  aiGenerationsLimit: number;
  highlight?: boolean;
}

const PLANS: PlanInfo[] = [
  {
    id: "free",
    planId: "",
    name: "Free",
    price: 0,
    interval: "forever",
    icon: Zap,
    features: ["Up to 3 trips", "Basic itinerary", "Trip chat", "5 AI generations/month"],
    tripsLimit: 3,
    aiGenerationsLimit: 5,
  },
  {
    id: "premium",
    planId: "premium_monthly",
    name: "Premium",
    price: 9.99,
    interval: "month",
    icon: Crown,
    highlight: true,
    features: [
      "Unlimited trips",
      "AI-powered itineraries",
      "File attachments in chat",
      "50 AI generations/month",
      "Priority support",
    ],
    tripsLimit: -1,
    aiGenerationsLimit: 50,
  },
  {
    id: "pro",
    planId: "enterprise_monthly",
    name: "Pro",
    price: 29.99,
    interval: "month",
    icon: Building2,
    features: [
      "Everything in Premium",
      "Unlimited AI generations",
      "Custom branding",
      "Team management",
      "API access",
      "Dedicated support",
    ],
    tripsLimit: -1,
    aiGenerationsLimit: -1,
  },
];

export default function PaymentPage() {
  const searchParams = useSearchParams();
  const { addToast } = useToast();

  const [subscription, setSubscription] = useState<SubscriptionInfo | null>(null);
  const [methods, setMethods] = useState<PaymentMethod[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkoutLoading, setCheckoutLoading] = useState<string>("");
  const [cancelLoading, setCancelLoading] = useState(false);
  const [showAddCard, setShowAddCard] = useState(false);

  // Check success param
  useEffect(() => {
    if (searchParams.get("success") === "true") {
      addToast("Subscription updated successfully!", "success");
    }
  }, [searchParams, addToast]);

  // Load data
  useEffect(() => {
    setLoading(true);
    Promise.all([
      paymentsApi.getSubscription().catch(() => null),
      paymentsApi.getMethods().catch(() => []),
    ]).then(([sub, meth]) => {
      setSubscription(sub);
      setMethods(meth);
      setLoading(false);
    });
  }, []);

  async function handleCheckout(planId: string) {
    if (methods.length === 0) {
      addToast("Please add a payment method first", "error");
      setShowAddCard(true);
      return;
    }
    const defaultMethod = methods.find((m) => m.isDefault) ?? methods[0];
    setCheckoutLoading(planId);
    try {
      await paymentsApi.checkout(planId, defaultMethod.paymentMethodId);
      addToast("Subscription updated!", "success");
      // Refresh data
      const sub = await paymentsApi.getSubscription().catch(() => null);
      setSubscription(sub);
    } catch {
      addToast("Payment failed. Please try again.", "error");
    } finally {
      setCheckoutLoading("");
    }
  }

  async function handleCancel() {
    if (!confirm("Are you sure you want to cancel your subscription? It will remain active until the end of your billing period.")) return;
    setCancelLoading(true);
    try {
      const sub = await paymentsApi.cancelSubscription(false);
      setSubscription(sub);
      addToast("Subscription will be cancelled at the end of this billing period", "info");
    } catch {
      addToast("Failed to cancel subscription", "error");
    } finally {
      setCancelLoading(false);
    }
  }

  async function handleAddCard(e: React.FormEvent) {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const data = new FormData(form);
    try {
      const method = await paymentsApi.addMethod({
        brand: data.get("brand") as string,
        last4: data.get("last4") as string,
        expiryMonth: parseInt(data.get("expiryMonth") as string),
        expiryYear: parseInt(data.get("expiryYear") as string),
        setAsDefault: true,
      });
      setMethods((prev) => [...prev, method]);
      setShowAddCard(false);
      addToast("Payment method added", "success");
    } catch {
      addToast("Failed to add payment method", "error");
    }
  }

  async function handleDeleteMethod(id: string) {
    try {
      await paymentsApi.deleteMethod(id);
      setMethods((prev) => prev.filter((m) => m.paymentMethodId !== id));
      addToast("Payment method removed", "success");
    } catch {
      addToast("Failed to remove payment method", "error");
    }
  }

  const currentPlan = subscription?.plan ?? "FREE";

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Loader2 size={32} className="animate-spin text-trippy-500" />
      </div>
    );
  }

  return (
    <div className="space-y-10">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          Plans & Billing
        </h1>
        <p className="mt-1 text-muted">
          Choose the plan that fits your travel style
        </p>
      </motion.div>

      {/* Current subscription status */}
      {subscription && subscription.plan !== "FREE" && (
        <GlassCard className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Badge variant="success">{subscription.status}</Badge>
              <span className="font-semibold">{subscription.plan} Plan</span>
            </div>
            {subscription.currentPeriodEnd && (
              <p className="text-sm text-muted mt-1">
                {subscription.cancelAtPeriodEnd
                  ? `Cancels on ${subscription.currentPeriodEnd}`
                  : `Renews on ${subscription.currentPeriodEnd}`}
              </p>
            )}
          </div>
          {!subscription.cancelAtPeriodEnd && (
            <Button variant="ghost" size="sm" onClick={handleCancel} disabled={cancelLoading}>
              {cancelLoading ? <Loader2 size={14} className="animate-spin" /> : "Cancel subscription"}
            </Button>
          )}
        </GlassCard>
      )}

      {/* Plan cards */}
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {PLANS.map((plan) => {
          const isCurrent = currentPlan === plan.name.toUpperCase();
          const Icon = plan.icon;

          return (
            <motion.div
              key={plan.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: PLANS.indexOf(plan) * 0.1 }}
            >
              <GlassCard
                variant={plan.highlight ? "strong" : "default"}
                className={cn(
                  "relative flex flex-col h-full",
                  plan.highlight && "ring-2 ring-trippy-500/30",
                )}
              >
                {plan.highlight && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                    <Badge variant="default">
                      <Sparkles size={10} className="mr-1" /> Most Popular
                    </Badge>
                  </div>
                )}

                <div className="flex items-center gap-3 mb-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-trippy-500/10">
                    <Icon size={20} className="text-trippy-500" />
                  </div>
                  <div>
                    <h3 className="font-bold">{plan.name}</h3>
                    <p className="text-muted text-sm">
                      {plan.price === 0 ? (
                        "Free"
                      ) : (
                        <>
                          €{plan.price}
                          <span className="text-xs">/{plan.interval}</span>
                        </>
                      )}
                    </p>
                  </div>
                </div>

                <ul className="flex-1 space-y-2 mb-6">
                  {plan.features.map((f) => (
                    <li key={f} className="flex items-start gap-2 text-sm">
                      <Check size={14} className="mt-0.5 text-success shrink-0" />
                      <span>{f}</span>
                    </li>
                  ))}
                </ul>

                {isCurrent ? (
                  <Button variant="secondary" className="w-full" disabled>
                    Current Plan
                  </Button>
                ) : plan.planId ? (
                  <Button
                    className="w-full"
                    onClick={() => handleCheckout(plan.planId)}
                    disabled={!!checkoutLoading}
                  >
                    {checkoutLoading === plan.planId ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : isCurrent ? (
                      "Current Plan"
                    ) : currentPlan === "FREE" ? (
                      "Subscribe"
                    ) : (
                      "Upgrade"
                    )}
                  </Button>
                ) : null}
              </GlassCard>
            </motion.div>
          );
        })}
      </div>

      {/* Payment Methods */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Payment Methods</h2>
        <div className="space-y-3">
          {methods.map((m) => (
            <GlassCard key={m.paymentMethodId} className="flex items-center gap-4 p-4">
              <CreditCard size={20} className="text-trippy-500" />
              <div className="flex-1">
                <p className="text-sm font-medium capitalize">
                  {m.brand} •••• {m.last4}
                </p>
                <p className="text-xs text-muted">
                  Expires {m.expiryMonth}/{m.expiryYear}
                  {m.isDefault && " · Default"}
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => handleDeleteMethod(m.paymentMethodId)}
              >
                <Trash2 size={14} />
              </Button>
            </GlassCard>
          ))}

          {methods.length === 0 && !showAddCard && (
            <p className="text-sm text-muted">No payment methods added yet.</p>
          )}

          {showAddCard ? (
            <GlassCard className="p-4">
              <form onSubmit={handleAddCard} className="space-y-4">
                <h4 className="font-medium text-sm">Add Payment Method</h4>
                <div className="grid gap-3 sm:grid-cols-2">
                  <select
                    name="brand"
                    className="glass-sm px-3 py-2 text-sm text-foreground"
                    required
                  >
                    <option value="visa">Visa</option>
                    <option value="mastercard">Mastercard</option>
                    <option value="amex">Amex</option>
                  </select>
                  <input
                    name="last4"
                    placeholder="Last 4 digits"
                    maxLength={4}
                    pattern="\d{4}"
                    className="glass-sm px-3 py-2 text-sm text-foreground placeholder:text-muted"
                    required
                  />
                  <input
                    name="expiryMonth"
                    placeholder="MM"
                    type="number"
                    min={1}
                    max={12}
                    className="glass-sm px-3 py-2 text-sm text-foreground placeholder:text-muted"
                    required
                  />
                  <input
                    name="expiryYear"
                    placeholder="YYYY"
                    type="number"
                    min={2024}
                    max={2035}
                    className="glass-sm px-3 py-2 text-sm text-foreground placeholder:text-muted"
                    required
                  />
                </div>
                <div className="flex gap-2">
                  <Button variant="secondary" size="sm" type="button" onClick={() => setShowAddCard(false)}>
                    Cancel
                  </Button>
                  <Button size="sm" type="submit">
                    Add Card
                  </Button>
                </div>
              </form>
            </GlassCard>
          ) : (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setShowAddCard(true)}
            >
              <Plus size={14} /> Add payment method
            </Button>
          )}
        </div>
      </div>
      {/* Billing History */}
      <div>
        <h2 className="text-lg font-semibold mb-4">Billing History</h2>
        {subscription && subscription.plan !== "FREE" ? (
          <GlassCard className="overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs text-muted">
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    {subscription.currentPeriodStart
                      ? new Date(subscription.currentPeriodStart).toLocaleDateString()
                      : "—"}
                  </td>
                  <td className="px-4 py-3">{subscription.plan} subscription</td>
                  <td className="px-4 py-3">
                    €{subscription.priceAmount ?? "0.00"}
                  </td>
                  <td className="px-4 py-3">
                    <Badge variant="success">Paid</Badge>
                  </td>
                </tr>
              </tbody>
            </table>
          </GlassCard>
        ) : (
          <p className="text-sm text-muted">No billing history yet.</p>
        )}
      </div>
    </div>
  );
}
