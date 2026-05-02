"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import { AnimatePresence, motion } from "framer-motion";
import {
  ArrowRight,
  Calendar,
  Check,
  ChevronDown,
  Compass,
  DollarSign,
  MapPin,
  Search,
  Sparkles,
  Star,
  Users,
} from "lucide-react";
import AITripBuilderModal, { type AIBuilderRequest } from "@/components/ai/AITripBuilderModal";
import Logo from "@/components/Logo";
import { Badge, Button } from "@/components/ui";
import { cn } from "@/lib/utils";

const HERO_DESTINATIONS = [
  "Kyoto, Japan",
  "Lisbon, Portugal",
  "Bali, Indonesia",
  "Barcelona, Spain",
  "Banff, Canada",
  "Cape Town, South Africa",
];

const EXPERIENCE_FILTERS = ["Beach", "City", "Food", "Nature", "Adventure", "Wellness"];

const BUDGET_OPTIONS = ["Budget", "Moderate", "Premium", "Luxury"];

const TRAVEL_GROUPS = [
  { label: "Solo", people: 1 },
  { label: "Couple", people: 2 },
  { label: "Friends", people: 4 },
  { label: "Family", people: 4 },
  { label: "Group", people: 8 },
];

const TRENDING_TRIPS = [
  {
    id: "1",
    title: "Greek Island Hopping",
    destination: "Santorini & Mykonos, Greece",
    image: "https://images.unsplash.com/photo-1613395877344-13d4a8e0d49e?w=900&h=650&fit=crop",
    dates: "Jul 12 - Jul 22",
    price: "$2,340",
    rating: 4.9,
    tags: ["Beach", "Culture"],
    isAI: false,
  },
  {
    id: "2",
    title: "Tokyo Food, Neon & Temples",
    destination: "Tokyo & Kyoto, Japan",
    image: "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=900&h=650&fit=crop",
    dates: "Aug 5 - Aug 18",
    price: "$3,120",
    rating: 4.8,
    tags: ["City", "Food"],
    isAI: true,
  },
  {
    id: "3",
    title: "Bali Wellness Retreat",
    destination: "Ubud & Seminyak, Bali",
    image: "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=900&h=650&fit=crop",
    dates: "Sep 1 - Sep 10",
    price: "$1,890",
    rating: 4.9,
    tags: ["Wellness", "Nature"],
    isAI: false,
  },
  {
    id: "4",
    title: "Patagonia Trek Adventure",
    destination: "Torres del Paine, Chile",
    image: "https://images.unsplash.com/photo-1531761535209-180857e963b9?w=900&h=650&fit=crop",
    dates: "Oct 8 - Oct 20",
    price: "$2,780",
    rating: 4.7,
    tags: ["Adventure", "Nature"],
    isAI: true,
  },
  {
    id: "5",
    title: "Amalfi Coast Road Trip",
    destination: "Naples to Positano, Italy",
    image: "https://images.unsplash.com/photo-1534113414509-0eec2bfb493f?w=900&h=650&fit=crop",
    dates: "Jun 20 - Jun 28",
    price: "$2,100",
    rating: 4.8,
    tags: ["Food", "Road Trip"],
    isAI: false,
  },
  {
    id: "6",
    title: "Northern Lights Chase",
    destination: "Tromso & Lofoten, Norway",
    image: "https://images.unsplash.com/photo-1483347756197-71ef80e95f73?w=900&h=650&fit=crop",
    dates: "Nov 15 - Nov 24",
    price: "$3,450",
    rating: 4.9,
    tags: ["Nature", "Adventure"],
    isAI: true,
  },
];

const WORKFLOW_STEPS = [
  {
    icon: Search,
    title: "Search",
    desc: "Pick a place and dates.",
  },
  {
    icon: Sparkles,
    title: "Generate",
    desc: "Get a first itinerary.",
  },
  {
    icon: Users,
    title: "Plan",
    desc: "Shape it with your people.",
  },
];

const revealContainer = {
  hidden: {},
  visible: {
    transition: { staggerChildren: 0.08 },
  },
};

