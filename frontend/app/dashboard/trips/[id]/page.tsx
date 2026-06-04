"use client";

import { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  MapPin,
  Calendar,
  Users,
  MessageSquare,
  Loader2,
  Edit,
  Trash2,
  Plus,
  Sparkles,
  Clock,
  ChevronDown,
  ChevronUp,
  X,
  Save,
  Plane,
  Sun,
  Coffee,
  Utensils,
  Camera,
  Moon,
  Wand2,
  Globe,
  DollarSign,
  Navigation,
  Crown,
  Heart,
  Map,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { GlassCard, Button, Badge, Avatar } from "@/components/ui";
import { tripsApi, usersApi, type TripDetail, type DayPlan, type Activity } from "@/lib/api";
import { useToast } from "@/lib/toast";
import { cn, tripIdFromSlug } from "@/lib/utils";

const statusVariant: Record<string, "default" | "success" | "warning" | "accent" | "danger"> = {
  DRAFT: "default",
  PLANNED: "accent",
  ONGOING: "success",
  COMPLETED: "warning",
  CANCELLED: "danger",
};

const statusLabel: Record<string, string> = {
  DRAFT: "Draft",
  PLANNED: "Planned",
  ONGOING: "In Progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

/* ─── Activity category icons ────────────────────────────────────── */
const categoryIcons: Record<string, typeof Coffee> = {
  morning: Sun,
  breakfast: Coffee,
  lunch: Utensils,
  dinner: Utensils,
  sightseeing: Camera,
  transport: Navigation,
  evening: Moon,
  default: MapPin,
};

function getCategoryIcon(category?: string) {
  if (!category) return categoryIcons.default;
  return categoryIcons[category.toLowerCase()] ?? categoryIcons.default;
}

/* ─── Currency options ────────────────────────────────────────────── */
const currencies = [
  { code: "USD", symbol: "$" },
  { code: "EUR", symbol: "€" },
  { code: "GBP", symbol: "£" },
  { code: "INR", symbol: "₹" },
  { code: "JPY", symbol: "¥" },
  { code: "AUD", symbol: "A$" },
  { code: "CAD", symbol: "C$" },
];

/* ─── Category options ────────────────────────────────────────────── */
const categoryOptions = [
  { key: "sightseeing", label: "Sightseeing", icon: Camera },
  { key: "breakfast", label: "Breakfast", icon: Coffee },
  { key: "lunch", label: "Lunch", icon: Utensils },
  { key: "dinner", label: "Dinner", icon: Utensils },
  { key: "transport", label: "Transport", icon: Navigation },
  { key: "morning", label: "Morning", icon: Sun },
  { key: "evening", label: "Evening", icon: Moon },
  { key: "default", label: "Other", icon: MapPin },
];

/* ─── Time Picker Popup (fixed overlay) ───────────────────────────── */
const hours = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, "0"));
const minutes = ["00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"];

