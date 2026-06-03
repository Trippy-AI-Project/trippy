"use client";

import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import { AnimatePresence, motion } from "framer-motion";
import {
  ArrowRight,
  Calendar,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  Compass,
  DollarSign,
  Map,
  MapPin,
  MessageCircle,
  Route,
  Search,
  SlidersHorizontal,
  Sparkles,
  Users,
  Utensils,
  Wand2,
} from "lucide-react";
import AITripBuilderModal, { type AIBuilderRequest } from "@/components/ai/AITripBuilderModal";
import { Button } from "@/components/ui";
import { cn } from "@/lib/utils";

const HERO_IDEAS = [
  "a 7-day food trip through Kyoto",
  "a quiet coastal week in Portugal",
  "a family adventure in Costa Rica",
  "a luxury long weekend in Paris",
  "a remote-work month in Barcelona",
];

const TRIP_TYPE_FILTERS = ["Beach", "Adventure", "City", "Nature", "Culture", "Wellness"];

const NO_PREFERENCE_LABEL = "No preference";

const BUDGET_OPTIONS = [NO_PREFERENCE_LABEL, "Budget", "Moderate", "Premium", "Luxury"];

const TRAVEL_GROUPS = [
  { label: "Solo", people: 1 },
  { label: "Couple", people: 2 },
  { label: "Friends", people: 4 },
  { label: "Family", people: 4 },
  { label: "Group", people: 8 },
];

const DIET_OPTIONS = [NO_PREFERENCE_LABEL, "Vegetarian", "Vegan", "Halal", "Jain"];

const PACE_OPTIONS = [NO_PREFERENCE_LABEL, "Balanced pace", "Relaxed", "Packed"];

type AdjustmentKey = "balanced" | "slower" | "food";

interface PreviewStop {
  time: string;
  title: string;
  detail: string;
}

interface PreviewAdjustment {
  key: AdjustmentKey;
  label: string;
  prompt: string;
  summary: string;
  filters: string[];
  itinerary: PreviewStop[];
}

const PREVIEW_ADJUSTMENTS: PreviewAdjustment[] = [
  {
    key: "balanced",
    label: "Balanced",
    prompt: "Keep it premium, but leave room to wander.",
    summary: "A polished Lisbon plan with timed anchors, soft gaps, and one guided evening.",
    filters: ["City", "Food", "Culture"],
    itinerary: [
      {
        time: "09:30",
        title: "Tile atelier in Principe Real",
        detail: "Private studio visit, then coffee within walking distance.",
      },
      {
        time: "13:00",
        title: "Chef-picked lunch in Chiado",
        detail: "Trippy holds two nearby backups if the first choice is busy.",
      },
      {
        time: "17:20",
        title: "Sunset tram route to Alfama",
        detail: "Low-friction route with a short scenic walk and viewpoint stop.",
      },
    ],
  },
  {
    key: "slower",
    label: "Make it slower",
    prompt: "Lower the pace and protect open time.",
    summary: "The day becomes calmer with fewer hops, later starts, and longer neighborhood blocks.",
    filters: ["Wellness", "Culture"],
    itinerary: [
      {
        time: "10:45",
        title: "Late start near Jardim da Estrela",
        detail: "A slow cafe block before the first planned stop.",
      },
      {
        time: "14:00",
        title: "One museum, not three",
        detail: "Trippy trims the route and keeps the strongest cultural anchor.",
      },
      {
        time: "18:30",
        title: "Dinner within 12 minutes",
        detail: "No cross-town transfer after sunset, just a reserved local table.",
      },
    ],
  },
  {
    key: "food",
    label: "More food",
    prompt: "Turn the day into a local food crawl.",
    summary: "More tasting stops, market timing, and reservations replace generic sightseeing.",
    filters: ["Food", "City"],
    itinerary: [
      {
        time: "09:00",
        title: "Market breakfast at Ribeira",
        detail: "Arrive before peak crowds and save room for the next stop.",
      },
      {
        time: "12:30",
        title: "Petiscos crawl in Bairro Alto",
        detail: "Three small plates, all clustered on a walkable route.",
      },
      {
        time: "20:00",
        title: "Fado dinner pairing",
        detail: "A quieter room with a reservation window and backup venue.",
      },
    ],
  },
];

const DEFAULT_PEOPLE = 2;
const DEFAULT_BUDGET = "";
const PREVIEW_BUDGET = "Moderate";
const PREVIEW_CITY = "Lisbon, Portugal";
const PREVIEW_START_DATE = "2026-09-12";
const PREVIEW_END_DATE = "2026-09-16";

const revealContainer = {
  hidden: {},
  visible: {
    transition: { staggerChildren: 0.08 },
  },
};

const revealItem = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.58, ease: "easeOut" as const },
  },
};

