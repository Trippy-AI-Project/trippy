"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  X,
  MapPin,
  Calendar,
  Users,
  Sparkles,
  Loader2,
  Check,
  Star,
  DollarSign,
  SlidersHorizontal,
  MessageSquare,
  Send,
  ArrowRightLeft,
  Plus,
  Trash2,
  Lightbulb,
  Undo2,
  Plane,
  Globe,
  Compass,
} from "lucide-react";
import Button from "@/components/ui/Button";
import GlassCard from "@/components/ui/GlassCard";
import TripFullScreenView from "@/components/ai/TripFullScreenView";
import { getAccessToken } from "@/lib/api";

/* ── Beautiful Loading Screen ─────────────────────────────────────── */
const LOADING_MESSAGES = [
  { emoji: "✈️", text: "Booking your imagination..." },
  { emoji: "🗺️", text: "Mapping hidden gems..." },
  { emoji: "🌍", text: "Exploring destinations..." },
  { emoji: "🍽️", text: "Finding the best local food..." },
  { emoji: "🏛️", text: "Discovering cultural treasures..." },
  { emoji: "🌅", text: "Planning perfect moments..." },
  { emoji: "📸", text: "Scouting Instagram spots..." },
  { emoji: "🧳", text: "Packing your itinerary..." },
  { emoji: "☕", text: "Finding cozy cafés..." },
  { emoji: "🎭", text: "Curating unique experiences..." },
];