function TimePickerPopup({
  value,
  onChange,
  onClose,
  label,
  anchorRef,
}: {
  value: string;
  onChange: (v: string) => void;
  onClose: () => void;
  label: string;
  anchorRef: React.RefObject<HTMLButtonElement | null>;
}) {
  const [selHour, setSelHour] = useState(value ? value.split(":")[0] : "09");
  const [selMin, setSelMin] = useState(value ? value.split(":")[1] : "00");

  // Position relative to the anchor button
  const [pos, setPos] = useState({ top: 0, left: 0 });
  useEffect(() => {
    if (anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect();
      setPos({ top: rect.bottom + 8, left: rect.left });
    }
  }, [anchorRef]);

  function confirm() {
    onChange(`${selHour}:${selMin}`);
    onClose();
  }

  return (
    <>
      {/* Backdrop */}
      <motion.div
        className="fixed inset-0 z-40"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      />
      {/* Picker */}
      <motion.div
        initial={{ opacity: 0, scale: 0.92, y: -6 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.92, y: -6 }}
        style={{ top: pos.top, left: pos.left }}
        className="fixed z-50 w-56 rounded-2xl border border-border bg-white p-4 shadow-2xl"
      >
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted mb-2">{label}</p>
        <div className="flex gap-3">
          {/* Hours */}
          <div className="flex-1">
            <p className="text-[9px] text-muted/60 mb-1 text-center">Hour</p>
            <div className="h-36 overflow-y-auto rounded-xl border border-border/60 scrollbar-thin">
              {hours.map((h) => (
                <button
                  key={h}
                  onClick={() => setSelHour(h)}
                  className={cn(
                    "w-full py-1.5 text-xs text-center transition-colors cursor-pointer",
                    selHour === h
                      ? "bg-accent-500 text-white font-bold"
                      : "text-foreground hover:bg-shore-50"
                  )}
                >
                  {h}
                </button>
              ))}
            </div>
          </div>
          {/* Minutes */}
          <div className="flex-1">
            <p className="text-[9px] text-muted/60 mb-1 text-center">Min</p>
            <div className="h-36 overflow-y-auto rounded-xl border border-border/60">
              {minutes.map((m) => (
                <button
                  key={m}
                  onClick={() => setSelMin(m)}
                  className={cn(
                    "w-full py-1.5 text-xs text-center transition-colors cursor-pointer",
                    selMin === m
                      ? "bg-accent-500 text-white font-bold"
                      : "text-foreground hover:bg-shore-50"
                  )}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>
        </div>
        <button
          onClick={confirm}
          className="mt-3 w-full rounded-xl bg-accent-500 py-2 text-xs font-bold text-white hover:bg-accent-600 transition-colors cursor-pointer"
        >
          Set {selHour}:{selMin}
        </button>
      </motion.div>
    </>
  );
}

/* ─── Editable Activity Row ──────────────────────────────────────── */
function ActivityRow({
  activity,
  currency,
  onCurrencyChange,
  onUpdate,
  onRemove,
}: {
  activity: Activity;
  currency: string;
  onCurrencyChange: (c: string) => void;
  onUpdate: (a: Activity) => void;
  onRemove: () => void;
}) {
  const Icon = getCategoryIcon(activity.category);
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);
  const [showStartPicker, setShowStartPicker] = useState(false);
  const [showEndPicker, setShowEndPicker] = useState(false);
  const startBtnRef = useRef<HTMLButtonElement>(null);
  const endBtnRef = useRef<HTMLButtonElement>(null);

  // Parse time range: "09:00 - 11:00" or just "09:00"
  const timeParts = (activity.time ?? "").split("-").map((s) => s.trim());
  const startTime = timeParts[0] ?? "";
  const endTime = timeParts[1] ?? "";

  function updateTime(start: string, end: string) {
    const combined = end ? `${start} - ${end}` : start;
    onUpdate({ ...activity, time: combined });
  }

  const currencySymbol = currencies.find((c) => c.code === currency)?.symbol ?? "$";

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -20, transition: { duration: 0.2 } }}
      className="group relative rounded-2xl border border-border/60 bg-white shadow-sm transition-all hover:border-accent-300 hover:shadow-md"
    >
      {/* Top accent bar */}
      <div className="absolute inset-x-0 top-0 h-0.5 rounded-t-2xl bg-gradient-to-r from-accent-400/60 via-accent-500/30 to-transparent" />

      <div className="p-4 space-y-3">
        {/* Row 1: Category icon + Title + Remove */}
        <div className="flex items-center gap-3">
          {/* Category picker button */}
          <div className="relative">
            <button
              onClick={() => setShowCategoryPicker(!showCategoryPicker)}
              className={cn(
                "flex h-9 w-9 shrink-0 items-center justify-center rounded-xl transition-all cursor-pointer",
                "bg-gradient-to-br from-accent-100 to-accent-50 border border-accent-200",
                "hover:from-accent-200 hover:to-accent-100 hover:shadow-sm"
              )}
              title="Change category"
            >
              <Icon size={15} className="text-accent-600" />
            </button>
            {/* Category dropdown */}
            <AnimatePresence>
              {showCategoryPicker && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.9, y: -4 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.9, y: -4 }}
                  className="absolute left-0 top-11 z-20 w-40 rounded-xl border border-border bg-white p-1.5 shadow-xl"
                >
                  {categoryOptions.map((cat) => {
                    const CatIcon = cat.icon;
                    return (
                      <button
                        key={cat.key}
                        onClick={() => {
                          onUpdate({ ...activity, category: cat.key });
                          setShowCategoryPicker(false);
                        }}
                        className={cn(
                          "flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-xs transition-colors cursor-pointer",
                          activity.category === cat.key
                            ? "bg-accent-50 text-accent-700 font-medium"
                            : "text-foreground hover:bg-shore-50"
                        )}
                      >
                        <CatIcon size={13} className={activity.category === cat.key ? "text-accent-500" : "text-muted"} />
                        {cat.label}
                      </button>
                    );
                  })}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Title input */}
          <input
            type="text"
            value={activity.title}
            onChange={(e) => onUpdate({ ...activity, title: e.target.value })}
            placeholder="What are you doing?"
            className="flex-1 bg-transparent text-sm font-semibold text-foreground placeholder:text-muted/40 focus:outline-none"
          />

          {/* Remove button */}
          <button
            onClick={onRemove}
            className="shrink-0 flex h-7 w-7 items-center justify-center rounded-lg text-muted/40 opacity-0 transition-all group-hover:opacity-100 hover:bg-red-50 hover:text-red-500 cursor-pointer"
          >
            <X size={13} />
          </button>
        </div>

        {/* Row 2: Time picker buttons + Cost with currency dropdown */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Start time */}
          <button
            ref={startBtnRef}
            onClick={() => { setShowStartPicker(!showStartPicker); setShowEndPicker(false); }}
            className="flex items-center gap-1.5 rounded-xl bg-shore-50 border border-border/80 px-3 py-2 text-xs font-medium text-foreground hover:border-accent-300 hover:bg-accent-50/30 transition-all cursor-pointer"
          >
            <Clock size={12} className="text-accent-500" />
            <span>{startTime || "Start"}</span>
          </button>
          <AnimatePresence>
            {showStartPicker && (
              <TimePickerPopup
                value={startTime}
                label="Start time"
                anchorRef={startBtnRef}
                onChange={(v) => updateTime(v, endTime)}
                onClose={() => setShowStartPicker(false)}
              />
            )}
          </AnimatePresence>

          <span className="text-[10px] text-muted/50 font-medium">→</span>

          {/* End time */}
          <button
            ref={endBtnRef}
            onClick={() => { setShowEndPicker(!showEndPicker); setShowStartPicker(false); }}
            className="flex items-center gap-1.5 rounded-xl bg-shore-50 border border-border/80 px-3 py-2 text-xs font-medium text-foreground hover:border-accent-300 hover:bg-accent-50/30 transition-all cursor-pointer"
          >
            <Clock size={12} className="text-muted/60" />
            <span>{endTime || "End"}</span>
          </button>
          <AnimatePresence>
            {showEndPicker && (
              <TimePickerPopup
                value={endTime}
                label="End time"
                anchorRef={endBtnRef}
                onChange={(v) => updateTime(startTime, v)}
                onClose={() => setShowEndPicker(false)}
              />
            )}
          </AnimatePresence>

          {/* Cost with inline currency dropdown */}
          <div className="flex items-center gap-1 rounded-xl bg-shore-50 border border-border/80 px-2 py-1.5 ml-auto">
            <div className="relative flex items-center">
              <select
                value={currency}
                onChange={(e) => onCurrencyChange(e.target.value)}
                className="bg-transparent text-[11px] font-bold text-accent-600 focus:outline-none cursor-pointer pr-4 appearance-none"
              >
                {currencies.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.symbol} {c.code}
                  </option>
                ))}
              </select>
              <ChevronDown size={10} className="pointer-events-none absolute right-0 text-accent-500" />
            </div>
            <div className="w-px h-4 bg-border/60 mx-0.5" />
            <input
              type="number"
              min="0"
              step="0.01"
              value={activity.estimatedCost ?? ""}
              onChange={(e) => onUpdate({ ...activity, estimatedCost: e.target.value })}
              placeholder="0.00"
              className="w-16 bg-transparent text-xs font-medium text-foreground placeholder:text-muted/40 focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            />
          </div>
        </div>

        {/* Row 3: Location */}
        <div className="flex items-center gap-2 rounded-xl bg-shore-50/60 border border-border/50 px-3 py-2">
          <MapPin size={12} className="text-muted/60 shrink-0" />
          <input
            type="text"
            value={activity.location ?? ""}
            onChange={(e) => onUpdate({ ...activity, location: e.target.value })}
            placeholder="Add a location..."
            className="flex-1 bg-transparent text-xs text-foreground placeholder:text-muted/40 focus:outline-none"
          />
        </div>

        {/* Row 4: Description / notes */}
        <textarea
          value={activity.description ?? ""}
          onChange={(e) => onUpdate({ ...activity, description: e.target.value })}
          placeholder="Add notes or description..."
          rows={1}
          className="w-full resize-none rounded-xl bg-shore-50/40 border border-border/40 px-3 py-2 text-xs text-foreground placeholder:text-muted/40 focus:border-accent-300 focus:outline-none focus:ring-1 focus:ring-accent-100 transition-colors"
        />
      </div>
    </motion.div>
  );
}