function useTypingEffect(words: string[], typingSpeed = 62, pause = 1500) {
  const [text, setText] = useState("");
  const [wordIndex, setWordIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (words.length === 0) return;

    const current = words[wordIndex] ?? "";
    const delay = !isDeleting && text === current ? pause : isDeleting ? typingSpeed / 2 : typingSpeed;
    const timeout = setTimeout(() => {
      if (!isDeleting && text === current) {
        setIsDeleting(true);
        return;
      }

      if (isDeleting && text === "") {
        setIsDeleting(false);
        setWordIndex((prev) => (prev + 1) % words.length);
        return;
      }

      setText(isDeleting ? current.slice(0, text.length - 1) : current.slice(0, text.length + 1));
    }, delay);

    return () => clearTimeout(timeout);
  }, [text, wordIndex, isDeleting, words, typingSpeed, pause]);

  return text;
}

export default function LandingPage() {
  const requestIdRef = useRef(0);
  const typedIdea = useTypingEffect(HERO_IDEAS);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilters, setActiveFilters] = useState<string[]>([]);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [travelGroup, setTravelGroup] = useState("");
  const [heroBudget, setHeroBudget] = useState(DEFAULT_BUDGET);
  const [dietPreference, setDietPreference] = useState("");
  const [pacePreference, setPacePreference] = useState("");
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const [showAIBuilder, setShowAIBuilder] = useState(false);
  const [aiBuilderRequest, setAiBuilderRequest] = useState<AIBuilderRequest | undefined>(undefined);
  const [activeAdjustment, setActiveAdjustment] = useState<AdjustmentKey>("balanced");

  const selectedGroup = TRAVEL_GROUPS.find((group) => group.label === travelGroup);
  const preview = useMemo(
    () => PREVIEW_ADJUSTMENTS.find((item) => item.key === activeAdjustment) ?? PREVIEW_ADJUSTMENTS[0],
    [activeAdjustment],
  );

  const nextRequestId = () => {
    requestIdRef.current += 1;
    return requestIdRef.current;
  };

  const openAIBuilder = ({
    city = searchQuery.trim(),
    start = startDate,
    end = endDate,
    people = selectedGroup?.people ?? DEFAULT_PEOPLE,
    budget = heroBudget,
    filters = activeFilters,
    diet = dietPreference,
    preferences = pacePreference,
    autoGenerate = false,
  }: {
    city?: string;
    start?: string;
    end?: string;
    people?: number;
    budget?: string;
    filters?: string[];
    diet?: string;
    preferences?: string;
    autoGenerate?: boolean;
  } = {}) => {
    setAiBuilderRequest({
      requestId: nextRequestId(),
      city: city.trim() || undefined,
      startDate: start || undefined,
      endDate: end || undefined,
      people,
      budget: budget || undefined,
      filters,
      diet: diet || undefined,
      preferences: preferences || undefined,
      autoGenerate,
    });
    setShowAIBuilder(true);
  };

  const toggleFilter = (filter: string) => {
    setActiveFilters((prev) =>
      prev.includes(filter) ? prev.filter((item) => item !== filter) : [...prev, filter],
    );
  };

  const adjustPreviewWithAI = () => {
    openAIBuilder({
      city: PREVIEW_CITY,
      start: PREVIEW_START_DATE,
      end: PREVIEW_END_DATE,
      people: DEFAULT_PEOPLE,
      budget: PREVIEW_BUDGET,
      filters: preview.filters,
      diet: undefined,
      preferences: "Balanced pace",
      autoGenerate: false,
    });
  };

  return (
    <div className="relative isolate min-h-screen overflow-x-hidden bg-[#f8efe1] text-[#18211f]">
      <LandingAmbientBackground />

      <header className="sticky top-0 z-50 border-b border-white/30 bg-white/18 px-4 shadow-[0_1px_0_rgba(20,47,43,0.04)] backdrop-blur-2xl lg:px-8">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between">
          <Link href="/" aria-label="Trippy home">
            <LandingLogo />
          </Link>

          <div className="flex items-center gap-2">
            <Link href="/login">
              <Button variant="ghost" size="sm" className="px-3 text-[#263936] hover:bg-white/36">
                Log in
              </Button>
            </Link>
            <Button
              size="sm"
              className="!border-[#d5653e] !bg-[#d5653e] !text-white shadow-[0_14px_26px_-18px_rgba(213,101,62,0.95)] hover:!border-[#b95534] hover:!bg-[#b95534]"
              onClick={() => openAIBuilder()}
            >
              Plan with AI
              <Sparkles size={15} />
            </Button>
          </div>
        </div>
      </header>

      <main className="relative z-10">
        <section className="relative isolate overflow-hidden border-b border-[#172522]/10">
          <div className="relative mx-auto flex min-h-[calc(100svh-8rem)] max-w-7xl flex-col justify-center px-4 py-14 lg:px-8 lg:py-16">
            <motion.div
              variants={revealContainer}
              initial="hidden"
              animate="visible"
              className="mx-auto w-full max-w-6xl text-center"
            >
              <motion.div
                variants={revealItem}
                className="mx-auto mb-5 inline-flex items-center gap-2 rounded-lg border border-[#cfd8c8] bg-white/70 px-3 py-2 text-sm font-bold text-[#24443e] shadow-sm backdrop-blur"
              >
                <Wand2 size={15} className="text-[#d5653e]" />
                AI-native trip planning
              </motion.div>

              <motion.h1
                variants={revealItem}
                className="mx-auto max-w-5xl font-display text-balance text-5xl font-black leading-tight text-[#17211f] sm:text-6xl lg:text-7xl"
              >
                Where are you going next?
              </motion.h1>

              <motion.p variants={revealItem} className="mx-auto mt-5 max-w-2xl text-base leading-7 text-[#5f6f69] sm:text-lg">
                Search a destination or trip idea, choose your dates, and let AI build the first plan.
              </motion.p>

              <motion.form
                variants={revealItem}
                onSubmit={(event) => {
                  event.preventDefault();
                  openAIBuilder({ autoGenerate: true });
                }}
                className="mx-auto mt-8 w-full max-w-5xl rounded-[1.35rem] border border-white/80 bg-white/78 p-2.5 shadow-[0_34px_96px_-58px_rgba(20,47,43,0.82)] backdrop-blur-xl"
              >
                <div className="grid gap-2 lg:grid-cols-[minmax(0,1fr)_250px_178px]">
                  <label
                    className={cn(
                      "flex min-h-16 items-center gap-3 rounded-[1.05rem] border bg-[#fbf7ee]/92 px-4 text-left transition-all duration-200",
                      focusedField === "destination"
                        ? "border-[#d5653e] bg-white shadow-[0_0_0_4px_rgba(213,101,62,0.12),0_16px_30px_-24px_rgba(20,47,43,0.55)]"
                        : "border-transparent hover:border-[#cbd7cb] hover:bg-white/90",
                    )}
                  >
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white text-[#d5653e] shadow-[inset_0_0_0_1px_rgba(213,101,62,0.16),0_8px_18px_-15px_rgba(20,47,43,0.7)]">
                      <Search size={17} />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-xs font-black uppercase text-[#6f7a73]">Destination / idea</span>
                      <input
                        value={searchQuery}
                        onFocus={() => setFocusedField("destination")}
                        onBlur={() => setFocusedField(null)}
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder={typedIdea || "Two weeks in Japan with great food"}
                        className="mt-1 w-full bg-transparent text-base font-semibold text-[#17211f] outline-none placeholder:text-[#8c978f]"
                      />
                    </span>
                  </label>

                  <DateRangePicker
                    startDate={startDate}
                    endDate={endDate}
                    onStartDateChange={setStartDate}
                    onEndDateChange={setEndDate}
                  />

                  <Button
                    type="submit"
                    size="lg"
                    className="group min-h-16 whitespace-nowrap !rounded-[1.05rem] !border-[#d5653e] !bg-[#d5653e] px-5 !text-white shadow-[0_22px_42px_-25px_rgba(213,101,62,0.95)] transition-all duration-300 hover:-translate-y-0.5 hover:!border-[#b95534] hover:!bg-[#b95534] hover:shadow-[0_28px_48px_-25px_rgba(213,101,62,0.98)]"
                  >
                    <span>Plan with AI</span>
                    <span className="grid h-7 w-7 place-items-center rounded-full bg-white/14 text-white transition-all duration-300 group-hover:bg-[#142f2b]/18">
                      <ArrowRight size={16} />
                    </span>
                  </Button>
                </div>
              </motion.form>

              <motion.div
                variants={revealItem}
                className="mx-auto mt-3 flex w-fit max-w-full flex-wrap items-center justify-center gap-2 rounded-[1.35rem] border border-white/60 bg-white/28 px-3 py-2 text-left shadow-[0_18px_54px_-42px_rgba(20,47,43,0.86)] backdrop-blur-2xl sm:rounded-full"
              >
                <PlannerMultiGroup
                  icon={<SlidersHorizontal size={12} />}
                  label="Trip"
                  values={activeFilters}
                  options={TRIP_TYPE_FILTERS}
                  onToggle={toggleFilter}
                />
                <PlannerChoiceGroup
                  icon={<DollarSign size={12} />}
                  label="Budget"
                  value={heroBudget}
                  selected={Boolean(heroBudget)}
                  options={BUDGET_OPTIONS}
                  onChange={setHeroBudget}
                />
                <PlannerChoiceGroup
                  icon={<Users size={12} />}
                  label="Travelers"
                  value={travelGroup}
                  selected={Boolean(travelGroup)}
                  options={[NO_PREFERENCE_LABEL, ...TRAVEL_GROUPS.map((group) => group.label)]}
                  onChange={setTravelGroup}
                />
                <PlannerChoiceGroup
                  icon={<Utensils size={12} />}
                  label="Diet"
                  value={dietPreference}
                  selected={Boolean(dietPreference)}
                  options={DIET_OPTIONS}
                  onChange={setDietPreference}
                />
                <PlannerChoiceGroup
                  icon={<Route size={12} />}
                  label="Pace"
                  value={pacePreference}
                  selected={Boolean(pacePreference)}
                  options={PACE_OPTIONS}
                  onChange={setPacePreference}
                />
              </motion.div>

            </motion.div>
          </div>
        </section>

        <section className="px-4 py-14 lg:px-8 lg:py-20">
          <div className="mx-auto max-w-7xl">
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.54, ease: "easeOut" }}
              className="max-w-3xl"
            >
              <p className="text-xs font-black uppercase text-[#b95534]">AI itinerary preview</p>
              <h2 className="mt-3 max-w-2xl text-3xl font-black leading-tight text-[#17211f] sm:text-4xl">
                A living itinerary, map, and timeline in one place.
              </h2>
              <p className="mt-4 max-w-2xl text-base leading-7 text-[#63736c]">
                Trippy keeps the plan concrete enough to use, but flexible enough to revise with one natural-language adjustment.
              </p>
            </motion.div>

            <div className="mt-9 grid gap-5 lg:grid-cols-[minmax(0,0.98fr)_minmax(380px,1.02fr)]">
              <ItineraryPreview preview={preview} />

              <div className="grid gap-5">
                <MapTimelinePreview />
                <AdjustmentPreview
                  activeKey={activeAdjustment}
                  onSelect={setActiveAdjustment}
                  onAdjust={adjustPreviewWithAI}
                  preview={preview}
                />
              </div>
            </div>
          </div>
        </section>
      </main>

      <AITripBuilderModal
        open={showAIBuilder}
        onClose={() => setShowAIBuilder(false)}
        initialRequest={aiBuilderRequest}
      />
    </div>
  );
}

