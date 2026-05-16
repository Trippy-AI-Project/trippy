"use client";

import { useState, useRef, useEffect, type FormEvent } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  X,
  Loader2,
  MapPin,
  Calendar,
  FileText,
  Plane,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Check,
  Lock,
  Globe,
  Package,
  Wallet,
  Gem,
  Crown,
  Info,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { CreateTripRequest } from "@/lib/api";

interface CreateTripModalProps {
  open: boolean;
  onClose: () => void;
  onCreate?: (data: CreateTripRequest) => Promise<void>;
}

const STEP_COUNT = 3;
type Visibility = "PRIVATE" | "PUBLIC";
type Budget = "ECONOMY" | "MODERATE" | "LUXURY";

export default function CreateTripModal({
  open,
  onClose,
  onCreate,
}: CreateTripModalProps) {
  const [step, setStep] = useState(0);
  const [title, setTitle] = useState("");
  const [destination, setDestination] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [description, setDescription] = useState("");
  const [visibility, setVisibility] = useState<Visibility>("PRIVATE");
  const [isPackage, setIsPackage] = useState(false);
  const [budget, setBudget] = useState<Budget>("MODERATE");
  const [loading, setLoading] = useState(false);
  const titleRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (open && step === 0) {
      setTimeout(() => titleRef.current?.focus(), 300);
    }
  }, [open, step]);

  useEffect(() => {
    if (!open) {
      setTimeout(() => {
        setStep(0);
        setTitle("");
        setDestination("");
        setStartDate("");
        setEndDate("");
        setDescription("");
        setVisibility("PRIVATE");
        setIsPackage(false);
        setBudget("MODERATE");
      }, 300);
    }
  }, [open]);

  const canAdvance = step === 0 ? title.trim() && destination.trim() : true;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!onCreate) return;
    setLoading(true);
    try {
      await onCreate({
        title,
        destination,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        description: description || undefined,
        visibility,
        ...(isPackage ? { budgetLevel: budget } : {}),
      });
    } finally {
      setLoading(false);
    }
  }

  const steps = [
    { icon: MapPin, label: "Basics" },
    { icon: Calendar, label: "Dates" },
    { icon: FileText, label: "Details" },
  ];

  const budgetOptions: {
    key: Budget;
    label: string;
    icon: typeof Wallet;
    desc: string;
  }[] = [
    { key: "ECONOMY", label: "Economy", icon: Wallet, desc: "Budget-friendly" },
    { key: "MODERATE", label: "Moderate", icon: Gem, desc: "Balanced comfort" },
    { key: "LUXURY", label: "Luxury", icon: Crown, desc: "Premium picks" },
  ];

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 backdrop-blur-md"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
            <motion.div
              className="relative my-8 w-full max-w-lg overflow-hidden rounded-3xl bg-surface border border-border shadow-2xl"
              initial={{ opacity: 0, scale: 0.92, y: 30 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.92, y: 30 }}
              transition={{ type: "spring", stiffness: 350, damping: 30 }}
            >
              {/* ── Header (no overlapping step circles) ────── */}
              <div className="relative overflow-hidden bg-gradient-to-br from-trippy-500 via-trippy-600 to-trippy-700 px-7 py-6">
                <div className="pointer-events-none absolute -right-6 -top-6 h-28 w-28 rounded-full bg-white/5" />
                <div className="pointer-events-none absolute right-16 top-14 h-10 w-10 rounded-full bg-accent-500/20" />
                <div className="pointer-events-none absolute -left-4 bottom-2 h-16 w-16 rounded-full bg-white/5" />

                <button
                  onClick={onClose}
                  className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-white/10 text-white/70 transition-colors hover:bg-white/20 hover:text-white cursor-pointer"
                  aria-label="Close"
                >
                  <X size={16} />
                </button>

                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/15 backdrop-blur-sm">
                    <Plane size={20} className="text-white" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">
                      Create a new trip
                    </h2>
                    <p className="text-xs text-white/60">
                      Step {step + 1} of {STEP_COUNT}
                      <span className="mx-1.5">•</span>
                      {steps[step].label}
                    </p>
                  </div>
                </div>
              </div>

              {/* ── Step indicator (clean, in body) ─────────── */}
              <div className="px-7 pt-5">
                <div className="flex items-center gap-2">
                  {steps.map((s, i) => {
                    const StepIcon = s.icon;
                    const isActive = i === step;
                    const isDone = i < step;
                    return (
                      <div key={i} className="flex flex-1 items-center gap-2">
                        <div
                          className={cn(
                            "flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 transition-all duration-300",
                            isActive
                              ? "border-accent-500 bg-accent-500 text-white shadow-md shadow-accent-500/30"
                              : isDone
                                ? "border-accent-500 bg-accent-500 text-white"
                                : "border-border bg-surface text-muted"
                          )}
                        >
                          {isDone ? (
                            <Check size={13} strokeWidth={3} />
                          ) : (
                            <StepIcon size={13} />
                          )}
                        </div>
                        {i < steps.length - 1 && (
                          <div
                            className={cn(
                              "h-0.5 flex-1 rounded-full transition-colors duration-300",
                              i < step ? "bg-accent-500" : "bg-border"
                            )}
                          />
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* ── Form body ─────────────────────────────────── */}
              <form
                onSubmit={handleSubmit}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && step < STEP_COUNT - 1) {
                    e.preventDefault();
                    if (canAdvance) setStep((s) => s + 1);
                  }
                }}
                className="px-7 pb-7 pt-5"
              >
                <AnimatePresence mode="wait">
                  {step === 0 && (
                    <motion.div
                      key="step-0"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.2 }}
                      className="space-y-5"
                    >
                      <p className="text-sm text-muted">
                        Where are you headed? Give your trip a name and
                        destination.
                      </p>

                      <div>
                        <label
                          htmlFor="trip-title"
                          className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted"
                        >
                          Trip name
                        </label>
                        <div className="relative">
                          <Sparkles
                            size={15}
                            className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-accent-400"
                          />
                          <input
                            ref={titleRef}
                            id="trip-title"
                            type="text"
                            placeholder="e.g. Summer in Barcelona"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            required
                            className="w-full rounded-xl border border-border bg-shore-50 py-3 pl-10 pr-4 text-sm text-foreground placeholder:text-muted/60 transition-colors focus:border-accent-400 focus:outline-none focus:ring-2 focus:ring-accent-200"
                          />
                        </div>
                      </div>

                      <div>
                        <label
                          htmlFor="trip-dest"
                          className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted"
                        >
                          Destination
                        </label>
                        <div className="relative">
                          <MapPin
                            size={15}
                            className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-accent-400"
                          />
                          <input
                            id="trip-dest"
                            type="text"
                            placeholder="e.g. Barcelona, Spain"
                            value={destination}
                            onChange={(e) => setDestination(e.target.value)}
                            required
                            className="w-full rounded-xl border border-border bg-shore-50 py-3 pl-10 pr-4 text-sm text-foreground placeholder:text-muted/60 transition-colors focus:border-accent-400 focus:outline-none focus:ring-2 focus:ring-accent-200"
                          />
                        </div>
                      </div>
                    </motion.div>
                  )}

                  {step === 1 && (
                    <motion.div
                      key="step-1"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.2 }}
                      className="space-y-5"
                    >
                      <p className="text-sm text-muted">
                        When does the adventure begin? Dates are optional — you
                        can set them later.
                      </p>

                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label
                            htmlFor="trip-start"
                            className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted"
                          >
                            Start date
                          </label>
                          <div className="relative">
                            <Calendar
                              size={15}
                              className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-accent-400"
                            />
                            <input
                              id="trip-start"
                              type="date"
                              value={startDate}
                              onChange={(e) => setStartDate(e.target.value)}
                              className="w-full rounded-xl border border-border bg-shore-50 py-3 pl-10 pr-3 text-sm text-foreground transition-colors focus:border-accent-400 focus:outline-none focus:ring-2 focus:ring-accent-200"
                            />
                          </div>
                        </div>

                        <div>
                          <label
                            htmlFor="trip-end"
                            className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted"
                          >
                            End date
                          </label>
                          <div className="relative">
                            <Calendar
                              size={15}
                              className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-accent-400"
                            />
                            <input
                              id="trip-end"
                              type="date"
                              value={endDate}
                              min={startDate || undefined}
                              onChange={(e) => setEndDate(e.target.value)}
                              className="w-full rounded-xl border border-border bg-shore-50 py-3 pl-10 pr-3 text-sm text-foreground transition-colors focus:border-accent-400 focus:outline-none focus:ring-2 focus:ring-accent-200"
                            />
                          </div>
                        </div>
                      </div>

                      {startDate && endDate && (
                        <motion.div
                          initial={{ opacity: 0, y: 6 }}
                          animate={{ opacity: 1, y: 0 }}
                          className="flex items-center gap-2 rounded-xl bg-accent-50 px-4 py-2.5 text-xs font-medium text-accent-600"
                        >
                          <Plane size={13} />
                          {(() => {
                            const days = Math.ceil(
                              (new Date(endDate).getTime() -
                                new Date(startDate).getTime()) /
                                86400000
                            );
                            return days > 0
                              ? `${days} day${days !== 1 ? "s" : ""} of adventure`
                              : "End date should be after start date";
                          })()}
                        </motion.div>
                      )}

                      {/* ── Visibility pill toggle ─────────────── */}
                      <div>
                        <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted">
                          Visibility
                        </label>
                        <div className="grid grid-cols-2 gap-2">
                          {(
                            [
                              {
                                key: "PRIVATE" as const,
                                icon: Lock,
                                label: "Private",
                                desc: "Only invited members",
                              },
                              {
                                key: "PUBLIC" as const,
                                icon: Globe,
                                label: "Public",
                                desc: "Visible in discovery",
                              },
                            ]
                          ).map((opt) => {
                            const OptIcon = opt.icon;
                            const active = visibility === opt.key;
                            return (
                              <button
                                key={opt.key}
                                type="button"
                                onClick={() => setVisibility(opt.key)}
                                className={cn(
                                  "flex items-start gap-2.5 rounded-xl border-2 p-3 text-left transition-all cursor-pointer",
                                  active
                                    ? "border-accent-500 bg-accent-50"
                                    : "border-border bg-shore-50 hover:border-accent-300"
                                )}
                              >
                                <div
                                  className={cn(
                                    "mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg",
                                    active
                                      ? "bg-accent-500 text-white"
                                      : "bg-surface text-muted"
                                  )}
                                >
                                  <OptIcon size={13} />
                                </div>
                                <div className="min-w-0">
                                  <p
                                    className={cn(
                                      "text-sm font-semibold",
                                      active ? "text-accent-600" : "text-foreground"
                                    )}
                                  >
                                    {opt.label}
                                  </p>
                                  <p className="text-[11px] leading-tight text-muted">
                                    {opt.desc}
                                  </p>
                                </div>
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    </motion.div>
                  )}

                  {step === 2 && (
                    <motion.div
                      key="step-2"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.2 }}
                      className="space-y-5"
                    >
                      <p className="text-sm text-muted">
                        Almost there! Add a few extras for your travel
                        companions.
                      </p>

                      {/* Description */}
                      <div>
                        <label
                          htmlFor="trip-desc"
                          className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-muted"
                        >
                          Description{" "}
                          <span className="font-normal text-muted/60">
                            (optional)
                          </span>
                        </label>
                        <textarea
                          id="trip-desc"
                          rows={2}
                          placeholder="A quick note — vibe, goals, must-sees..."
                          value={description}
                          onChange={(e) => setDescription(e.target.value)}
                          className="w-full resize-none rounded-xl border border-border bg-shore-50 px-4 py-3 text-sm text-foreground placeholder:text-muted/60 transition-colors focus:border-accent-400 focus:outline-none focus:ring-2 focus:ring-accent-200"
                        />
                      </div>

                      {/* ── Package toggle ─────────────────────── */}
                      <div className="rounded-xl border border-border bg-shore-50 p-3.5">
                        <label className="flex cursor-pointer items-start gap-3">
                          <div className="relative mt-0.5 shrink-0">
                            <input
                              type="checkbox"
                              checked={isPackage}
                              onChange={(e) => setIsPackage(e.target.checked)}
                              className="peer sr-only"
                            />
                            <div className="h-5 w-9 rounded-full bg-border transition-colors peer-checked:bg-accent-500" />
                            <div className="absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform peer-checked:translate-x-4" />
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-1.5">
                              <Package size={13} className="text-accent-500" />
                              <span className="text-sm font-semibold">
                                Make this a package trip
                              </span>
                            </div>
                            <p className="mt-0.5 text-[11px] leading-snug text-muted">
                              Set a budget tier so we can pre-fill cost estimates
                              for activities and stays.
                            </p>
                          </div>
                        </label>

                        {/* Budget options */}
                        <AnimatePresence initial={false}>
                          {isPackage && (
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: "auto", opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              transition={{ duration: 0.22 }}
                              className="overflow-hidden"
                            >
                              <div className="mt-3.5 grid grid-cols-3 gap-2">
                                {budgetOptions.map((opt) => {
                                  const OptIcon = opt.icon;
                                  const active = budget === opt.key;
                                  return (
                                    <button
                                      key={opt.key}
                                      type="button"
                                      onClick={() => setBudget(opt.key)}
                                      className={cn(
                                        "flex flex-col items-center gap-1 rounded-lg border-2 p-2.5 text-center transition-all cursor-pointer",
                                        active
                                          ? "border-accent-500 bg-accent-50"
                                          : "border-border bg-surface hover:border-accent-300"
                                      )}
                                    >
                                      <OptIcon
                                        size={16}
                                        className={cn(
                                          active
                                            ? "text-accent-500"
                                            : "text-muted"
                                        )}
                                      />
                                      <span
                                        className={cn(
                                          "text-[12px] font-semibold leading-none",
                                          active
                                            ? "text-accent-600"
                                            : "text-foreground"
                                        )}
                                      >
                                        {opt.label}
                                      </span>
                                      <span className="text-[10px] leading-tight text-muted">
                                        {opt.desc}
                                      </span>
                                    </button>
                                  );
                                })}
                              </div>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>

                      {/* Hint */}
                      <div className="flex items-start gap-2.5 rounded-xl border border-trippy-200/40 bg-trippy-500/5 p-3">
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-trippy-500/10">
                          <Info size={12} className="text-trippy-500" />
                        </div>
                        <p className="text-[12px] leading-snug text-muted">
                          You can customize the detailed day-by-day itinerary,
                          places, per-activity cost estimates, and the total
                          estimated cost after creating the trip.
                        </p>
                      </div>

                      {/* Summary */}
                      <div className="rounded-2xl border border-border bg-shore-50 p-4">
                        <p className="mb-2.5 text-xs font-semibold uppercase tracking-wider text-muted">
                          Trip summary
                        </p>
                        <div className="flex items-start gap-3">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent-500/10">
                            <Plane size={16} className="text-accent-500" />
                          </div>
                          <div className="min-w-0 flex-1 space-y-0.5">
                            <p className="text-sm font-semibold truncate">
                              {title || "Untitled trip"}
                            </p>
                            <p className="flex items-center gap-1 text-xs text-muted">
                              <MapPin size={11} />
                              {destination || "—"}
                            </p>
                            {startDate && (
                              <p className="flex items-center gap-1 text-xs text-muted">
                                <Calendar size={11} />
                                {startDate}
                                {endDate ? ` → ${endDate}` : ""}
                              </p>
                            )}
                            <div className="flex flex-wrap items-center gap-1.5 pt-1">
                              <span
                                className={cn(
                                  "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold",
                                  visibility === "PUBLIC"
                                    ? "bg-leaf-100 text-green-700"
                                    : "bg-shore-200/60 text-trippy-500"
                                )}
                              >
                                {visibility === "PUBLIC" ? (
                                  <Globe size={10} />
                                ) : (
                                  <Lock size={10} />
                                )}
                                {visibility === "PUBLIC"
                                  ? "Public"
                                  : "Private"}
                              </span>
                              {isPackage && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-accent-50 px-2 py-0.5 text-[10px] font-semibold text-accent-600">
                                  <Package size={10} />
                                  {budget.charAt(0) +
                                    budget.slice(1).toLowerCase()}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* ── Navigation buttons ──────────────────────── */}
                <div className="mt-7 flex gap-3">
                  {step > 0 && (
                    <button
                      type="button"
                      onClick={() => setStep((s) => s - 1)}
                      className="flex items-center gap-1.5 rounded-xl border border-border bg-surface px-5 py-2.5 text-sm font-medium text-muted transition-all hover:bg-surface-hover hover:text-foreground cursor-pointer"
                    >
                      <ArrowLeft size={14} />
                      Back
                    </button>
                  )}

                  <div className="flex-1" />

                  {step < STEP_COUNT - 1 ? (
                    <button
                      type="button"
                      disabled={!canAdvance}
                      onClick={() => setStep((s) => s + 1)}
                      className={cn(
                        "flex items-center gap-1.5 rounded-xl px-6 py-2.5 text-sm font-semibold text-white transition-all cursor-pointer",
                        "bg-gradient-to-r from-accent-500 to-accent-600 shadow-md",
                        "hover:shadow-lg hover:-translate-y-0.5",
                        "disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-md"
                      )}
                    >
                      Continue
                      <ArrowRight size={14} />
                    </button>
                  ) : (
                    <button
                      type="submit"
                      disabled={loading || !canAdvance}
                      className={cn(
                        "flex items-center gap-2 rounded-xl px-7 py-2.5 text-sm font-semibold text-white transition-all cursor-pointer",
                        "bg-gradient-to-r from-accent-500 to-accent-600 shadow-md",
                        "hover:shadow-lg hover:-translate-y-0.5",
                        "disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-md"
                      )}
                    >
                      {loading ? (
                        <Loader2 size={15} className="animate-spin" />
                      ) : (
                        <Sparkles size={15} />
                      )}
                      {loading ? "Creating..." : "Create trip"}
                    </button>
                  )}
                </div>
              </form>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