/* ─── Day Card ────────────────────────────────────────────────────── */
function DayCard({
  day,
  tripStartDate,
  expanded,
  onToggle,
  onUpdateDay,
  currency,
  onCurrencyChange,
}: {
  day: DayPlan;
  tripStartDate?: string;
  expanded: boolean;
  onToggle: () => void;
  onUpdateDay: (d: DayPlan) => void;
  currency: string;
  onCurrencyChange: (c: string) => void;
}) {
  const dayDate = tripStartDate
    ? new Date(new Date(tripStartDate).getTime() + (day.dayNumber - 1) * 86400000).toLocaleDateString("en-US", {
        weekday: "short",
        month: "short",
        day: "numeric",
      })
    : null;

  function addActivity() {
    const newActivity: Activity = {
      activityId: `temp-${Date.now()}-${Math.random()}`,
      title: "",
      time: "",
      description: "",
      location: "",
      category: "default",
      estimatedCost: "",
    };
    onUpdateDay({ ...day, activities: [...day.activities, newActivity] });
  }

  function updateActivity(idx: number, updated: Activity) {
    const acts = [...day.activities];
    acts[idx] = updated;
    onUpdateDay({ ...day, activities: acts });
  }

  function removeActivity(idx: number) {
    onUpdateDay({ ...day, activities: day.activities.filter((_, i) => i !== idx) });
  }

  const totalCost = day.activities.reduce((sum, a) => {
    const cost = parseFloat(a.estimatedCost ?? "0");
    return sum + (isNaN(cost) ? 0 : cost);
  }, 0);

  return (
    <motion.div
      layout
      className={cn(
        "rounded-2xl border-2 transition-all duration-300",
        expanded
          ? "border-accent-400/50 shadow-lg shadow-accent-500/5 bg-white"
          : "border-border bg-surface hover:border-accent-300 hover:shadow-sm"
      )}
    >
      {/* Day header */}
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-4 p-5 text-left cursor-pointer"
      >
        <div
          className={cn(
            "flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl font-bold text-lg transition-colors",
            expanded
              ? "bg-gradient-to-br from-accent-400 to-accent-600 text-white shadow-md shadow-accent-500/20"
              : "bg-shore-100 text-muted"
          )}
        >
          {day.dayNumber}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <input
              type="text"
              value={day.title ?? ""}
              onChange={(e) => {
                e.stopPropagation();
                onUpdateDay({ ...day, title: e.target.value });
              }}
              onClick={(e) => e.stopPropagation()}
              placeholder={`Day ${day.dayNumber} — Give it a title`}
              className={cn(
                "flex-1 bg-transparent text-sm font-bold placeholder:text-muted/50 focus:outline-none",
                expanded ? "text-foreground" : "text-foreground"
              )}
            />
          </div>
          <div className="flex items-center gap-3 mt-0.5">
            {dayDate && (
              <span className="text-[11px] text-muted flex items-center gap-1">
                <Calendar size={10} /> {dayDate}
              </span>
            )}
            <span className="text-[11px] text-muted">
              {day.activities.length} activit{day.activities.length !== 1 ? "ies" : "y"}
            </span>
            {totalCost > 0 && (
              <span className="text-[11px] text-accent-600 font-medium flex items-center gap-0.5">
                <DollarSign size={9} /> ~{currencies.find((c) => c.code === currency)?.symbol ?? "$"}{totalCost.toFixed(0)}
              </span>
            )}
          </div>
        </div>
        <div
          className={cn(
            "flex h-8 w-8 items-center justify-center rounded-xl transition-colors",
            expanded ? "bg-accent-100 text-accent-600" : "bg-shore-100 text-muted"
          )}
        >
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </div>
      </button>

      {/* Day content */}
      <AnimatePresence initial={false}>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: "easeInOut" }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 space-y-3">
              {/* Activities list */}
              <AnimatePresence>
                {day.activities.map((activity, idx) => (
                  <ActivityRow
                    key={activity.activityId}
                    activity={activity}
                    currency={currency}
                    onCurrencyChange={onCurrencyChange}
                    onUpdate={(a) => updateActivity(idx, a)}
                    onRemove={() => removeActivity(idx)}
                  />
                ))}
              </AnimatePresence>

              {/* Empty state */}
              {day.activities.length === 0 && (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-shore-100 mb-3">
                    <Plane size={20} className="text-muted/50" />
                  </div>
                  <p className="text-sm text-muted/70">No activities yet</p>
                  <p className="text-xs text-muted/50 mt-0.5">Add activities or let AI plan this day</p>
                </div>
              )}

              {/* Add activity button */}
              <button
                onClick={addActivity}
                className="flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-border py-3 text-xs font-medium text-muted transition-all hover:border-accent-400 hover:text-accent-600 hover:bg-accent-50/50 cursor-pointer"
              >
                <Plus size={14} /> Add activity
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

/* ─── AI Generation Modal (dummy) ────────────────────────────────── */
function AIGeneratePanel({
  open,
  onClose,
  tripTitle,
  destination,
  numDays,
  onGenerate,
}: {
  open: boolean;
  onClose: () => void;
  tripTitle: string;
  destination: string;
  numDays: number;
  onGenerate: (days: DayPlan[]) => void;
}) {
  const [generating, setGenerating] = useState(false);
  const [style, setStyle] = useState<"adventure" | "relaxed" | "cultural" | "foodie">("adventure");

  async function handleGenerate() {
    setGenerating(true);
    // Simulate AI generation delay
    await new Promise((r) => setTimeout(r, 2500));

    // Generate dummy itinerary
    const dummyDays: DayPlan[] = Array.from({ length: numDays }, (_, i) => ({
      dayPlanId: `ai-day-${i + 1}-${Date.now()}`,
      dayNumber: i + 1,
      title: getDummyDayTitle(i, destination, style),
      activities: getDummyActivities(i, destination, style),
    }));

    setGenerating(false);
    onGenerate(dummyDays);
    onClose();
  }

  const styles = [
    { key: "adventure" as const, label: "Adventure", emoji: "🏔️" },
    { key: "relaxed" as const, label: "Relaxed", emoji: "🏖️" },
    { key: "cultural" as const, label: "Cultural", emoji: "🏛️" },
    { key: "foodie" as const, label: "Foodie", emoji: "🍽️" },
  ];

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
          <motion.div
            className="relative z-10 w-full max-w-md overflow-hidden rounded-3xl bg-surface border border-border shadow-2xl"
            initial={{ opacity: 0, scale: 0.9, y: 30 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 30 }}
            transition={{ type: "spring", stiffness: 300, damping: 28 }}
          >
            {/* Header */}
            <div className="relative overflow-hidden bg-gradient-to-br from-trippy-600 via-trippy-700 to-trippy-800 px-6 py-5">
              <div className="pointer-events-none absolute -right-8 -top-8 h-32 w-32 rounded-full bg-accent-500/10 blur-2xl" />
              <div className="pointer-events-none absolute -left-4 bottom-0 h-20 w-20 rounded-full bg-white/5" />
              <button
                onClick={onClose}
                className="absolute right-4 top-4 flex h-7 w-7 items-center justify-center rounded-full bg-white/10 text-white/70 hover:bg-white/20 hover:text-white cursor-pointer"
              >
                <X size={14} />
              </button>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm">
                  <Wand2 size={18} className="text-white" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">AI Itinerary Builder</h3>
                  <p className="text-xs text-white/60">Generate a full plan with one click</p>
                </div>
              </div>
            </div>

            {/* Content */}
            <div className="p-6 space-y-5">
              <div className="rounded-xl bg-shore-50 border border-border p-4 space-y-2">
                <div className="flex items-center gap-2 text-sm">
                  <MapPin size={13} className="text-accent-500" />
                  <span className="font-medium">{destination}</span>
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <Calendar size={13} className="text-accent-500" />
                  <span className="text-muted">{numDays} day{numDays !== 1 ? "s" : ""}</span>
                </div>
              </div>

              {/* Style selection */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-muted uppercase tracking-wider">
                  Travel style
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {styles.map((s) => (
                    <button
                      key={s.key}
                      onClick={() => setStyle(s.key)}
                      className={cn(
                        "flex items-center gap-2 rounded-xl border-2 p-3 text-left transition-all cursor-pointer",
                        style === s.key
                          ? "border-accent-500 bg-accent-50 shadow-sm"
                          : "border-border bg-white hover:border-accent-300"
                      )}
                    >
                      <span className="text-lg">{s.emoji}</span>
                      <span className={cn(
                        "text-sm font-medium",
                        style === s.key ? "text-accent-600" : "text-foreground"
                      )}>
                        {s.label}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Generate button */}
              <button
                onClick={handleGenerate}
                disabled={generating}
                className={cn(
                  "w-full flex items-center justify-center gap-2.5 rounded-2xl py-3.5 text-sm font-bold text-white transition-all cursor-pointer",
                  "bg-gradient-to-r from-accent-500 to-accent-600 shadow-lg shadow-accent-500/20",
                  "hover:shadow-xl hover:-translate-y-0.5",
                  "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
                )}
              >
                {generating ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    Crafting your itinerary...
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    Generate {numDays}-Day Itinerary
                  </>
                )}
              </button>

              {generating && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="text-center text-xs text-muted"
                >
                  AI is planning activities, meals, and sightseeing for each day...
                </motion.div>
              )}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ─── Dummy data generators ───────────────────────────────────────── */
function getDummyDayTitle(dayIdx: number, destination: string, style: string): string {
  const titles: Record<string, string[]> = {
    adventure: ["Arrival & First Exploration", "Mountain Trek & Scenic Views", "River Rafting Day", "Cycling the Countryside", "Summit Challenge", "Forest Trail & Waterfalls", "Final Adventure"],
    relaxed: ["Settle In & Stroll", "Beach & Spa Morning", "Leisurely Brunch & Gardens", "Art Gallery & Café Hopping", "Sunset Cruise", "Local Market & Cooking Class", "Farewell Day"],
    cultural: ["Historic Old Town Walk", "Museum & Heritage Tour", "Local Traditions Workshop", "Architecture & Landmarks", "Traditional Music & Dance", "Sacred Sites Visit", "Cultural Wrap-up"],
    foodie: ["Street Food Discovery", "Market & Cooking Class", "Fine Dining Experience", "Wine & Cheese Tour", "Bakery & Dessert Trail", "Farm-to-Table Visit", "Farewell Feast"],
  };
  const list = titles[style] ?? titles.adventure;
  return list[dayIdx % list.length];
}

function getDummyActivities(dayIdx: number, destination: string, style: string): Activity[] {
  const base: Activity[] = [
    { activityId: `ai-${dayIdx}-1-${Date.now()}`, time: "08:00", title: "Breakfast at hotel", category: "breakfast", location: "Hotel", estimatedCost: "15" },
    { activityId: `ai-${dayIdx}-2-${Date.now()}`, time: "09:30", title: style === "adventure" ? "Hiking trail exploration" : style === "cultural" ? "Guided museum tour" : style === "foodie" ? "Local market visit" : "Morning yoga session", category: "sightseeing", location: destination, estimatedCost: style === "adventure" ? "25" : "20" },
    { activityId: `ai-${dayIdx}-3-${Date.now()}`, time: "12:30", title: "Lunch at local restaurant", category: "lunch", location: `${destination} city center`, estimatedCost: "30" },
    { activityId: `ai-${dayIdx}-4-${Date.now()}`, time: "14:00", title: style === "adventure" ? "Rock climbing" : style === "cultural" ? "Heritage site visit" : style === "foodie" ? "Wine tasting" : "Spa treatment", category: "sightseeing", location: destination, estimatedCost: "40" },
    { activityId: `ai-${dayIdx}-5-${Date.now()}`, time: "19:00", title: "Dinner", category: "dinner", location: destination, estimatedCost: "45" },
  ];
  return base;
}

/* ─── Main Page Component ─────────────────────────────────────────── */
export default function TripDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { addToast } = useToast();
  const tripId = tripIdFromSlug(params.id as string);

  const [trip, setTrip] = useState<TripDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedDays, setExpandedDays] = useState<Set<number>>(new Set([1]));
  const [itineraryDays, setItineraryDays] = useState<DayPlan[]>([]);
  const [aiPanelOpen, setAiPanelOpen] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [currency, setCurrency] = useState("USD");

  useEffect(() => {
    if (!tripId) return;
    setLoading(true);
    tripsApi
      .get(tripId)
      .then(async (data) => {
        // Fetch participant display names from user-service
        if (data.participants && data.participants.length > 0) {
          try {
            const userIds = data.participants.map((p) => p.userId);
            const profiles = await usersApi.batchProfiles(userIds);
            const profileMap = new Map(profiles.map((p) => [p.id, p]));
            data.participants = data.participants.map((p) => {
              const profile = profileMap.get(p.userId);
              return {
                ...p,
                displayName: profile?.displayName ?? p.displayName,
                avatarUrl: profile?.avatarUrl ?? p.avatarUrl,
              };
            });
          } catch {
            // Silently fall back to missing names
          }
        }
        setTrip(data);
        if (data.itinerary?.days && data.itinerary.days.length > 0) {
          setItineraryDays(data.itinerary.days);
        } else {
          // Initialize empty days based on trip dates
          const numDays = getNumDays(data.startDate, data.endDate);
          if (numDays > 0) {
            setItineraryDays(
              Array.from({ length: numDays }, (_, i) => ({
                dayPlanId: `day-${i + 1}`,
                dayNumber: i + 1,
                title: "",
                activities: [],
              }))
            );
          }
        }
      })
      .catch(() => setError("Failed to load trip details"))
      .finally(() => setLoading(false));
  }, [tripId]);

  function getNumDays(startDate?: string, endDate?: string): number {
    if (!startDate || !endDate) return 0;
    const diff = new Date(endDate).getTime() - new Date(startDate).getTime();
    return Math.max(1, Math.ceil(diff / 86400000) + 1);
  }

  const numDays = getNumDays(trip?.startDate, trip?.endDate);

  function toggleDay(dayNumber: number) {
    setExpandedDays((prev) => {
      const next = new Set(prev);
      if (next.has(dayNumber)) next.delete(dayNumber);
      else next.add(dayNumber);
      return next;
    });
  }

  function updateDay(updated: DayPlan) {
    setItineraryDays((prev) => prev.map((d) => (d.dayNumber === updated.dayNumber ? updated : d)));
    setHasUnsavedChanges(true);
  }

  function handleAIGenerate(days: DayPlan[]) {
    setItineraryDays(days);
    setExpandedDays(new Set([1]));
    setHasUnsavedChanges(true);
    addToast("AI itinerary generated! Review and customize as needed.", "success");
  }

  function addDay() {
    const nextNum = itineraryDays.length + 1;
    setItineraryDays((prev) => [
      ...prev,
      { dayPlanId: `day-${nextNum}-${Date.now()}`, dayNumber: nextNum, title: "", activities: [] },
    ]);
    setExpandedDays((prev) => new Set([...prev, nextNum]));
    setHasUnsavedChanges(true);
  }

  function handleSave() {
    // Dummy save — just show success toast
    setHasUnsavedChanges(false);
    addToast("Itinerary saved successfully", "success");
  }

  async function handleDelete() {
    if (!confirm("Are you sure you want to delete this trip?")) return;
    try {
      await tripsApi.delete(tripId);
      addToast("Trip deleted", "success");
      router.push("/dashboard");
    } catch {
      addToast("Failed to delete trip", "error");
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 size={32} className="animate-spin text-accent-500" />
          <p className="text-sm text-muted">Loading trip details...</p>
        </div>
      </div>
    );
  }

  if (error || !trip) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
        <p className="text-muted">{error || "Trip not found"}</p>
        <Button variant="secondary" onClick={() => router.push("/dashboard")}>
          <ArrowLeft size={16} /> Back to trips
        </Button>
      </div>
    );
  }

  const totalEstimatedCost = itineraryDays.reduce(
    (sum, day) =>
      sum +
      day.activities.reduce((s, a) => {
        const c = parseFloat(a.estimatedCost ?? "0");
        return s + (isNaN(c) ? 0 : c);
      }, 0),
    0
  );

  return (
    <div className="space-y-8 pb-12">
      {/* Back link */}
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-2 text-sm text-muted hover:text-foreground transition-colors"
      >
        <ArrowLeft size={16} /> Back to trips
      </Link>

      {/* ─── Hero Header ──────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-trippy-600 via-trippy-700 to-trippy-800 p-8 sm:p-10"
      >
        {/* Decorative elements */}
        <div className="pointer-events-none absolute -right-12 -top-12 h-48 w-48 rounded-full bg-accent-500/10 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-8 -left-8 h-32 w-32 rounded-full bg-white/5 blur-2xl" />
        <div className="pointer-events-none absolute right-8 bottom-4 opacity-10">
          <Plane size={80} className="text-white rotate-12" />
        </div>

        <div className="relative z-10 flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 mb-2">
              <Badge variant={statusVariant[trip.status] ?? "default"}>
                {statusLabel[trip.status] ?? trip.status}
              </Badge>
              {trip.visibility === "PUBLIC" && (
                <span className="inline-flex items-center gap-1 rounded-full bg-white/10 px-2.5 py-0.5 text-[10px] font-medium text-white/70">
                  <Globe size={10} /> Public
                </span>
              )}
            </div>
            <h1 className="text-3xl font-bold text-white sm:text-4xl">{trip.title}</h1>
            {trip.description && (
              <p className="mt-2 text-sm text-white/60 max-w-xl">{trip.description}</p>
            )}

            {/* Quick stats */}
            <div className="flex flex-wrap items-center gap-4 mt-5">
              <div className="flex items-center gap-2 text-white/80">
                <MapPin size={14} className="text-accent-400" />
                <span className="text-sm font-medium">{trip.destination}</span>
              </div>
              {trip.startDate && trip.endDate && (
                <div className="flex items-center gap-2 text-white/80">
                  <Calendar size={14} className="text-accent-400" />
                  <span className="text-sm">
                    {new Date(trip.startDate).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                    {" — "}
                    {new Date(trip.endDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                  </span>
                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] font-medium text-white/60">
                    {numDays} day{numDays !== 1 ? "s" : ""}
                  </span>
                </div>
              )}
              <div className="flex items-center gap-2 text-white/80">
                <Users size={14} className="text-accent-400" />
                <span className="text-sm">{trip.participantCount} member{trip.participantCount !== 1 ? "s" : ""}</span>
              </div>
              {totalEstimatedCost > 0 && (
                <div className="flex items-center gap-2 text-white/80">
                  <DollarSign size={14} className="text-accent-400" />
                  <span className="text-sm font-medium">~{currencies.find((c) => c.code === currency)?.symbol ?? "$"}{totalEstimatedCost.toFixed(0)} est.</span>
                </div>
              )}
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap gap-2">
            <Link href={`/dashboard/chat/${trip.tripId}`}>
              <Button variant="secondary" size="sm" className="bg-white/10 border-white/20 text-white hover:bg-white/20">
                <MessageSquare size={14} /> Chat
              </Button>
            </Link>
            <Button variant="secondary" size="sm" className="bg-white/10 border-white/20 text-white hover:bg-white/20">
              <Edit size={14} /> Edit
            </Button>
            <Button variant="danger" size="sm" onClick={handleDelete} className="bg-red-500/80 border-red-400/30 hover:bg-red-500">
              <Trash2 size={14} /> Delete
            </Button>
          </div>
        </div>
      </motion.div>

      {/* ─── Team Section ──────────────────────────────────────────── */}
      {trip.participants && trip.participants.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <GlassCard className="!p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Users size={15} className="text-accent-500" />
                <h3 className="text-sm font-bold text-foreground">Team</h3>
                <span className="text-[10px] text-muted bg-shore-100 px-2 py-0.5 rounded-full">
                  {trip.participants.length} member{trip.participants.length !== 1 ? "s" : ""}
                </span>
              </div>
              <Button variant="secondary" size="sm" className="text-xs">
                <Plus size={12} /> Invite
              </Button>
            </div>
            <div className="flex flex-wrap gap-3">
              {trip.participants.map((p) => {
                const name = p.displayName ?? "User";
                const initials = name
                  .split(" ")
                  .map((n) => n[0])
                  .join("")
                  .toUpperCase()
                  .slice(0, 2);
                const isOwner = p.role === "OWNER";
                // Dummy stats for hover card display
                const tripsCount = Math.floor(Math.random() * 12) + 1;
                const friendliness = Math.floor(Math.random() * 3) + 3;

                return (
                  <div key={p.participantId} className="group/member relative">
                    {/* Avatar + name chip */}
                    <div className="flex items-center gap-2 rounded-xl border border-border bg-white px-3 py-2 transition-all hover:border-accent-300 hover:shadow-sm cursor-default">
                      <div className={cn(
                        "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold border-2",
                        isOwner
                          ? "bg-gradient-to-br from-accent-100 to-accent-200 text-accent-700 border-accent-300"
                          : "bg-shore-100 text-trippy-600 border-border"
                      )}>
                        {p.avatarUrl ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img src={p.avatarUrl} alt={name} className="w-full h-full rounded-full object-cover" />
                        ) : (
                          initials
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1">
                          <span className="text-xs font-semibold text-foreground truncate max-w-[100px]">{name}</span>
                          {isOwner && <Crown size={10} className="text-accent-500 shrink-0" />}
                        </div>
                        <span className="text-[10px] text-muted capitalize">{p.role.toLowerCase()}</span>
                      </div>
                    </div>

                    {/* Hover profile card */}
                    <div className="pointer-events-none absolute left-1/2 -translate-x-1/2 bottom-full mb-2 z-30 w-56 rounded-2xl border border-border bg-white p-4 shadow-2xl opacity-0 scale-95 transition-all duration-200 group-hover/member:opacity-100 group-hover/member:scale-100 group-hover/member:pointer-events-auto">
                      <div className="flex items-center gap-3 mb-3">
                        <div className={cn(
                          "flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold",
                          isOwner
                            ? "bg-gradient-to-br from-accent-200 to-accent-300 text-accent-800"
                            : "bg-shore-100 text-trippy-600"
                        )}>
                          {initials}
                        </div>
                        <div>
                          <p className="text-sm font-bold text-foreground">{name}</p>
                          <p className="text-[10px] text-muted capitalize flex items-center gap-1">
                            {isOwner && <Crown size={9} className="text-accent-500" />}
                            {p.role.toLowerCase()}
                            {p.status === "ACCEPTED" && " · Active"}
                            {p.status === "PENDING" && " · Pending"}
                          </p>
                        </div>
                      </div>
                      <div className="space-y-2 border-t border-border/60 pt-3">
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] text-muted flex items-center gap-1.5">
                            <Map size={10} /> Trips together
                          </span>
                          <span className="text-[10px] font-bold text-foreground">{tripsCount}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] text-muted flex items-center gap-1.5">
                            <Heart size={10} /> Friendliness
                          </span>
                          <div className="flex gap-0.5">
                            {Array.from({ length: 5 }, (_, i) => (
                              <div
                                key={i}
                                className={cn(
                                  "h-1.5 w-1.5 rounded-full",
                                  i < friendliness ? "bg-accent-500" : "bg-shore-200"
                                )}
                              />
                            ))}
                          </div>
                        </div>
                        {p.joinedAt && (
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] text-muted flex items-center gap-1.5">
                              <Calendar size={10} /> Joined
                            </span>
                            <span className="text-[10px] font-medium text-foreground">
                              {new Date(p.joinedAt).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </GlassCard>
        </motion.div>
      )}

      {/* ─── Itinerary Builder Section ────────────────────────────── */}
      <motion.section
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className="space-y-5"
      >
        {/* Section header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-accent-400 to-accent-600 shadow-md shadow-accent-500/20">
              <Calendar size={16} className="text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold">Itinerary</h2>
              <p className="text-xs text-muted">
                {itineraryDays.length > 0
                  ? `${itineraryDays.length} day${itineraryDays.length !== 1 ? "s" : ""} planned`
                  : "Plan your day-by-day adventure"}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {hasUnsavedChanges && (
              <Button size="sm" onClick={handleSave}>
                <Save size={14} /> Save
              </Button>
            )}
            <button
              onClick={() => setAiPanelOpen(true)}
              className={cn(
                "flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all cursor-pointer",
                "bg-gradient-to-r from-trippy-600 to-trippy-700 text-white shadow-md shadow-trippy-500/20",
                "hover:shadow-lg hover:-translate-y-0.5"
              )}
            >
              <Sparkles size={14} />
              AI Generate
            </button>
          </div>
        </div>

        {/* Days list */}
        {itineraryDays.length > 0 ? (
          <div className="space-y-4">
            {itineraryDays.map((day) => (
              <DayCard
                key={day.dayPlanId}
                day={day}
                tripStartDate={trip.startDate}
                expanded={expandedDays.has(day.dayNumber)}
                onToggle={() => toggleDay(day.dayNumber)}
                onUpdateDay={updateDay}
                currency={currency}
                onCurrencyChange={(c) => { setCurrency(c); setHasUnsavedChanges(true); }}
              />
            ))}

            {/* Add day button */}
            <button
              onClick={addDay}
              className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-border py-5 text-sm font-medium text-muted transition-all hover:border-accent-400 hover:text-accent-600 hover:bg-accent-50/30 cursor-pointer"
            >
              <Plus size={16} /> Add another day
            </button>
          </div>
        ) : (
          /* Empty itinerary state */
          <GlassCard className="!py-12 flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-gradient-to-br from-accent-100 to-shore-100 mb-4">
              <Calendar size={28} className="text-accent-500" />
            </div>
            <h3 className="text-lg font-bold">No itinerary yet</h3>
            <p className="text-sm text-muted mt-1 max-w-sm">
              {numDays > 0
                ? `You have ${numDays} days to plan. Add days manually or let AI create a complete itinerary for you.`
                : "Set your trip dates first, then plan your day-by-day adventure here."}
            </p>
            <div className="flex items-center gap-3 mt-5">
              {numDays > 0 && (
                <Button variant="secondary" size="sm" onClick={addDay}>
                  <Plus size={14} /> Add first day
                </Button>
              )}
              <button
                onClick={() => setAiPanelOpen(true)}
                className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-trippy-600 to-trippy-700 px-4 py-2.5 text-sm font-semibold text-white shadow-md hover:shadow-lg hover:-translate-y-0.5 transition-all cursor-pointer"
              >
                <Sparkles size={14} /> Generate with AI
              </button>
            </div>
          </GlassCard>
        )}
      </motion.section>

      {/* AI Generate Panel */}
      <AIGeneratePanel
        open={aiPanelOpen}
        onClose={() => setAiPanelOpen(false)}
        tripTitle={trip.title}
        destination={trip.destination}
        numDays={numDays > 0 ? numDays : 5}
        onGenerate={handleAIGenerate}
      />
    </div>
  );
}