const revealItem = {
  hidden: { opacity: 0, y: 22 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.56, ease: "easeOut" as const },
  },
};

const cardReveal = {
  hidden: { opacity: 0, y: 26 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.06, duration: 0.48, ease: "easeOut" as const },
  }),
};

function useTypingEffect(words: string[], typingSpeed = 90, pause = 1900) {
  const [text, setText] = useState("");
  const [wordIndex, setWordIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    const current = words[wordIndex];
    const timeout = setTimeout(
      () => {
        if (!isDeleting) {
          setText(current.slice(0, text.length + 1));
          if (text.length + 1 === current.length) {
            setTimeout(() => setIsDeleting(true), pause);
          }
        } else {
          setText(current.slice(0, text.length - 1));
          if (text.length === 0) {
            setIsDeleting(false);
            setWordIndex((prev) => (prev + 1) % words.length);
          }
        }
      },
      isDeleting ? typingSpeed / 2 : typingSpeed,
    );

    return () => clearTimeout(timeout);
  }, [text, wordIndex, isDeleting, words, typingSpeed, pause]);

  return text;
}

export default function LandingPage() {
  const requestIdRef = useRef(0);
  const typedDestination = useTypingEffect(HERO_DESTINATIONS);
  const [activeStep, setActiveStep] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilters, setActiveFilters] = useState<string[]>([]);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [travelGroup, setTravelGroup] = useState("Couple");
  const [heroBudget, setHeroBudget] = useState("Moderate");
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const [showAIBuilder, setShowAIBuilder] = useState(false);
  const [aiBuilderRequest, setAiBuilderRequest] = useState<AIBuilderRequest | undefined>(undefined);

  useEffect(() => {
    const interval = setInterval(() => {
      setActiveStep((prev) => (prev + 1) % WORKFLOW_STEPS.length);
    }, 2800);
    return () => clearInterval(interval);
  }, []);

  const selectedGroup = TRAVEL_GROUPS.find((group) => group.label === travelGroup);

  const nextRequestId = () => {
    requestIdRef.current += 1;
    return requestIdRef.current;
  };

  const openAIBuilder = (autoGenerate = false) => {
    setAiBuilderRequest({
      requestId: nextRequestId(),
      city: searchQuery.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      people: selectedGroup?.people ?? 2,
      budget: heroBudget,
      filters: activeFilters,
      autoGenerate,
    });
    setShowAIBuilder(true);
  };

  const openAIBuilderForTrip = (trip: (typeof TRENDING_TRIPS)[number]) => {
    setAiBuilderRequest({
      requestId: nextRequestId(),
      city: trip.destination,
      people: selectedGroup?.people ?? 2,
      budget: heroBudget,
      filters: trip.tags,
      autoGenerate: false,
    });
    setShowAIBuilder(true);
  };

  const toggleFilter = (tag: string) => {
    setActiveFilters((prev) =>
      prev.includes(tag) ? prev.filter((item) => item !== tag) : [...prev, tag],
    );
  };

  const clearPlanner = () => {
    setSearchQuery("");
    setActiveFilters([]);
    setStartDate("");
    setEndDate("");
    setTravelGroup("Couple");
    setHeroBudget("Moderate");
  };

  const filteredTrips = TRENDING_TRIPS.filter((trip) => {
    const q = searchQuery.trim().toLowerCase();
    const matchesSearch =
      !q ||
      trip.title.toLowerCase().includes(q) ||
      trip.destination.toLowerCase().includes(q) ||
      trip.tags.some((tag) => tag.toLowerCase().includes(q));
    const matchesTags =
      activeFilters.length === 0 ||
      trip.tags.some((tag) => activeFilters.includes(tag));
    return matchesSearch && matchesTags;
  });

  const hasPlannerInput =
    Boolean(searchQuery.trim()) ||
    activeFilters.length > 0 ||
    Boolean(startDate) ||
    Boolean(endDate) ||
    travelGroup !== "Couple" ||
    heroBudget !== "Moderate";

  return (
    <div className="min-h-screen overflow-x-hidden bg-background text-foreground">
      <header className="sticky top-0 z-50 border-b border-border bg-surface px-4 lg:px-8">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between">
          <Logo size="md" />

          <nav className="hidden items-center gap-7 text-sm font-semibold text-muted md:flex">
            <a href="#discover" className="transition-colors hover:text-trippy-500">Discover</a>
            <a href="#planning" className="transition-colors hover:text-trippy-500">How it works</a>
          </nav>

          <div className="flex items-center gap-2">
            <Link href="/login">
              <Button variant="ghost" size="sm">Log in</Button>
            </Link>
            <Link href="/register">
              <Button size="sm">Get Started</Button>
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section className="relative isolate overflow-hidden border-b border-border bg-[#f4f8f3]">
          <AnimatedTravelBackdrop />

          <div className="relative mx-auto flex min-h-[calc(100vh-4rem)] max-w-7xl flex-col justify-center px-4 py-14 lg:px-8 lg:py-20">
            <motion.div
              className="mx-auto w-full max-w-5xl text-center"
              variants={revealContainer}
              initial="hidden"
              animate="visible"
            >
              <motion.div
                variants={revealItem}
                className="mx-auto mb-5 inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-bold text-trippy-500 shadow-sm"
              >
                <Compass size={15} className="text-accent-500" />
                Trippy planner
              </motion.div>

              <motion.h1
                variants={revealItem}
                className="font-display text-balance text-5xl font-black leading-tight text-foreground sm:text-6xl lg:text-7xl"
              >
                Where are you going next?
              </motion.h1>

              <motion.p variants={revealItem} className="mx-auto mt-4 max-w-2xl text-base leading-7 text-muted sm:text-lg">
                Search, choose your dates, and build the first plan.
              </motion.p>

              <motion.form
                variants={revealItem}
                onSubmit={(event) => {
                  event.preventDefault();
                  openAIBuilder(true);
                }}
                className="mx-auto mt-8 w-full rounded-xl border border-border bg-surface p-3 shadow-xl sm:p-4"
              >
                <div className="grid gap-3 lg:grid-cols-[1fr_250px_170px]">
                  <label
                    className={cn(
                      "flex min-h-16 items-center gap-3 rounded-lg border bg-shore-50 px-4 text-left transition-all duration-200",
                      focusedField === "destination" ? "border-trippy-500 shadow-md" : "border-border",
                    )}
                  >
                    <Search size={18} className="text-trippy-500" />
                    <span className="sr-only">Destination</span>
                    <input
                      value={searchQuery}
                      onFocus={() => setFocusedField("destination")}
                      onBlur={() => setFocusedField(null)}
                      onChange={(event) => setSearchQuery(event.target.value)}
                      placeholder={typedDestination || "Where do you want to go?"}
                      className="w-full bg-transparent text-base font-semibold text-foreground outline-none placeholder:text-muted"
                    />
                  </label>

                  <DateRangePicker
                    startDate={startDate}
                    endDate={endDate}
                    onStartDateChange={setStartDate}
                    onEndDateChange={setEndDate}
                  />

                  <Button type="submit" size="lg" className="min-h-16 rounded-lg">
                    Plan Trip <ArrowRight size={18} />
                  </Button>
                </div>

                <div className="mt-3 grid gap-2 lg:grid-cols-[160px_170px_1fr]">
                  <PlannerDropdown
                    icon={<DollarSign size={16} />}
                    label="Budget"
                    value={heroBudget}
                    options={BUDGET_OPTIONS}
                    onChange={setHeroBudget}
                  />

                  <PlannerDropdown
                    icon={<Users size={16} />}
                    label="Travelers"
                    value={travelGroup}
                    options={TRAVEL_GROUPS.map((group) => group.label)}
                    onChange={setTravelGroup}
                  />

                  <div className="flex flex-wrap items-center gap-2 rounded-lg border border-border bg-shore-50 p-2">
                    {EXPERIENCE_FILTERS.map((tag) => {
                      const active = activeFilters.includes(tag);
                      return (
                        <motion.button
                          key={tag}
                          type="button"
                          onClick={() => toggleFilter(tag)}
                          whileTap={{ scale: 0.96 }}
                          className={cn(
                            "rounded-md border px-3 py-2 text-sm font-semibold transition-all duration-200",
                            active
                              ? "border-trippy-500 bg-trippy-500 text-white shadow-sm"
                              : "border-border bg-surface text-muted hover:border-trippy-500 hover:text-trippy-500",
                          )}
                        >
                          {tag}
                        </motion.button>
                      );
                    })}
                    {hasPlannerInput && (
                      <button
                        type="button"
                        onClick={clearPlanner}
                        className="ml-auto rounded-md border border-border bg-surface px-3 py-2 text-sm font-semibold text-muted transition-colors hover:text-danger"
                      >
                        Clear
                      </button>
                    )}
                  </div>
                </div>
              </motion.form>
            </motion.div>
          </div>
        </section>

        <section id="discover" className="mx-auto max-w-7xl px-4 py-14 lg:px-8 lg:py-16">
          <SectionHeading
            eyebrow="Discover"
            title="Popular starts"
            description="Use an example or search your own destination above."
          />

          <AnimatePresence mode="popLayout">
            {filteredTrips.length > 0 ? (
              <motion.div layout className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {filteredTrips.map((trip, index) => (
                  <motion.article
                    key={trip.id}
                    custom={index}
                    variants={cardReveal}
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true, margin: "-80px" }}
                    className="overflow-hidden rounded-lg border border-border bg-surface shadow-sm transition-transform duration-300 hover:-translate-y-1 hover:shadow-lg"
                  >
                    <div className="aspect-[4/3] overflow-hidden bg-shore-200">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={trip.image}
                        alt={trip.title}
                        className="h-full w-full object-cover transition-transform duration-700 hover:scale-105"
                      />
                    </div>
                    <div className="p-4">
                      <div className="mb-3 flex flex-wrap gap-2">
                        {trip.tags.map((tag) => (
                          <Badge key={tag}>{tag}</Badge>
                        ))}
                        {trip.isAI && <Badge variant="accent">AI draft</Badge>}
                      </div>

                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <h3 className="text-lg font-black leading-tight">{trip.title}</h3>
                          <p className="mt-2 flex items-center gap-2 text-sm font-semibold text-muted">
                            <MapPin size={15} className="text-trippy-500" />
                            {trip.destination}
                          </p>
                        </div>
                        <div className="flex items-center gap-1 rounded-md bg-shore-100 px-2.5 py-1 text-sm font-black text-trippy-500">
                          <Star size={14} fill="currentColor" />
                          {trip.rating}
                        </div>
                      </div>

                      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
                        <div className="rounded-lg border border-border bg-shore-50 p-3">
                          <p className="text-xs font-bold uppercase text-muted">Dates</p>
                          <p className="mt-1 font-black">{trip.dates}</p>
                        </div>
                        <div className="rounded-lg border border-border bg-shore-50 p-3">
                          <p className="text-xs font-bold uppercase text-muted">From</p>
                          <p className="mt-1 font-black">{trip.price}</p>
                        </div>
                      </div>

                      <Button className="mt-4 w-full rounded-lg" onClick={() => openAIBuilderForTrip(trip)}>
                        Plan similar trip <ArrowRight size={16} />
                      </Button>
                    </div>
                  </motion.article>
                ))}
              </motion.div>
            ) : (
              <motion.div
                key="no-results"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -16 }}
                className="mt-8 rounded-lg border border-border bg-surface p-8 text-center shadow-sm"
              >
                <Sparkles size={28} className="mx-auto text-accent-500" />
                <h3 className="mt-4 text-xl font-black">No examples match that search</h3>
                <p className="mx-auto mt-2 max-w-md text-muted">
                  Clear the filters or open the builder with your idea.
                </p>
                <div className="mt-5 flex flex-col justify-center gap-3 sm:flex-row">
                  <Button variant="secondary" onClick={clearPlanner}>Clear filters</Button>
                  <Button onClick={() => openAIBuilder(true)}>Open AI builder</Button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </section>

        <section id="planning" className="bg-surface py-12 lg:py-14">
          <div className="mx-auto max-w-7xl px-4 lg:px-8">
            <SectionHeading
              eyebrow="How it works"
              title="From search to itinerary"
              description="A short path from idea to first plan."
            />

            <div className="relative mt-7">
              <motion.svg
                viewBox="0 0 900 170"
                className="mb-2 hidden h-24 w-full md:block"
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true, margin: "-80px" }}
              >
                <motion.path
                  d="M48 112 C190 32 322 132 458 78 C600 22 700 126 852 54"
                  fill="none"
                  stroke="#123C69"
                  strokeWidth="6"
                  strokeLinecap="round"
                  initial={{ pathLength: 0 }}
                  whileInView={{ pathLength: 1 }}
                  viewport={{ once: true }}
                  transition={{ duration: 1.25, ease: "easeInOut" }}
                />
                {[48, 458, 852].map((cx, index) => (
                  <motion.circle
                    key={cx}
                    cx={cx}
                    cy={[112, 78, 54][index]}
                    r="11"
                    fill={index === activeStep ? "#E76F51" : "#FFFFFF"}
                    stroke="#123C69"
                    strokeWidth="5"
                    initial={{ scale: 0 }}
                    whileInView={{ scale: 1 }}
                    viewport={{ once: true }}
                    transition={{ delay: 0.2 + index * 0.12, duration: 0.3 }}
                  />
                ))}
              </motion.svg>

              <div className="grid overflow-hidden rounded-lg border border-border bg-background md:grid-cols-3">
                {WORKFLOW_STEPS.map((step, index) => {
                  const StepIcon = step.icon;
                  const active = activeStep === index;
                  return (
                    <motion.button
                      key={step.title}
                      type="button"
                      onClick={() => setActiveStep(index)}
                      custom={index}
                      variants={cardReveal}
                      initial="hidden"
                      whileInView="visible"
                      viewport={{ once: true }}
                      className={cn(
                        "border-border p-5 text-left transition-all duration-300 md:border-r md:last:border-r-0",
                        active
                          ? "bg-trippy-500 text-white"
                          : "bg-background text-foreground hover:bg-shore-50",
                      )}
                    >
                      <div
                        className={cn(
                          "mb-4 grid h-10 w-10 place-items-center rounded-lg",
                          active ? "bg-surface text-trippy-500" : "bg-surface text-trippy-500",
                        )}
                      >
                        <StepIcon size={20} />
                      </div>
                      <h3 className="text-lg font-black">{step.title}</h3>
                      <p className={cn("mt-2 text-sm leading-6", active ? "text-white" : "text-muted")}>{step.desc}</p>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-7xl px-4 py-12 lg:px-8 lg:py-14">
          <motion.div
            className="overflow-hidden rounded-lg border border-border bg-surface p-5 shadow-sm sm:p-6"
            initial={{ opacity: 0, y: 28 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-80px" }}
            transition={{ duration: 0.52, ease: "easeOut" }}
          >
            <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="text-2xl font-black leading-tight text-foreground sm:text-3xl">
                  Ready to build your plan?
                </h2>
                <p className="mt-2 max-w-2xl text-sm font-semibold text-muted">
                  Open the AI builder or create an account to save trips.
                </p>
              </div>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                <Button
                  size="lg"
                  onClick={() => openAIBuilder(false)}
                  className="w-full min-w-44 rounded-lg sm:w-auto"
                >
                  Try AI Builder <ArrowRight size={16} />
                </Button>
                <Link href="/register" className="w-full sm:w-auto">
                  <Button
                    variant="secondary"
                    size="lg"
                    className="w-full min-w-44 rounded-lg whitespace-nowrap"
                  >
                    Create account
                  </Button>
                </Link>
              </div>
            </div>
          </motion.div>
        </section>
      </main>

      <footer className="border-t border-border bg-surface px-4 py-10 lg:px-8">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <Logo size="md" />
          <div className="flex flex-wrap gap-5 text-sm font-semibold text-muted">
            <a href="#discover" className="hover:text-trippy-500">Discover</a>
            <a href="#planning" className="hover:text-trippy-500">How it works</a>
            <Link href="/login" className="hover:text-trippy-500">Log in</Link>
          </div>
        </div>
      </footer>

      <AITripBuilderModal
        open={showAIBuilder}
        onClose={() => setShowAIBuilder(false)}
        initialRequest={aiBuilderRequest}
      />
    </div>
  );
}

function AnimatedTravelBackdrop() {
  const pins = [
    { label: "Lisbon", top: "17%", left: "14%", delay: 0 },
    { label: "Kyoto", top: "21%", left: "78%", delay: 0.35 },
    { label: "Bali", top: "69%", left: "82%", delay: 0.7 },
    { label: "Banff", top: "72%", left: "19%", delay: 1.05 },
  ];

  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute inset-0 bg-[linear-gradient(#d8d2c8_1px,transparent_1px),linear-gradient(90deg,#d8d2c8_1px,transparent_1px)] bg-[size:72px_72px] opacity-20" />
      <motion.svg
        viewBox="0 0 1200 720"
        className="absolute inset-0 h-full w-full opacity-80"
        preserveAspectRatio="none"
      >
        <path
          d="M-40 590 C120 520 180 635 330 548 C500 448 590 520 720 412 C860 292 980 350 1240 210"
          fill="none"
          stroke="#d8d2c8"
          strokeWidth="2"
          strokeDasharray="10 16"
        />
        <motion.path
          d="M-40 590 C120 520 180 635 330 548 C500 448 590 520 720 412 C860 292 980 350 1240 210"
          fill="none"
          stroke="#123C69"
          strokeWidth="4"
          strokeLinecap="round"
          initial={{ pathLength: 0, opacity: 0.2 }}
          animate={{ pathLength: [0, 1, 1], opacity: [0.2, 0.8, 0.2] }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.path
          d="M140 160 C230 96 348 112 420 178 C506 257 638 214 720 142 C790 80 922 92 1040 156"
          fill="none"
          stroke="#2f5d50"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="6 18"
          animate={{ strokeDashoffset: [0, -72] }}
          transition={{ duration: 16, repeat: Infinity, ease: "linear" }}
        />
        <motion.path
          d="M120 440 C230 392 304 420 382 472 C504 552 620 488 682 426 C760 348 858 394 932 476 C1000 552 1092 526 1160 456"
          fill="none"
          stroke="#e76f51"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="5 20"
          animate={{ strokeDashoffset: [0, 90] }}
          transition={{ duration: 18, repeat: Infinity, ease: "linear" }}
        />
      </motion.svg>

      {pins.map((pin) => (
        <motion.div
          key={pin.label}
          className="absolute hidden items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-black text-trippy-500 shadow-md sm:flex"
          style={{ top: pin.top, left: pin.left }}
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: [0.35, 0.9, 0.35], y: [14, 0, 14] }}
          transition={{ delay: pin.delay, duration: 6, repeat: Infinity, ease: "easeInOut" }}
        >
          <MapPin size={15} className="text-accent-500" />
          {pin.label}
        </motion.div>
      ))}
    </div>
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
        className={cn(
          "flex min-h-16 w-full items-center gap-3 rounded-lg border bg-shore-50 px-4 text-left transition-all duration-200 focus-visible:focus-ring",
          open ? "border-trippy-500 shadow-md" : "border-border hover:border-trippy-500",
        )}
      >
        <Calendar size={18} className="text-trippy-500" />
        <span className="min-w-0 flex-1">
          <span className="block text-xs font-black uppercase text-muted">Dates</span>
          <span className={cn("mt-1 block truncate text-sm font-black", rangeLabel ? "text-foreground" : "text-muted")}>
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
            className="absolute left-0 top-full z-40 mt-2 w-full min-w-[20rem] max-w-[calc(100vw-2rem)] rounded-lg border border-border bg-surface p-4 text-left shadow-2xl"
          >
            <div className="mb-4 flex items-center justify-between">
              <button
                type="button"
                onClick={() => setCalendarMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))}
                className="grid h-9 w-9 place-items-center rounded-md border border-border bg-shore-50 text-lg font-black text-trippy-500 hover:border-trippy-500"
                aria-label="Previous month"
              >
                &lt;
              </button>
              <div className="text-center">
                <p className="text-sm font-black">{monthStart.toLocaleDateString(undefined, { month: "long", year: "numeric" })}</p>
                <p className="mt-0.5 text-xs font-semibold text-muted">
                  {start && !end ? "Select an end date" : "Select a start date"}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setCalendarMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))}
                className="grid h-9 w-9 place-items-center rounded-md border border-border bg-shore-50 text-lg font-black text-trippy-500 hover:border-trippy-500"
                aria-label="Next month"
              >
                &gt;
              </button>
            </div>

            <div className="grid grid-cols-7 gap-1 text-center">
              {["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"].map((day) => (
                <div key={day} className="py-2 text-[11px] font-black uppercase text-muted">
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
                      "grid h-10 place-items-center rounded-md text-sm font-bold transition-all",
                      isStart || isEnd
                        ? "bg-trippy-500 text-white"
                        : isBetween
                          ? "bg-lagoon-100 text-trippy-500"
                          : "text-foreground hover:bg-shore-100 hover:text-trippy-500",
                    )}
                  >
                    {date.getDate()}
                  </button>
                );
              })}
            </div>

            <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
              <button
                type="button"
                onClick={clearDates}
                className="text-sm font-bold text-muted transition-colors hover:text-danger"
              >
                Clear
              </button>
              <Button type="button" size="sm" className="rounded-md" onClick={() => setOpen(false)}>
                Done
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function PlannerDropdown({
  icon,
  label,
  value,
  options,
  onChange,
}: {
  icon: ReactNode;
  label: string;
  value: string;
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

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className={cn(
          "flex min-h-14 w-full items-center gap-3 rounded-lg border bg-shore-50 px-4 text-left transition-all duration-200 focus-visible:focus-ring",
          open ? "border-trippy-500 shadow-md" : "border-border hover:border-trippy-500",
        )}
      >
        <span className="text-trippy-500">{icon}</span>
        <span className="min-w-0 flex-1">
          <span className="block text-xs font-black uppercase text-muted">{label}</span>
          <span className="mt-0.5 block truncate text-sm font-black text-foreground">{value}</span>
        </span>
        <ChevronDown size={16} className={cn("text-muted transition-transform", open && "rotate-180")} />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="absolute left-0 top-full z-40 mt-2 w-full min-w-[11rem] overflow-hidden rounded-lg border border-border bg-surface p-1 shadow-xl"
          >
            {options.map((option) => {
              const selected = option === value;
              return (
                <button
                  key={option}
                  type="button"
                  onClick={() => {
                    onChange(option);
                    setOpen(false);
                  }}
                  className={cn(
                    "flex w-full items-center justify-between rounded-md px-3 py-2.5 text-left text-sm font-bold transition-colors",
                    selected
                      ? "bg-trippy-500 text-white"
                      : "text-foreground hover:bg-shore-100 hover:text-trippy-500",
                  )}
                >
                  {option}
                  {selected && <Check size={15} />}
                </button>
              );
            })}
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

function SectionHeading({
  eyebrow,
  title,
  description,
}: {
  eyebrow: string;
  title: string;
  description: string;
}) {
  return (
    <motion.div
      className="mx-auto max-w-2xl text-center"
      initial={{ opacity: 0, y: 22 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      <p className="text-xs font-black uppercase text-accent-500">{eyebrow}</p>
      <h2 className="mt-2 text-3xl font-black sm:text-4xl">{title}</h2>
      <p className="mt-3 text-base leading-7 text-muted">{description}</p>
    </motion.div>
  );
}