function TripLoadingScreen({ destination }: { destination: string }) {
  const [msgIndex, setMsgIndex] = useState(0);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setMsgIndex((prev) => (prev + 1) % LOADING_MESSAGES.length);
    }, 2800);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const interval = setInterval(() => {
      setProgress((prev) => Math.min(prev + Math.random() * 4 + 1, 92));
    }, 500);
    return () => clearInterval(interval);
  }, []);

  const msg = LOADING_MESSAGES[msgIndex];

  return (
    <motion.div
      className="fixed inset-0 z-[100] flex items-center justify-center"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-br from-[#f0f7f4] via-white to-[#e8f5e9]" />

      {/* Floating travel icons */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {[Plane, Globe, Compass, MapPin, Star].map((Icon, i) => (
          <motion.div
            key={i}
            className="absolute text-trippy-500/10"
            style={{
              left: `${15 + i * 18}%`,
              top: `${20 + (i % 3) * 25}%`,
            }}
            animate={{
              y: [0, -20, 0],
              rotate: [0, i % 2 === 0 ? 10 : -10, 0],
              scale: [1, 1.1, 1],
            }}
            transition={{
              duration: 3 + i * 0.5,
              repeat: Infinity,
              ease: "easeInOut",
              delay: i * 0.4,
            }}
          >
            <Icon size={32 + i * 8} strokeWidth={1.2} />
          </motion.div>
        ))}
      </div>

      {/* Center content */}
      <div className="relative z-10 flex flex-col items-center gap-8 max-w-md px-6">
        {/* Animated airplane circle */}
        <div className="relative w-28 h-28">
          {/* Orbit ring */}
          <motion.div
            className="absolute inset-0 rounded-full border-2 border-dashed border-trippy-500/20"
            animate={{ rotate: 360 }}
            transition={{ duration: 12, repeat: Infinity, ease: "linear" }}
          />
          {/* Inner glow */}
          <motion.div
            className="absolute inset-3 rounded-full bg-gradient-to-br from-trippy-500/15 to-accent-400/10"
            animate={{ scale: [1, 1.08, 1] }}
            transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
          />
          {/* Center icon */}
          <motion.div
            className="absolute inset-0 flex items-center justify-center"
            animate={{ rotate: [0, 5, -5, 0] }}
            transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          >
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-trippy-500 to-trippy-600 flex items-center justify-center shadow-lg shadow-trippy-500/30">
              <Plane size={28} className="text-white -rotate-45" />
            </div>
          </motion.div>
          {/* Orbiting dot */}
          <motion.div
            className="absolute w-3 h-3 rounded-full bg-accent-500 shadow-sm shadow-accent-500/50"
            style={{ top: 0, left: "50%", marginLeft: -6 }}
            animate={{ rotate: 360 }}
            transition={{ duration: 3, repeat: Infinity, ease: "linear" }}
          />
        </div>

        {/* Destination text */}
        <div className="text-center space-y-2">
          <motion.h2
            className="text-xl font-bold text-foreground"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            {destination ? `Planning your trip to ${destination}` : "Crafting your perfect trip"}
          </motion.h2>
          <p className="text-sm text-muted">This usually takes a few seconds</p>
        </div>

        {/* Rotating message */}
        <AnimatePresence mode="wait">
          <motion.div
            key={msgIndex}
            className="flex items-center gap-2.5 bg-white/80 backdrop-blur-sm border border-border/50 rounded-full px-5 py-2.5 shadow-sm"
            initial={{ opacity: 0, y: 12, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -12, scale: 0.95 }}
            transition={{ duration: 0.35 }}
          >
            <span className="text-base">{msg.emoji}</span>
            <span className="text-sm font-medium text-foreground/80">{msg.text}</span>
          </motion.div>
        </AnimatePresence>

        {/* Progress bar */}
        <div className="w-56 space-y-2">
          <div className="h-1.5 bg-trippy-500/10 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-gradient-to-r from-trippy-500 to-accent-500 rounded-full"
              initial={{ width: "0%" }}
              animate={{ width: `${progress}%` }}
              transition={{ duration: 0.4, ease: "easeOut" }}
            />
          </div>
        </div>
      </div>
    </motion.div>
  );
}

interface AiItineraryDay {
  dayNumber: number;
  date?: string;
  title: string;
  activities: { time?: string; title: string; description?: string; location?: string; estimatedCost?: string }[];
}

interface GeneratedTrip {
  title: string;
  destination: string;
  duration: string;
  budget: string;
  groupSize: string;
  rating: number;
  highlights: string[];
  reason: string;
  bestTimeToVisit: string;
  image: string;
  aiItinerary?: AiItineraryDay[];
}

interface DestinationSuggestionItem {
  city?: string;
  country?: string;
  estimatedDailyCost?: string;
  bestTimeToVisit?: string;
  highlights?: string[];
  reason?: string;
  matchScore?: number;
}

export interface AIBuilderRequest {
  requestId: number;
  city?: string;
  startDate?: string;
  endDate?: string;
  people?: number;
  budget?: string;
  filters?: string[];
  diet?: string;
  preferences?: string;
  customNotes?: string;
  autoGenerate?: boolean;
}

interface AITripBuilderModalProps {
  open: boolean;
  onClose: () => void;
  initialRequest?: AIBuilderRequest;
}

const QUICK_FILTERS = [
  "Beach",
  "Adventure",
  "City",
  "Family",
  "Romantic",
  "Food",
  "Nature",
  "Wellness",
  "Culture",
  "Budget",
];

function formatDestination(suggestion: DestinationSuggestionItem): string {
  const city = suggestion.city?.trim();
  const country = suggestion.country?.trim();
  if (city && country) return `${city}, ${country}`;
  if (city) return city;
  if (country) return country;
  return "Recommended Destination";
}

function daysBetween(startDate: string, endDate: string): number {
  if (!startDate || !endDate) return 0;
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  return Math.max(1, Number.isFinite(diff) ? diff + 1 : 1);
}

// Placeholder shown while real city image loads from Wikipedia API
const PLACEHOLDER_IMAGE = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&auto=format&fit=crop&q=80";

function toTripCardFromSuggestion(
  suggestion: DestinationSuggestionItem,
  people: number,
  startDate: string,
  endDate: string
): GeneratedTrip {
  const destination = formatDestination(suggestion);
  const highlights = suggestion.highlights?.length
    ? suggestion.highlights.slice(0, 6)
    : ["Local attractions", "Cultural sites", "Cuisine", "Scenic spots"];
  const tripDays = daysBetween(startDate, endDate);

  return {
    title: `${destination} — ${tripDays}-Day Trip`,
    destination,
    duration: `${tripDays} days`,
    budget: suggestion.estimatedDailyCost || "Flexible budget",
    groupSize: `${people}`,
    rating: parseFloat(Math.min(5.0, 3.5 + (suggestion.matchScore ?? 0.75) * 1.5).toFixed(1)),
    highlights,
    reason: suggestion.reason || "",
    bestTimeToVisit: suggestion.bestTimeToVisit || "",
    image: PLACEHOLDER_IMAGE, // Will be replaced dynamically by TripResultCard
  };
}

interface AiRequestPayload {
  prompt: string;
  city?: string;
  budget?: string;
  durationDays?: number;
  people?: number;
  interests?: string[];
  diet?: string;
  preferences?: string;
  customNotes?: string;
}

async function fetchAiSuggestions(payload: AiRequestPayload): Promise<DestinationSuggestionItem[]> {
  const response = await fetch("/api/ai/destination-suggestions", {
    method: "POST",
    headers: aiRequestHeaders(),
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errData = await response.json().catch(() => null);
    const errMsg = errData?.error || `AI request failed (${response.status})`;
    throw new Error(errMsg);
  }

  const data = await response.json();
  const suggestions = Array.isArray(data?.suggestions) ? data.suggestions : [];
  if (data?.error) {
    throw new Error(data.error);
  }
  return suggestions;
}

function aiRequestHeaders(): Record<string, string> {
  const token = getAccessToken();
  return token
    ? { "Content-Type": "application/json", Authorization: `Bearer ${token}` }
    : { "Content-Type": "application/json" };
}

/* ── Date Range Picker ────────────────────────────────────────────── */
function DateRangePicker({
  startDate,
  endDate,
  onChange,
}: {
  startDate: string;
  endDate: string;
  onChange: (start: string, end: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(() => {
    const d = startDate ? new Date(startDate) : new Date();
    return d.getFullYear();
  });
  const [viewMonth, setViewMonth] = useState(() => {
    const d = startDate ? new Date(startDate) : new Date();
    return d.getMonth();
  });
  const [hoverDate, setHoverDate] = useState("");
  const pickerRef = useRef<HTMLDivElement>(null);
  const today = new Date().toISOString().slice(0, 10);

  useEffect(() => {
    if (!open) return;
    function onMouseDown(e: MouseEvent) {
      if (pickerRef.current && !pickerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [open]);

  function toDateStr(y: number, m: number, d: number) {
    return `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  }

  function handleDayClick(dateStr: string) {
    if (dateStr < today) return;
    if (!startDate || (startDate && endDate)) {
      onChange(dateStr, "");
    } else if (dateStr < startDate) {
      onChange(dateStr, startDate);
      setOpen(false);
    } else if (dateStr === startDate) {
      onChange("", "");
    } else {
      onChange(startDate, dateStr);
      setOpen(false);
    }
  }

  function isInRange(d: string) {
    const s = startDate;
    const e = endDate || hoverDate;
    if (!s || !e) return false;
    const [lo, hi] = s <= e ? [s, e] : [e, s];
    return d > lo && d < hi;
  }

  function prevMonth() {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  }
  function nextMonth() {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  }

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDay = new Date(viewYear, viewMonth, 1).getDay();
  const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
  const DAY_NAMES = ["Su","Mo","Tu","We","Th","Fr","Sa"];

  let displayLabel = "Select travel dates";
  if (startDate && endDate) {
    const s = new Date(startDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    const e = new Date(endDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    const nights = Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000);
    displayLabel = `${s} → ${e}  ·  ${nights} night${nights !== 1 ? "s" : ""}`;
  } else if (startDate) {
    const s = new Date(startDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" });
    displayLabel = `${s} → pick return date`;
  }

  return (
    <div className="relative" ref={pickerRef}>
      <button
        type="button"
        onClick={() => setOpen(p => !p)}
        className={`w-full flex items-center gap-2 rounded-lg border bg-transparent px-3 py-2 text-sm outline-none text-left transition-colors cursor-pointer ${
          open ? "border-trippy-500/60 bg-trippy-500/5" : "border-border hover:border-trippy-500/40"
        } ${!startDate && !endDate ? "text-muted/70" : "text-foreground"}`}
      >
        <Calendar size={14} className="text-trippy-500 shrink-0" />
        <span className="flex-1 truncate">{displayLabel}</span>
        {(startDate || endDate) && (
          <span
            role="button"
            tabIndex={0}
            onClick={(e) => { e.stopPropagation(); onChange("", ""); }}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.stopPropagation(); onChange("", ""); } }}
            className="text-muted hover:text-foreground transition-colors shrink-0 cursor-pointer"
          >
            <X size={12} />
          </span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.97 }}
            transition={{ duration: 0.15 }}
            className="absolute top-full left-0 mt-1.5 z-[200] bg-white border border-border rounded-2xl shadow-2xl p-4 w-[300px]"
          >
            {/* Month navigation */}
            <div className="flex items-center justify-between mb-3">
              <button type="button" onClick={prevMonth}
                className="w-8 h-8 rounded-xl flex items-center justify-center hover:bg-surface text-muted hover:text-foreground transition-colors cursor-pointer text-lg"
              >‹</button>
              <span className="text-sm font-semibold text-foreground">{MONTHS[viewMonth]} {viewYear}</span>
              <button type="button" onClick={nextMonth}
                className="w-8 h-8 rounded-xl flex items-center justify-center hover:bg-surface text-muted hover:text-foreground transition-colors cursor-pointer text-lg"
              >›</button>
            </div>

            {/* Day names */}
            <div className="grid grid-cols-7 mb-1">
              {DAY_NAMES.map(d => (
                <div key={d} className="text-center text-[10px] text-muted font-semibold py-1">{d}</div>
              ))}
            </div>

            {/* Day cells */}
            <div className="grid grid-cols-7">
              {Array.from({ length: firstDay }, (_, i) => <div key={`e${i}`} />)}
              {Array.from({ length: daysInMonth }, (_, i) => {
                const day = i + 1;
                const ds = toDateStr(viewYear, viewMonth, day);
                const isPast = ds < today;
                const isToday = ds === today;
                const isStart = ds === startDate;
                const isEnd = ds === endDate;
                const inRange = isInRange(ds);
                const isSelected = isStart || isEnd;

                return (
                  <div key={day} className={`relative h-8 flex items-center justify-center ${inRange ? "bg-trippy-500/10" : ""} ${isStart && endDate ? "rounded-l-full" : ""} ${isEnd ? "rounded-r-full" : ""} ${isStart && !endDate ? "rounded-full" : ""}`}>
                    <button
                      type="button"
                      disabled={isPast}
                      onClick={() => handleDayClick(ds)}
                      onMouseEnter={() => { if (startDate && !endDate) setHoverDate(ds); }}
                      onMouseLeave={() => setHoverDate("")}
                      className={`w-8 h-8 rounded-full text-xs transition-all relative
                        ${isPast ? "text-muted/30 cursor-not-allowed" : "cursor-pointer"}
                        ${isSelected ? "bg-trippy-500 text-white font-bold shadow-sm" : ""}
                        ${!isPast && !isSelected ? "hover:bg-trippy-500/15 text-foreground" : ""}
                        ${isToday && !isSelected ? "font-bold text-trippy-600" : ""}
                        ${inRange && !isSelected ? "text-trippy-700 font-medium" : ""}
                      `}
                    >
                      {day}
                      {isToday && !isSelected && (
                        <span className="absolute bottom-0.5 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-trippy-500" />
                      )}
                    </button>
                  </div>
                );
              })}
            </div>

            {/* Status */}
            <div className="mt-3 pt-2.5 border-t border-border/50 text-center">
              {!startDate ? (
                <p className="text-[11px] text-muted">Tap a date to start</p>
              ) : !endDate ? (
                <p className="text-[11px] text-trippy-600 font-medium">Now pick your return date</p>
              ) : (
                <p className="text-[11px] text-green-600 font-medium">
                  ✓ {Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000)} nights selected
                </p>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default function AITripBuilderModal({ open, onClose, initialRequest }: AITripBuilderModalProps) {
  const [city, setCity] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [people, setPeople] = useState(2);

  const [budget, setBudget] = useState("");
  const [diet, setDiet] = useState("");
  const [preferences, setPreferences] = useState("");
  const [customPreference, setCustomPreference] = useState("");
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [showFormExpanded, setShowFormExpanded] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [reply, setReply] = useState("");
  const [results, setResults] = useState<GeneratedTrip[]>([]);
  const [alsoExplore, setAlsoExplore] = useState<DestinationSuggestionItem[]>([]);
  const [savedTrips, setSavedTrips] = useState<Set<string>>(new Set());
  const [fullScreenTrip, setFullScreenTrip] = useState<GeneratedTrip | null>(null);

  const lastAutoRequestId = useRef<number>(0);

  const promptPreview = useMemo(() => {
    const parts: string[] = [];

    if (city.trim()) {
      parts.push(`Plan a trip to ${city.trim()}.`);
    } else {
      parts.push("Suggest travel destinations.");
    }

    if (startDate && endDate) parts.push(`Travel dates: ${startDate} to ${endDate}.`);
    if (people) parts.push(`${people} traveler(s).`);
    if (selectedFilters.length) parts.push(`Trip style: ${selectedFilters.join(", ")}.`);
    if (budget) parts.push(`Budget: ${budget}.`);
    if (diet) parts.push(`Diet: ${diet}.`);
    if (preferences) parts.push(`Preferences: ${preferences}.`);
    if (customPreference) parts.push(`Notes: ${customPreference}.`);

    if (city.trim()) {
      parts.push(`Focus ONLY on ${city.trim()} as the destination.`);
    } else {
      parts.push("Return 3-5 diverse destination suggestions.");
    }

    return parts.join(" ");
  }, [city, startDate, endDate, people, selectedFilters, budget, diet, preferences, customPreference]);

  const pendingAutoGenerate = useRef(false);

  useEffect(() => {
    if (!open) return;

    if (initialRequest?.requestId) {
      setCity(initialRequest.city || "");
      setStartDate(initialRequest.startDate || "");
      setEndDate(initialRequest.endDate || "");
      setPeople(initialRequest.people || 2);
      setBudget(initialRequest.budget || "");
      setSelectedFilters(initialRequest.filters || []);
      setDiet(initialRequest.diet || "");
      setPreferences(initialRequest.preferences || "");
      setCustomPreference(initialRequest.customNotes || "");
      setShowAdvanced(Boolean(initialRequest.budget || initialRequest.diet || initialRequest.preferences || initialRequest.customNotes));

      if (
        initialRequest.autoGenerate &&
        initialRequest.requestId !== lastAutoRequestId.current &&
        initialRequest.startDate &&
        initialRequest.endDate
      ) {
        lastAutoRequestId.current = initialRequest.requestId;
        pendingAutoGenerate.current = true;
      }
    }
  }, [open, initialRequest]);

  // Trigger auto-generate after state has settled (fixes stale closure bug)
  useEffect(() => {
    if (pendingAutoGenerate.current && !isLoading) {
      pendingAutoGenerate.current = false;
      void handleGenerate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [city, startDate, endDate, people, selectedFilters, budget, diet, preferences, customPreference, isLoading]);

  useEffect(() => {
    if (!open) {
      const timer = setTimeout(() => {
        setError("");
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [open]);

  function toggleFilter(filter: string) {
    setSelectedFilters((prev) =>
      prev.includes(filter) ? prev.filter((f) => f !== filter) : [...prev, filter]
    );
  }

  function handleSave(title: string) {
    setSavedTrips((prev) => new Set(prev).add(title));
  }

  // Minimum loading time to let the animation play
  const MIN_LOADING_MS = 10000;
  const loadingStartRef = useRef<number>(0);

  const waitMinimumLoading = useCallback(async () => {
    const elapsed = Date.now() - loadingStartRef.current;
    const remaining = MIN_LOADING_MS - elapsed;
    if (remaining > 0) {
      await new Promise((resolve) => setTimeout(resolve, remaining));
    }
  }, []);

  async function handleGenerate() {
    if (isLoading) return;
    if (!startDate || !endDate) return;

    setIsLoading(true);
    setError("");
    setReply("");
    setFullScreenTrip(null);
    loadingStartRef.current = Date.now();

    try {
      // Build structured payload with ALL user preferences for the AI
      const tripDays = startDate && endDate ? daysBetween(startDate, endDate) : undefined;
      const payload: AiRequestPayload = {
        prompt: promptPreview,
        city: city.trim() || undefined,
        budget: budget || undefined,
        durationDays: tripDays,
        people: people || undefined,
        interests: selectedFilters.length > 0 ? selectedFilters : undefined,
        diet: diet || undefined,
        preferences: preferences || undefined,
        customNotes: customPreference || undefined,
      };

      const suggestions = await fetchAiSuggestions(payload);

      // Ensure loading animation shows for at least MIN_LOADING_MS
      await waitMinimumLoading();

      if (!suggestions.length) {
        setResults([]);
        setAlsoExplore([]);
        setReply("No results found. Try changing city, filters, or preferences.");
        return;
      }

      const searchCity = city.trim().toLowerCase();
      let primarySuggestions: DestinationSuggestionItem[];
      let secondarySuggestions: DestinationSuggestionItem[];

      if (searchCity) {
        const primary = suggestions.find(
          (s) => s.city?.toLowerCase().trim() === searchCity
        ) ?? suggestions.find(
          (s) => s.city?.toLowerCase().trim().includes(searchCity) || searchCity.includes(s.city?.toLowerCase().trim() || "__")
        ) ?? suggestions[0];
        primarySuggestions = [primary];
        // Deduplicate "Also explore" by city — remove same city and duplicates
        const seen = new Set<string>([primary.city?.toLowerCase().trim() || ""]);
        secondarySuggestions = suggestions.filter((s) => {
          const key = s.city?.toLowerCase().trim() || "";
          if (s === primary || seen.has(key)) return false;
          seen.add(key);
          return true;
        });
      } else {
        primarySuggestions = suggestions;
        secondarySuggestions = [];
      }

      const mapped = primarySuggestions.map((s) =>
        toTripCardFromSuggestion(s, people, startDate, endDate)
      );

      setResults(mapped);
      setAlsoExplore(secondarySuggestions);

      const destNames = primarySuggestions.map((s) => formatDestination(s)).join(", ");
      setReply(
        city
          ? `Here is your AI-generated trip plan for ${destNames}.`
          : `Here are ${primarySuggestions.length} AI-generated trip option${primarySuggestions.length > 1 ? "s" : ""} based on your inputs.`
      );

      // Auto-open the first trip in fullscreen view
      if (mapped.length > 0) {
        setFullScreenTrip(mapped[0]);
      }
    } catch (err) {
      // Ensure minimum loading time even on errors
      await waitMinimumLoading();
      const message = err instanceof Error ? err.message : "Could not connect to AI service.";
      setError(message);
      setResults([]);
    } finally {
      setIsLoading(false);
    }
  }

  if (!open) return null;

  // Beautiful loading screen during trip generation
  if (isLoading) {
    return (
      <AnimatePresence>
        <TripLoadingScreen destination={city.trim()} />
      </AnimatePresence>
    );
  }

  // Full-screen split view when a trip is selected
  if (fullScreenTrip) {
    return (
      <AnimatePresence>
        <TripFullScreenView
          trip={fullScreenTrip}
          userPrompt={promptPreview}
          userDates={{ start: startDate, end: endDate }}
          onBack={() => onClose()}
          onClose={onClose}
          onSave={() => handleSave(fullScreenTrip.title)}
          saved={savedTrips.has(fullScreenTrip.title)}
          onRegenerate={() => {
            setFullScreenTrip(null);
            void handleGenerate();
          }}
          onEditSearch={() => {
            setFullScreenTrip(null);
            setShowFormExpanded(true);
          }}
        />
      </AnimatePresence>
    );
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="absolute inset-0 bg-black/30 backdrop-blur-sm"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />

          <motion.div
            className="relative z-10 w-full max-w-4xl h-[90vh] max-h-[820px] flex flex-col rounded-2xl overflow-hidden"
            initial={{ opacity: 0, y: 30, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 30, scale: 0.97 }}
            transition={{ type: "spring", damping: 24, stiffness: 260 }}
          >
            <GlassCard variant="strong" className="flex flex-col h-full !p-0 !rounded-2xl">
              <div className="flex items-center justify-between px-5 py-4 border-b border-border">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-trippy-500 flex items-center justify-center">
                    <Sparkles size={18} className="text-white" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm">AI Trip Builder</h3>
                    <p className="text-xs text-muted">Minimal planner with optional fields</p>
                  </div>
                </div>
                <button
                  onClick={onClose}
                  className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-surface transition-colors cursor-pointer"
                >
                  <X size={16} className="text-muted" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-5 space-y-4">
                {/* Compact summary bar when results exist or loading */}
                {(results.length > 0 || isLoading) && !showFormExpanded ? (
                  <div className="glass rounded-xl px-4 py-3 flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-2 text-sm">
                      <MapPin size={14} className="text-trippy-500" />
                      <span className="font-medium">{city || "Anywhere"}</span>
                    </div>
                    {startDate && endDate && (
                      <div className="flex items-center gap-1.5 text-xs text-muted">
                        <Calendar size={12} />
                        {new Date(startDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" })} → {new Date(endDate + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" })}
                      </div>
                    )}
                    <div className="flex items-center gap-1.5 text-xs text-muted">
                      <Users size={12} />
                      {people} {people === 1 ? "person" : "people"}
                    </div>
                    {selectedFilters.length > 0 && (
                      <div className="flex items-center gap-1 text-xs text-muted">
                        {selectedFilters.slice(0, 3).map((f) => (
                          <span key={f} className="bg-trippy-500/10 text-trippy-600 px-2 py-0.5 rounded-full text-[10px]">{f}</span>
                        ))}
                        {selectedFilters.length > 3 && <span className="text-[10px]">+{selectedFilters.length - 3}</span>}
                      </div>
                    )}
                    <div className="ml-auto flex items-center gap-2">
                      <button
                        onClick={() => setShowFormExpanded(true)}
                        className="text-xs text-trippy-600 hover:underline cursor-pointer"
                      >
                        Edit search
                      </button>
                      <Button size="sm" onClick={() => void handleGenerate()} disabled={isLoading || !startDate || !endDate} title={!startDate || !endDate ? "Select travel dates first" : undefined} className="text-xs">
                        {isLoading ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />}
                        {isLoading ? "Generating…" : "Regenerate"}
                      </Button>
                    </div>
                  </div>
                ) : (
                <div className="glass rounded-xl p-4 space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <label className="text-xs text-muted block">
                      City
                      <input
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        placeholder="Delhi, Paris, Tokyo..."
                        className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50"
                      />
                    </label>

                    <label className="text-xs text-muted block">
                      People
                      <input
                        type="number"
                        min={1}
                        max={20}
                        value={people}
                        onChange={(e) => setPeople(Math.max(1, Number(e.target.value || 1)))}
                        className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50"
                      />
                    </label>
                  </div>

                  <div>
                    <p className="text-xs text-muted mb-1">
                      Travel Dates <span className="text-red-400">*</span>
                    </p>
                    <DateRangePicker
                      startDate={startDate}
                      endDate={endDate}
                      onChange={(s, e) => { setStartDate(s); setEndDate(e); }}
                    />
                    {(!startDate || !endDate) && (
                      <p className="text-[10px] text-red-400 mt-1">Select your travel dates to continue</p>
                    )}
                  </div>

                  <div>
                    <p className="text-xs text-muted mb-2">Direct filters</p>
                    <div className="flex flex-wrap gap-2">
                      {QUICK_FILTERS.map((filter) => {
                        const active = selectedFilters.includes(filter);
                        return (
                          <button
                            key={filter}
                            onClick={() => toggleFilter(filter)}
                            className={`px-3 py-1.5 text-xs rounded-full border transition-colors cursor-pointer ${active
                                ? "bg-trippy-500/15 border-trippy-500/30 text-foreground"
                                : "bg-transparent border-border text-muted hover:text-foreground"
                              }`}
                          >
                            {filter}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <button
                    onClick={() => setShowAdvanced((prev) => !prev)}
                    className="text-xs text-trippy-600 font-medium flex items-center gap-1 cursor-pointer"
                  >
                    <SlidersHorizontal size={12} />
                    {showAdvanced ? "Hide optional preferences" : "Add optional preferences"}
                  </button>

                  <AnimatePresence>
                    {showAdvanced && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        className="overflow-hidden"
                      >
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                          <label className="text-xs text-muted block">
                            Budget (optional)
                            <input
                              value={budget}
                              onChange={(e) => setBudget(e.target.value)}
                              placeholder="Example: $80/day or moderate"
                              className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50"
                            />
                          </label>

                          <label className="text-xs text-muted block">
                            Diet (optional)
                            <input
                              value={diet}
                              onChange={(e) => setDiet(e.target.value)}
                              placeholder="Veg, Jain, halal, no preference"
                              className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50"
                            />
                          </label>

                          <label className="text-xs text-muted block sm:col-span-2">
                            Preferences (optional)
                            <input
                              value={preferences}
                              onChange={(e) => setPreferences(e.target.value)}
                              placeholder="Museums, nightlife, kids activities, local markets..."
                              className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50"
                            />
                          </label>

                          <label className="text-xs text-muted block sm:col-span-2">
                            Custom notes (optional)
                            <textarea
                              value={customPreference}
                              onChange={(e) => setCustomPreference(e.target.value)}
                              rows={2}
                              placeholder="Any special requirement"
                              className="mt-1 w-full rounded-lg border border-border bg-transparent px-3 py-2 text-sm outline-none focus:border-trippy-500/50 resize-none"
                            />
                          </label>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  <div className="flex flex-wrap items-center gap-2">
                    <Button
                      onClick={() => { void handleGenerate(); setShowFormExpanded(false); }}
                      disabled={isLoading || !startDate || !endDate}
                      title={!startDate || !endDate ? "Please select travel dates first" : undefined}
                    >
                      {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />}
                      {isLoading ? "Generating" : "Generate AI Trip"}
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => {
                        setCity("");
                        setStartDate("");
                        setEndDate("");
                        setPeople(2);
                        setBudget("");
                        setDiet("");
                        setPreferences("");
                        setCustomPreference("");
                        setSelectedFilters([]);
                        setResults([]);
                        setAlsoExplore([]);
                        setReply("");
                        setError("");
                        setShowFormExpanded(false);
                      }}
                    >
                      Reset
                    </Button>
                  </div>
                </div>
                )}

                {error && <p className="text-sm text-danger">{error}</p>}
                {reply && <p className="text-sm text-muted">{reply}</p>}

                {results.length > 0 && (
                  <div className="space-y-3">
                    {results.map((trip) => (
                      <TripResultCard
                        key={trip.title}
                        trip={trip}
                        saved={savedTrips.has(trip.title)}
                        onSave={() => handleSave(trip.title)}
                        onOpenFullScreen={() => setFullScreenTrip(trip)}
                        userPrompt={promptPreview}
                        userDates={{ start: startDate, end: endDate }}
                      />
                    ))}

                    {alsoExplore.length > 0 && (
                      <div className="flex flex-wrap items-center gap-2 pt-1">
                        <span className="text-xs text-muted">Also explore:</span>
                        {alsoExplore.map((s, i) => (
                          <button
                            key={`${s.city}-${i}`}
                            onClick={() => setCity(s.city || "")}
                            className="text-xs bg-surface border border-border px-3 py-1 rounded-full text-foreground hover:border-trippy-500/40 hover:bg-trippy-500/5 transition-colors cursor-pointer"
                          >
                            {s.city}{s.country ? `, ${s.country}` : ""}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </GlassCard>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function TripResultCard({
  trip,
  saved,
  onSave,
  onOpenFullScreen,
  userPrompt,
  userDates,
}: {
  trip: GeneratedTrip;
  saved: boolean;
  onSave: () => void;
  onOpenFullScreen: () => void;
  userPrompt?: string;
  userDates?: { start: string; end: string };
}) {
  const [showItinerary, setShowItinerary] = useState(false);
  const [itineraryLoading, setItineraryLoading] = useState(false);
  const [itineraryError, setItineraryError] = useState("");
  const [draftTrip, setDraftTrip] = useState<GeneratedTrip>(trip);
  const [showChat, setShowChat] = useState(false);
  const [chatMessages, setChatMessages] = useState<{ role: "user" | "assistant"; content: string; changes?: { type: string; dayNumber?: number; removed?: string; added?: string; summary: string }[]; hasModification?: boolean }[]>([]);
  const [chatInput, setChatInput] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [prevItinerary, setPrevItinerary] = useState<AiItineraryDay[] | null>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setDraftTrip(trip);
  }, [trip]);

  // Fetch real city image from Wikipedia API
  useEffect(() => {
    if (!trip.destination) return;
    let cancelled = false;
    const query = encodeURIComponent(trip.destination);
    fetch(`/api/images/city?q=${query}`)
      .then((res) => res.json())
      .then((data) => {
        if (!cancelled && data?.image) {
          setDraftTrip((prev) => ({ ...prev, image: data.image }));
        }
      })
      .catch(() => { /* keep placeholder */ });
    return () => { cancelled = true; };
  }, [trip.destination]);

  // Auto-generate itinerary on mount
  const autoGenerated = useRef(false);
  useEffect(() => {
    if (!autoGenerated.current && !draftTrip.aiItinerary?.length) {
      autoGenerated.current = true;
      void handleGenerateItinerary();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleGenerateItinerary() {
    if (draftTrip.aiItinerary?.length) {
      setShowItinerary((p) => !p);
      return;
    }
    setItineraryLoading(true);
    setItineraryError("");
    setShowItinerary(true);
    try {
      const durationMatch = draftTrip.duration.match(/(\d+)/);
      const days = durationMatch ? parseInt(durationMatch[1]) : 3;
      const today = new Date();
      const startDate = today.toISOString().slice(0, 10);
      const endDateObj = new Date(today);
      endDateObj.setDate(endDateObj.getDate() + days - 1);
      const endDate = endDateObj.toISOString().slice(0, 10);

      const res = await fetch("/api/ai/itineraries", {
        method: "POST",
        headers: aiRequestHeaders(),
        body: JSON.stringify({
          constraints: {
            destination: draftTrip.destination,
            startDate: userDates?.start || startDate,
            endDate: userDates?.end || endDate,
            budgetLevel: "MODERATE",
            adults: parseInt(draftTrip.groupSize) || 2,
            children: 0,
          },
          userPrompt: userPrompt
            ? `${userPrompt}. Highlights to include: ${draftTrip.highlights.join(", ")}`
            : `Highlights to include: ${draftTrip.highlights.join(", ")}`,
          interests: draftTrip.highlights,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data?.error || "Failed to generate itinerary");
      }
      if (data.dailyPlan?.length) {
        setDraftTrip((prev) => ({ ...prev, aiItinerary: data.dailyPlan }));
      } else {
        setItineraryError(data?.error || "AI could not generate an itinerary. Please try again.");
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Unknown error";
      setItineraryError(`Could not generate itinerary: ${msg}`);
    } finally {
      setItineraryLoading(false);
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass rounded-xl overflow-hidden"
    >
      <div className="relative h-36 overflow-hidden">
        <img src={draftTrip.image} alt={draftTrip.destination} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&auto=format&fit=crop&q=80'; }} />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        <div className="absolute bottom-3 left-3 right-3">
          <h4 className="text-white font-bold text-sm">{draftTrip.title}</h4>
          <div className="flex items-center gap-1 mt-0.5">
            <MapPin size={11} className="text-white/80" />
            <span className="text-white/80 text-xs">{draftTrip.destination}</span>
          </div>
        </div>
        <div className="absolute top-3 right-3 flex items-center gap-1 bg-black/40 backdrop-blur-sm rounded-full px-2 py-0.5">
          <Star size={10} className="text-amber-400" fill="currentColor" />
          <span className="text-white text-xs font-medium">{draftTrip.rating}</span>
        </div>
      </div>

      <div className="p-3 space-y-3">
        <div className="flex flex-wrap gap-2">
          <span className="flex items-center gap-1 text-xs bg-trippy-500/10 text-trippy-600 px-2 py-1 rounded-full">
            <Calendar size={10} /> {draftTrip.duration}
          </span>
          <span className="flex items-center gap-1 text-xs bg-accent-400/10 text-accent-600 px-2 py-1 rounded-full">
            <DollarSign size={10} /> {draftTrip.budget}
          </span>
          <span className="flex items-center gap-1 text-xs bg-trippy-500/10 text-trippy-600 px-2 py-1 rounded-full">
            <Users size={10} /> {draftTrip.groupSize} people
          </span>
          {draftTrip.bestTimeToVisit && (
            <span className="flex items-center gap-1 text-xs bg-accent-400/10 text-accent-600 px-2 py-1 rounded-full">
              <Calendar size={10} /> Best: {draftTrip.bestTimeToVisit}
            </span>
          )}
        </div>

        {/* AI Reason */}
        {draftTrip.reason && (
          <p className="text-xs text-muted leading-relaxed italic">
            &ldquo;{draftTrip.reason}&rdquo;
          </p>
        )}

        {/* Highlights */}
        <div className="flex flex-wrap gap-1.5">
          {draftTrip.highlights.map((h) => (
            <span key={h} className="text-[10px] bg-surface px-2 py-0.5 rounded-full text-muted">
              {h}
            </span>
          ))}
        </div>

        {/* Itinerary toggle */}
        <button
          onClick={() => void handleGenerateItinerary()}
          disabled={itineraryLoading}
          className="text-xs text-trippy-600 font-medium hover:underline cursor-pointer flex items-center gap-1"
        >
          {itineraryLoading ? (
            <><Loader2 size={10} className="animate-spin" /> Generating AI itinerary…</>
          ) : draftTrip.aiItinerary?.length ? (
            <>{showItinerary ? "Hide itinerary" : "Show AI itinerary"} →</>
          ) : (
            <><Sparkles size={10} /> Generate AI itinerary →</>
          )}
        </button>

        {itineraryError && <p className="text-[10px] text-danger">{itineraryError}</p>}

        <AnimatePresence>
          {showItinerary && draftTrip.aiItinerary && draftTrip.aiItinerary.length > 0 && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="space-y-3 pt-2">
                {draftTrip.aiItinerary.map((day) => {
                  const dateStr = day.date
                    ? new Date(day.date).toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" })
                    : "";
                  const catColors: Record<string, string> = {
                    FOOD: "bg-orange-100 text-orange-700",
                    SIGHTSEEING: "bg-blue-100 text-blue-700",
                    TRANSPORT: "bg-slate-100 text-slate-600",
                    SHOPPING: "bg-pink-100 text-pink-700",
                    CULTURE: "bg-purple-100 text-purple-700",
                    NIGHTLIFE: "bg-indigo-100 text-indigo-700",
                    NATURE: "bg-green-100 text-green-700",
                    WELLNESS: "bg-teal-100 text-teal-700",
                  };
                  return (
                    <div key={day.dayNumber} className="rounded-lg border border-border/60 overflow-hidden bg-white/50">
                      <div className="flex items-center gap-3 px-3 py-2.5 bg-gradient-to-r from-trippy-500/10 to-transparent border-b border-border/40">
                        <div className="w-8 h-8 rounded-lg bg-trippy-500 text-white text-xs flex items-center justify-center font-bold shadow-sm">
                          D{day.dayNumber}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-bold text-foreground truncate">{day.title}</p>
                          {dateStr && (
                            <p className="text-[10px] text-muted">{dateStr}</p>
                          )}
                        </div>
                        <span className="text-[9px] text-muted bg-surface px-2 py-0.5 rounded-full shrink-0">
                          {day.activities.length} activities
                        </span>
                      </div>
                      <div className="divide-y divide-border/30">
                        {day.activities.map((act, idx) => (
                          <div key={idx} className="flex gap-2.5 px-3 py-2 hover:bg-trippy-500/[0.02] transition-colors">
                            <div className="shrink-0 pt-0.5">
                              {act.time ? (
                                <div className="w-12 text-center">
                                  <span className="text-[10px] font-bold text-trippy-600 bg-trippy-500/10 px-1.5 py-0.5 rounded">
                                    {act.time}
                                  </span>
                                </div>
                              ) : (
                                <div className="w-12 flex justify-center">
                                  <div className="w-1.5 h-1.5 rounded-full bg-trippy-300 mt-1.5" />
                                </div>
                              )}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-1.5">
                                <p className="text-[11px] font-semibold text-foreground leading-tight">{act.title}</p>
                                {(() => { const cat = String((act as Record<string, unknown>).category || ""); return cat ? (
                                  <span className={`text-[8px] font-medium px-1.5 py-0.5 rounded-full ${catColors[cat] || "bg-gray-100 text-gray-600"}`}>
                                    {cat.toLowerCase()}
                                  </span>
                                ) : null; })()}
                              </div>
                              {act.description && (
                                <p className="text-[10px] text-muted mt-0.5 leading-relaxed">{act.description}</p>
                              )}
                              <div className="flex flex-wrap items-center gap-1.5 mt-1">
                                {act.location && (
                                  <span className="text-[9px] text-muted flex items-center gap-0.5">
                                    <MapPin size={7} className="text-trippy-400" />
                                    {act.location}
                                  </span>
                                )}
                                {act.estimatedCost && (
                                  <span className="text-[9px] font-medium text-accent-600 bg-accent-400/10 px-1.5 py-0.5 rounded">
                                    {act.estimatedCost}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Chat with Trippy AI */}
        {draftTrip.aiItinerary && draftTrip.aiItinerary.length > 0 && (
          <div className="pt-1">
            <button
              onClick={() => setShowChat((p) => !p)}
              className="text-xs font-medium flex items-center gap-1.5 text-trippy-600 hover:underline cursor-pointer"
            >
              <MessageSquare size={11} />
              {showChat ? "Hide chat" : "✨ Refine trip with Trippy AI"}
            </button>

            <AnimatePresence>
              {showChat && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="overflow-hidden"
                >
                  <div className="mt-2 rounded-xl border border-trippy-500/20 bg-gradient-to-b from-white/80 to-trippy-500/[0.03] overflow-hidden shadow-sm">
                    {/* Chat header */}
                    <div className="flex items-center gap-2 px-3 py-2 border-b border-border/40 bg-gradient-to-r from-trippy-500/10 to-transparent">
                      <div className="w-5 h-5 rounded-full bg-trippy-500 flex items-center justify-center">
                        <Sparkles size={10} className="text-white" />
                      </div>
                      <span className="text-[10px] font-semibold text-foreground">Trippy AI — Trip Modifier</span>
                      {prevItinerary && (
                        <button
                          onClick={() => { setDraftTrip(prev => ({ ...prev, aiItinerary: prevItinerary })); setPrevItinerary(null); }}
                          className="ml-auto flex items-center gap-1 text-[9px] text-orange-600 hover:text-orange-700 cursor-pointer bg-orange-100 px-2 py-0.5 rounded-full"
                        >
                          <Undo2 size={8} /> Undo last change
                        </button>
                      )}
                    </div>

                    {/* Quick action chips */}
                    {chatMessages.length === 0 && (
                      <div className="px-3 pt-2.5 pb-1">
                        <p className="text-[9px] text-muted mb-1.5 uppercase tracking-wider font-semibold">Quick modifications</p>
                        <div className="flex flex-wrap gap-1.5">
                          {["Make it vegetarian 🥗", "More budget-friendly 💰", "Add nightlife 🌙", "Make it relaxed 🧘", "More adventure 🏔️"].map(chip => (
                            <button key={chip} onClick={() => { setChatInput(chip); }} className="text-[10px] px-2.5 py-1 rounded-full border border-trippy-500/20 text-trippy-700 bg-trippy-500/5 hover:bg-trippy-500/15 hover:border-trippy-500/40 transition-all cursor-pointer">
                              {chip}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Messages */}
                    <div className="max-h-64 overflow-y-auto px-3 py-2 space-y-2.5">
                      {chatMessages.map((msg, i) => (
                        <motion.div key={i} initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
                          {msg.role === "user" ? (
                            <div className="max-w-[80%] px-3 py-2 rounded-2xl rounded-br-sm bg-trippy-500 text-white text-[11px] leading-relaxed shadow-sm">
                              {msg.content}
                            </div>
                          ) : (
                            <div className="max-w-[90%] space-y-1.5">
                              {/* Change cards */}
                              {msg.changes && msg.changes.length > 0 && (
                                <div className="space-y-1">
                                  {msg.changes.map((c, ci) => (
                                    <div key={ci} className={`flex items-start gap-2 px-2.5 py-2 rounded-lg border text-[10px] ${
                                      c.type === "swap" ? "border-blue-200 bg-blue-50/80" :
                                      c.type === "add" ? "border-green-200 bg-green-50/80" :
                                      c.type === "remove" ? "border-red-200 bg-red-50/80" :
                                      "border-amber-200 bg-amber-50/80"
                                    }`}>
                                      <div className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 mt-0.5 ${
                                        c.type === "swap" ? "bg-blue-500" :
                                        c.type === "add" ? "bg-green-500" :
                                        c.type === "remove" ? "bg-red-500" :
                                        "bg-amber-500"
                                      }`}>
                                        {c.type === "swap" ? <ArrowRightLeft size={9} className="text-white" /> :
                                         c.type === "add" ? <Plus size={9} className="text-white" /> :
                                         c.type === "remove" ? <Trash2 size={9} className="text-white" /> :
                                         <Lightbulb size={9} className="text-white" />}
                                      </div>
                                      <div className="flex-1 min-w-0">
                                        {c.dayNumber && <span className="font-bold text-foreground">Day {c.dayNumber} • </span>}
                                        <span className="text-foreground/80">{c.summary}</span>
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              )}
                              {/* Text portion (filter out change summaries if changes exist) */}
                              {(!msg.changes || msg.changes.length === 0) && (
                                <div className="px-3 py-2 rounded-2xl rounded-bl-sm bg-surface/80 border border-border/40 text-[11px] text-foreground leading-relaxed whitespace-pre-wrap">
                                  {msg.content.split("\n").map((line, li) => {
                                    if (line.startsWith("**") || line.includes("**")) {
                                      return <p key={li} className="font-semibold mt-1">{line.replace(/\*\*/g, "")}</p>;
                                    }
                                    if (line.startsWith("•") || line.startsWith("-")) {
                                      return <p key={li} className="pl-2 text-muted">{line}</p>;
                                    }
                                    if (line.startsWith("*") && line.endsWith("*")) {
                                      return <p key={li} className="italic text-[10px] text-trippy-600 mt-1">{line.replace(/\*/g, "")}</p>;
                                    }
                                    return line ? <p key={li}>{line}</p> : <br key={li} />;
                                  })}
                                </div>
                              )}
                              {msg.hasModification && (
                                <div className="flex items-center gap-1 text-[9px] text-green-600">
                                  <Check size={8} /> Itinerary updated
                                </div>
                              )}
                            </div>
                          )}
                        </motion.div>
                      ))}
                      {chatLoading && (
                        <div className="flex justify-start">
                          <div className="bg-surface/80 border border-border/40 px-4 py-2.5 rounded-2xl rounded-bl-sm flex items-center gap-2">
                            <Loader2 size={12} className="animate-spin text-trippy-500" />
                            <span className="text-[10px] text-muted">Modifying your trip…</span>
                          </div>
                        </div>
                      )}
                      <div ref={chatEndRef} />
                    </div>

                    {/* Input form */}
                    <form
                      onSubmit={async (e) => {
                        e.preventDefault();
                        if (!chatInput.trim() || chatLoading) return;
                        const newMsg = { role: "user" as const, content: chatInput.trim() };
                        const updated = [...chatMessages, newMsg];
                        setChatMessages(updated);
                        setChatInput("");
                        setChatLoading(true);
                        try {
                          const tripCtx = `Trip: ${draftTrip.title}\nDestination: ${draftTrip.destination}\nDuration: ${draftTrip.duration}`;
                          const res = await fetch("/api/ai/chat", {
                            method: "POST",
                            headers: aiRequestHeaders(),
                            body: JSON.stringify({
                              messages: updated.map(m => ({ role: m.role, content: m.content })),
                              tripContext: tripCtx,
                              currentItinerary: draftTrip.aiItinerary,
                              destination: draftTrip.destination,
                            }),
                          });
                          const data = await res.json();
                          const assistantMsg = {
                            role: "assistant" as const,
                            content: data.reply || data.error || "No response",
                            changes: data.changes,
                            hasModification: data.hasModification,
                          };
                          setChatMessages([...updated, assistantMsg]);
                          if (data.hasModification && data.updatedItinerary) {
                            setPrevItinerary(draftTrip.aiItinerary || null);
                            setDraftTrip(prev => ({ ...prev, aiItinerary: data.updatedItinerary }));
                            setShowItinerary(true);
                          }
                        } catch {
                          setChatMessages([...updated, { role: "assistant", content: "Sorry, something went wrong. Try again." }]);
                        } finally {
                          setChatLoading(false);
                          setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: "smooth" }), 100);
                        }
                      }}
                      className="flex items-center gap-2 px-3 py-2.5 border-t border-border/40 bg-white/50"
                    >
                      <input
                        value={chatInput}
                        onChange={(e) => setChatInput(e.target.value)}
                        placeholder="e.g. Make Day 1 vegetarian, add nightlife..."
                        className="flex-1 text-xs bg-transparent outline-none placeholder:text-muted/50"
                      />
                      <button type="submit" disabled={chatLoading || !chatInput.trim()} className="w-7 h-7 rounded-full bg-trippy-500 flex items-center justify-center text-white disabled:opacity-30 cursor-pointer hover:bg-trippy-600 transition-colors">
                        <Send size={12} />
                      </button>
                    </form>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        <div className="flex gap-2 pt-1">
          <Button size="sm" className="flex-1 text-xs" onClick={onOpenFullScreen}>
            <Sparkles size={12} /> Open Trip
          </Button>
          <Button size="sm" variant="secondary" className="text-xs" onClick={onSave} disabled={saved}>
            {saved ? <><Check size={12} /> Saved</> : <><Check size={12} /> Save</>}
          </Button>
        </div>
      </div>
    </motion.div>
  );
}