function LandingLogo({ compact = false }: { compact?: boolean }) {
  return (
    <div className="group flex items-center gap-2.5">
      <div
        className={cn(
          "relative grid place-items-center overflow-hidden rounded-lg bg-[#142f2b] text-white shadow-[0_16px_36px_-24px_rgba(20,47,43,0.8)] transition-transform duration-200 group-hover:-translate-y-0.5",
          compact ? "h-8 w-8" : "h-10 w-10",
        )}
      >
        <Route size={compact ? 17 : 21} strokeWidth={2.2} />
        <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-[#d5653e]" />
      </div>
      <span className={cn("font-display font-black text-[#17211f]", compact ? "text-lg" : "text-xl")}>Trippy</span>
    </div>
  );
}

function PlannerMultiGroup({
  icon,
  label,
  values,
  options,
  onToggle,
}: {
  icon: ReactNode;
  label: string;
  values: string[];
  options: string[];
  onToggle: (value: string) => void;
}) {
  const dropdownRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const summary =
    values.length === 0
      ? label
      : values.length <= 2
        ? values.join(", ")
        : `${values.slice(0, 2).join(", ")} +${values.length - 2}`;
  const hasSelection = values.length > 0;

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: PointerEvent) => {
      if (!dropdownRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const clearSelections = () => {
    values.forEach(onToggle);
  };

  const selectNoPreference = () => {
    clearSelections();
    setOpen(false);
  };

  const selectOption = (option: string) => {
    onToggle(option);
    setOpen(false);
  };

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-haspopup="listbox"
        onClick={() => setOpen((current) => !current)}
        className={cn(
          "group inline-flex h-9 max-w-full items-center gap-2 rounded-full border px-2.5 pr-2 text-left text-xs font-black shadow-[0_12px_28px_-22px_rgba(20,47,43,0.9)] transition-all duration-200",
          open
            ? "border-[#d5653e] bg-white text-[#17211f] shadow-[0_0_0_3px_rgba(213,101,62,0.1),0_16px_28px_-22px_rgba(20,47,43,0.9)]"
            : hasSelection
              ? "border-[#142f2b] bg-[#142f2b] text-white hover:-translate-y-0.5 hover:bg-[#203f39]"
              : "border-[#d8e0d3] bg-[linear-gradient(180deg,#fffdf8_0%,#f5efe4_100%)] text-[#53635d] hover:-translate-y-0.5 hover:border-[#c5d0c3] hover:bg-white",
        )}
      >
        <span
          className={cn(
            "grid h-6 w-6 shrink-0 place-items-center rounded-full shadow-[inset_0_0_0_1px_rgba(213,101,62,0.14)] transition-colors",
            hasSelection ? "bg-white/12 text-[#f0b091]" : "bg-white text-[#d5653e]",
          )}
        >
          {icon}
        </span>
        <span
          className={cn(
            "min-w-0 max-w-[10.5rem] truncate",
            hasSelection ? "text-white" : "text-[#6f7a73]",
          )}
        >
          {summary}
        </span>
        <ChevronDown
          size={13}
          className={cn("shrink-0 transition-transform", hasSelection ? "text-white/62" : "text-[#7b8881]", open && "rotate-180")}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="listbox"
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
            className="absolute left-0 top-full z-50 mt-2 w-52 max-w-[calc(100vw-2rem)] overflow-hidden rounded-xl border border-white/80 bg-white/95 p-1.5 shadow-[0_28px_72px_-42px_rgba(20,47,43,0.9)] backdrop-blur-xl"
          >
            <div className="grid gap-1">
              <button
                type="button"
                role="option"
                aria-selected={!hasSelection}
                onClick={selectNoPreference}
                className={cn(
                  "flex items-center justify-between gap-3 rounded-lg px-2.5 py-1.5 text-left text-xs font-bold transition-colors",
                  !hasSelection ? "bg-[#142f2b] text-white" : "text-[#53635d] hover:bg-[#fbf7ee] hover:text-[#b95534]",
                )}
              >
                <span>{NO_PREFERENCE_LABEL}</span>
                {!hasSelection && <Check size={13} className="text-[#f0b091]" />}
              </button>
              {options.map((option) => {
                const active = values.includes(option);
                return (
                  <button
                    key={option}
                    type="button"
                    role="option"
                    aria-selected={active}
                    onClick={() => selectOption(option)}
                    className={cn(
                      "flex items-center justify-between gap-3 rounded-lg px-2.5 py-1.5 text-left text-xs font-bold transition-colors",
                      active ? "bg-[#142f2b] text-white" : "text-[#53635d] hover:bg-[#fbf7ee] hover:text-[#b95534]",
                    )}
                  >
                    <span>{option}</span>
                    {active && <Check size={13} className="text-[#f0b091]" />}
                  </button>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function PlannerChoiceGroup({
  icon,
  label,
  value,
  selected,
  options,
  onChange,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  selected: boolean;
  options: string[];
  onChange: (value: string) => void;
}) {
  const dropdownRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: PointerEvent) => {
      if (!dropdownRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const selectOption = (option: string) => {
    onChange(option === NO_PREFERENCE_LABEL ? "" : option);
    setOpen(false);
  };

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-haspopup="listbox"
        onClick={() => setOpen((current) => !current)}
        className={cn(
          "group inline-flex h-9 max-w-full items-center gap-2 rounded-full border px-2.5 pr-2 text-left text-xs font-black shadow-[0_12px_28px_-22px_rgba(20,47,43,0.9)] transition-all duration-200",
          open
            ? "border-[#d5653e] bg-white text-[#17211f] shadow-[0_0_0_3px_rgba(213,101,62,0.1),0_16px_28px_-22px_rgba(20,47,43,0.9)]"
            : selected
              ? "border-[#142f2b] bg-[#142f2b] text-white hover:-translate-y-0.5 hover:bg-[#203f39]"
              : "border-[#d8e0d3] bg-[linear-gradient(180deg,#fffdf8_0%,#f5efe4_100%)] text-[#53635d] hover:-translate-y-0.5 hover:border-[#c5d0c3] hover:bg-white",
        )}
      >
        <span
          className={cn(
            "grid h-6 w-6 shrink-0 place-items-center rounded-full shadow-[inset_0_0_0_1px_rgba(213,101,62,0.14)] transition-colors",
            selected ? "bg-white/12 text-[#f0b091]" : "bg-white text-[#d5653e]",
          )}
        >
          {icon}
        </span>
        <span
          className={cn(
            "min-w-0 max-w-[10.5rem] truncate",
            selected ? "text-white" : "text-[#6f7a73]",
          )}
        >
          {selected ? value : label}
        </span>
        <ChevronDown
          size={13}
          className={cn("shrink-0 transition-transform", selected ? "text-white/62" : "text-[#7b8881]", open && "rotate-180")}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="listbox"
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
            className="absolute left-0 top-full z-50 mt-2 w-44 max-w-[calc(100vw-2rem)] overflow-hidden rounded-xl border border-white/80 bg-white/95 p-1.5 shadow-[0_28px_72px_-42px_rgba(20,47,43,0.9)] backdrop-blur-xl"
          >
            <div className="grid gap-1">
              {options.map((option) => {
                const active = option === NO_PREFERENCE_LABEL ? !value : option === value;
                return (
                  <button
                    key={option}
                    type="button"
                    role="option"
                    aria-selected={active}
                    onClick={() => selectOption(option)}
                    className={cn(
                      "flex items-center justify-between gap-3 rounded-lg px-2.5 py-1.5 text-left text-xs font-bold transition-colors",
                      active ? "bg-[#142f2b] text-white" : "text-[#53635d] hover:bg-[#fbf7ee] hover:text-[#b95534]",
                    )}
                  >
                    <span>{option}</span>
                    {active && <Check size={13} className="text-[#f0b091]" />}
                  </button>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function LandingAmbientBackground() {
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-0 z-0 overflow-hidden bg-[#f8efe1]">
      <div className="absolute inset-0 bg-[url('/trippy-landing-background.png')] bg-cover bg-center bg-no-repeat" />
      <div className="absolute inset-0 bg-[linear-gradient(rgba(23,33,31,0.048)_1px,transparent_1px),linear-gradient(90deg,rgba(23,33,31,0.042)_1px,transparent_1px)] bg-[size:78px_78px] opacity-50" />
      <div className="absolute inset-0 bg-[linear-gradient(to_bottom,rgba(255,250,242,0.38)_0%,rgba(255,250,242,0.12)_38%,rgba(248,239,225,0.42)_100%)]" />
    </div>
  );
}

function ItineraryPreview({ preview }: { preview: PreviewAdjustment }) {
  return (
    <motion.article
      key={preview.key}
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.34, ease: "easeOut" }}
      className="rounded-lg border border-[#cbd7cb] bg-white p-5 shadow-[0_22px_70px_-52px_rgba(20,47,43,0.75)] sm:p-6"
    >
      <div className="flex flex-col gap-4 border-b border-[#e3e9e1] pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs font-black uppercase text-[#b95534]">Generated itinerary</p>
          <h3 className="mt-2 text-2xl font-black text-[#17211f]">Lisbon in five days</h3>
          <p className="mt-2 max-w-xl text-sm leading-6 text-[#63736c]">{preview.summary}</p>
        </div>
        <div className="grid h-12 w-12 shrink-0 place-items-center rounded-lg bg-[#142f2b] text-white">
          <Compass size={22} />
        </div>
      </div>

      <div className="mt-5 grid gap-3 sm:grid-cols-3">
        {[
          { icon: Calendar, label: "Dates", value: "Sep 12-16" },
          { icon: MapPin, label: "Base", value: "Principe Real" },
          { icon: Sparkles, label: "Style", value: preview.label },
        ].map(({ icon: Icon, label, value }) => (
          <div key={label} className="rounded-lg border border-[#dce4da] bg-[#fbf7ee] p-3">
            <div className="flex items-center gap-2 text-[#b95534]">
              <Icon size={14} />
              <p className="text-[11px] font-black uppercase text-[#7b827d]">{label}</p>
            </div>
            <p className="mt-1 text-sm font-black text-[#17211f]">{value}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {preview.itinerary.map((stop, index) => (
          <div key={`${stop.time}-${stop.title}`} className="grid grid-cols-[auto_1fr] gap-4">
            <div className="flex flex-col items-center">
              <div className="grid h-10 w-10 place-items-center rounded-lg border border-[#cbd7cb] bg-[#eef3ec] text-[#142f2b]">
                <Clock size={17} />
              </div>
              {index < preview.itinerary.length - 1 && <div className="h-full min-h-8 w-px bg-[#cbd7cb]" />}
            </div>
            <div className="pb-4">
              <p className="text-xs font-black uppercase text-[#b95534]">{stop.time}</p>
              <h4 className="mt-1 text-lg font-black text-[#17211f]">{stop.title}</h4>
              <p className="mt-1 text-sm leading-6 text-[#63736c]">{stop.detail}</p>
            </div>
          </div>
        ))}
      </div>
    </motion.article>
  );
}

function MapTimelinePreview() {
  return (
    <div className="grid gap-5 md:grid-cols-[1fr_0.72fr] lg:grid-cols-1 xl:grid-cols-[1fr_0.72fr]">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.48, ease: "easeOut" }}
        className="relative min-h-72 overflow-hidden rounded-lg border border-[#cbd7cb] bg-[#dfe9df]"
      >
        <div className="absolute inset-0 bg-[linear-gradient(rgba(23,33,31,0.08)_1px,transparent_1px),linear-gradient(90deg,rgba(23,33,31,0.07)_1px,transparent_1px)] bg-[size:42px_42px]" />
        <div className="absolute left-4 top-4 rounded-lg border border-white/70 bg-white/78 px-3 py-2 text-xs font-black uppercase text-[#63736c] backdrop-blur">
          Map preview
        </div>
        <motion.svg viewBox="0 0 580 360" className="absolute inset-0 h-full w-full">
          <path d="M66 236 C144 88 268 278 346 112 C392 14 480 70 526 142" fill="none" stroke="#142f2b" strokeWidth="8" strokeLinecap="round" opacity="0.18" />
          <motion.path
            d="M66 236 C144 88 268 278 346 112 C392 14 480 70 526 142"
            fill="none"
            stroke="#d5653e"
            strokeWidth="5"
            strokeLinecap="round"
            initial={{ pathLength: 0 }}
            whileInView={{ pathLength: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 1.25, ease: "easeInOut" }}
          />
          {[
            [66, 236],
            [194, 172],
            [346, 112],
            [526, 142],
          ].map(([cx, cy], index) => (
            <motion.circle
              key={`${cx}-${cy}`}
              cx={cx}
              cy={cy}
              r={index === 2 ? 14 : 11}
              fill={index === 2 ? "#d5653e" : "#ffffff"}
              stroke="#142f2b"
              strokeWidth="4"
              initial={{ scale: 0 }}
              whileInView={{ scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: 0.2 + index * 0.1, duration: 0.25 }}
            />
          ))}
        </motion.svg>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ delay: 0.08, duration: 0.48, ease: "easeOut" }}
        className="rounded-lg border border-[#cbd7cb] bg-[#17211f] p-5 text-white"
      >
        <div className="mb-5 flex items-center justify-between">
          <div>
            <p className="text-xs font-black uppercase text-white/50">Timeline</p>
            <h3 className="mt-1 text-xl font-black">Day 2 flow</h3>
          </div>
          <Map size={21} className="text-[#d5653e]" />
        </div>

        <div className="space-y-4">
          {[
            ["09:30", "Studio visit"],
            ["13:00", "Lunch hold"],
            ["15:30", "Open time"],
            ["17:20", "Scenic route"],
          ].map(([time, label], index) => (
            <div key={time} className="grid grid-cols-[52px_1fr] gap-3">
              <p className="text-xs font-black text-white/48">{time}</p>
              <div>
                <div className="h-2 overflow-hidden rounded-full bg-white/12">
                  <motion.div
                    className="h-full rounded-full bg-[#d5653e]"
                    initial={{ width: 0 }}
                    whileInView={{ width: `${42 + index * 15}%` }}
                    viewport={{ once: true }}
                    transition={{ delay: 0.15 + index * 0.08, duration: 0.48, ease: "easeOut" }}
                  />
                </div>
                <p className="mt-2 text-sm font-bold text-white/88">{label}</p>
              </div>
            </div>
          ))}
        </div>
      </motion.div>
    </div>
  );
}

function AdjustmentPreview({
  activeKey,
  onSelect,
  onAdjust,
  preview,
}: {
  activeKey: AdjustmentKey;
  onSelect: (key: AdjustmentKey) => void;
  onAdjust: () => void;
  preview: PreviewAdjustment;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ delay: 0.12, duration: 0.48, ease: "easeOut" }}
      className="rounded-lg border border-[#cbd7cb] bg-white p-5 shadow-[0_22px_70px_-54px_rgba(20,47,43,0.72)]"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs font-black uppercase text-[#b95534]">Adjust with AI</p>
          <h3 className="mt-2 text-xl font-black text-[#17211f]">Change the plan without starting over.</h3>
        </div>
        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-[#eef3ec] text-[#142f2b]">
          <MessageCircle size={19} />
        </div>
      </div>

      <div className="mt-5 flex flex-wrap gap-2">
        {PREVIEW_ADJUSTMENTS.map((adjustment) => {
          const active = adjustment.key === activeKey;
          return (
            <button
              key={adjustment.key}
              type="button"
              onClick={() => onSelect(adjustment.key)}
              className={cn(
                "rounded-lg border px-3 py-2 text-sm font-bold transition-all duration-200",
                active
                  ? "border-[#142f2b] bg-[#142f2b] text-white"
                  : "border-[#d7dfd5] bg-[#fbf7ee] text-[#63736c] hover:border-[#d5653e] hover:text-[#b95534]",
              )}
            >
              {adjustment.label}
            </button>
          );
        })}
      </div>

      <div className="mt-4 rounded-lg border border-[#dbe3d9] bg-[#fbf7ee] p-4">
        <div className="flex gap-3">
          <Sparkles size={18} className="mt-0.5 shrink-0 text-[#d5653e]" />
          <p className="text-sm font-semibold leading-6 text-[#263936]">{preview.prompt}</p>
        </div>
      </div>

      <Button className="mt-4 w-full rounded-lg bg-[#d5653e] text-white hover:border-[#b95534] hover:bg-[#b95534]" onClick={onAdjust}>
        Adjust with AI
        <ArrowRight size={16} />
      </Button>
    </motion.div>
  );
}

function DateRangePicker({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
}: {
  startDate: string;
  endDate: string;
  onStartDateChange: (value: string) => void;
  onEndDateChange: (value: string) => void;
}) {
  const pickerRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const selected = parseLocalDate(startDate);
    return selected ?? new Date();
  });

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: PointerEvent) => {
      if (!pickerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  const monthStart = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1);
  const days = buildCalendarDays(monthStart);
  const rangeLabel = formatDateRangeLabel(startDate, endDate);

  const selectDate = (date: Date) => {
    const selected = toDateInputValue(date);

    if (!start || end || date < start) {
      onStartDateChange(selected);
      onEndDateChange("");
      return;
    }

    onEndDateChange(selected);
    setOpen(false);
  };

  const clearDates = () => {
    onStartDateChange("");
    onEndDateChange("");
  };

  return (
    <div ref={pickerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        className={cn(
          "flex min-h-16 w-full items-center gap-3 rounded-[1.05rem] border bg-[#fbf7ee]/92 px-4 text-left transition-all duration-200 focus-visible:focus-ring",
          open
            ? "border-[#d5653e] bg-white shadow-[0_0_0_4px_rgba(213,101,62,0.12),0_16px_30px_-24px_rgba(20,47,43,0.55)]"
            : "border-transparent hover:border-[#cbd7cb] hover:bg-white/90",
        )}
      >
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white text-[#d5653e] shadow-[inset_0_0_0_1px_rgba(213,101,62,0.16),0_8px_18px_-15px_rgba(20,47,43,0.7)]">
          <Calendar size={17} />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-xs font-black uppercase text-[#6f7a73]">Dates</span>
          <span className={cn("mt-1 block truncate text-sm font-black", rangeLabel ? "text-[#17211f]" : "text-[#8c978f]")}>
            {rangeLabel || "Add dates"}
          </span>
        </span>
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.98 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
            className="absolute left-0 top-full z-40 mt-2 w-[min(22rem,calc(100vw-2rem))] rounded-lg border border-[#cbd7cb] bg-white p-4 text-left shadow-2xl"
          >
            <div className="mb-4 flex items-center justify-between gap-3">
              <button
                type="button"
                onClick={() => setCalendarMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))}
                className="grid h-9 w-9 place-items-center rounded-lg border border-[#d6ded4] bg-[#fbf7ee] text-[#142f2b] hover:border-[#d5653e]"
                aria-label="Previous month"
              >
                <ChevronLeft size={17} />
              </button>
              <div className="text-center">
                <p className="text-sm font-black text-[#17211f]">
                  {monthStart.toLocaleDateString(undefined, { month: "long", year: "numeric" })}
                </p>
                <p className="mt-0.5 text-xs font-semibold text-[#6f7a73]">
                  {start && !end ? "Select an end date" : "Select a start date"}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setCalendarMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))}
                className="grid h-9 w-9 place-items-center rounded-lg border border-[#d6ded4] bg-[#fbf7ee] text-[#142f2b] hover:border-[#d5653e]"
                aria-label="Next month"
              >
                <ChevronRight size={17} />
              </button>
            </div>

            <div className="grid grid-cols-7 gap-1 text-center">
              {["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"].map((day) => (
                <div key={day} className="py-2 text-[11px] font-black uppercase text-[#7b827d]">
                  {day}
                </div>
              ))}

              {days.map((date, index) => {
                if (!date) return <div key={`empty-${index}`} className="h-10" />;

                const isStart = Boolean(start && sameDay(date, start));
                const isEnd = Boolean(end && sameDay(date, end));
                const isBetween = Boolean(start && end && date > start && date < end);

                return (
                  <button
                    key={toDateInputValue(date)}
                    type="button"
                    onClick={() => selectDate(date)}
                    className={cn(
                      "grid h-10 place-items-center rounded-lg text-sm font-bold transition-all",
                      isStart || isEnd
                        ? "bg-[#142f2b] text-white"
                        : isBetween
                          ? "bg-[#e1ebe0] text-[#142f2b]"
                          : "text-[#17211f] hover:bg-[#fbf1e8] hover:text-[#b95534]",
                    )}
                  >
                    {date.getDate()}
                  </button>
                );
              })}
            </div>

            <div className="mt-4 flex items-center justify-between border-t border-[#e1e8de] pt-4">
              <button
                type="button"
                onClick={clearDates}
                className="text-sm font-bold text-[#6f7a73] transition-colors hover:text-[#b95534]"
              >
                Clear
              </button>
              <Button type="button" size="sm" className="rounded-lg bg-[#142f2b] text-white hover:border-[#142f2b] hover:bg-[#203f39]" onClick={() => setOpen(false)}>
                Done
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function parseLocalDate(value: string) {
  if (!value) return null;
  return new Date(`${value}T00:00:00`);
}

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDateRangeLabel(startDate: string, endDate: string) {
  const start = parseLocalDate(startDate);
  const end = parseLocalDate(endDate);
  const format = (date: Date) => date.toLocaleDateString(undefined, { month: "short", day: "numeric" });

  if (start && end) return `${format(start)} - ${format(end)}`;
  if (start) return `${format(start)} - Add return`;
  return "";
}

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function buildCalendarDays(monthStart: Date) {
  const days: Array<Date | null> = [];
  const year = monthStart.getFullYear();
  const month = monthStart.getMonth();
  const firstDay = monthStart.getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  for (let index = 0; index < firstDay; index += 1) {
    days.push(null);
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    days.push(new Date(year, month, day));
  }

  return days;
}
