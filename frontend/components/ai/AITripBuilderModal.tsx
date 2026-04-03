"use client";

import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  X,
  
  Send,
  MapPin,
  Calendar,
  Users,
  Sparkles,
  Loader2,
  Check,
  Star,
  DollarSign,
} from "lucide-react";
import Button from "@/components/ui/Button";
import GlassCard from "@/components/ui/GlassCard";

/* ── Types ─────────────────────────────────────────────────────────────── */

interface Message {
  id: string;
  role: "user" | "ai";
  content: string;
  tripCard?: GeneratedTrip;
}

interface GeneratedTrip {
  title: string;
  destination: string;
  duration: string;
  budget: string;
  groupSize: string;
  rating: number;
  itinerary: { day: number; title: string; activities: string[] }[];
  highlights: string[];
  image: string;
}

/* ── Pre-built AI responses ────────────────────────────────────────────── */

const AI_RESPONSES: Record<string, { reply: string; trip: GeneratedTrip }> = {
  beach: {
    reply:
      "I found the perfect beach getaway for you! Here's a curated trip to Bali with stunning beaches, vibrant culture, and incredible sunsets.",
    trip: {
      title: "Bali Beach Paradise",
      destination: "Bali, Indonesia",
      duration: "7 days",
      budget: "$1,200",
      groupSize: "2-4",
      rating: 4.9,
      itinerary: [
        { day: 1, title: "Arrival & Seminyak", activities: ["Airport transfer", "Beach sunset at Seminyak", "Welcome dinner at La Lucciola"] },
        { day: 2, title: "Uluwatu Explorer", activities: ["Uluwatu Temple visit", "Cliff-side lunch", "Kecak fire dance at sunset"] },
        { day: 3, title: "Nusa Penida Day Trip", activities: ["Speedboat to Nusa Penida", "Kelingking Beach viewpoint", "Snorkeling at Crystal Bay"] },
        { day: 4, title: "Cultural Ubud", activities: ["Tegallalang Rice Terraces", "Ubud Monkey Forest", "Traditional Balinese spa"] },
        { day: 5, title: "Water Adventures", activities: ["Surfing lesson at Kuta", "Underwater sea walk", "Beach BBQ"] },
        { day: 6, title: "Hidden Gems", activities: ["Tirta Gangga water palace", "Lempuyang Temple gates", "Local warung food tour"] },
        { day: 7, title: "Farewell", activities: ["Sunrise yoga session", "Last-minute shopping", "Departure transfer"] },
      ],
      highlights: ["Private villa with pool", "Traditional cooking class", "All transfers included", "Local guide throughout"],
      image: "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800",
    },
  },
  europe: {
    reply:
      "Europe is calling! I've designed an amazing multi-city adventure through the most iconic European destinations.",
    trip: {
      title: "European Grand Tour",
      destination: "Paris → Rome → Barcelona",
      duration: "10 days",
      budget: "$2,800",
      groupSize: "2-6",
      rating: 4.8,
      itinerary: [
        { day: 1, title: "Bonjour Paris!", activities: ["Eiffel Tower visit", "Seine river cruise", "Dinner in Le Marais"] },
        { day: 2, title: "Parisian Culture", activities: ["Louvre Museum", "Notre-Dame area walk", "Montmartre sunset"] },
        { day: 3, title: "Versailles Day Trip", activities: ["Palace of Versailles", "Gardens exploration", "Train back to Paris"] },
        { day: 4, title: "Ciao Roma!", activities: ["Flight to Rome", "Colosseum tour", "Trastevere dinner"] },
        { day: 5, title: "Vatican & History", activities: ["Vatican Museums", "Sistine Chapel", "Trevi Fountain at night"] },
        { day: 6, title: "Roman Holiday", activities: ["Roman Forum", "Palatine Hill", "Gelato tour"] },
        { day: 7, title: "¡Hola Barcelona!", activities: ["Flight to Barcelona", "La Rambla walk", "Tapas tour in El Born"] },
        { day: 8, title: "Gaudí Day", activities: ["Sagrada Família", "Park Güell", "Casa Batlló"] },
        { day: 9, title: "Beach & Gothic", activities: ["Barceloneta Beach", "Gothic Quarter", "Flamenco show"] },
        { day: 10, title: "Departure", activities: ["La Boqueria market", "Last shopping", "Airport transfer"] },
      ],
      highlights: ["Intercity flights included", "Skip-the-line passes", "Boutique hotels", "Local food experiences"],
      image: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800",
    },
  },
  adventure: {
    reply:
      "For the thrill-seekers! Here's an adrenaline-packed adventure trip to New Zealand — the adventure capital of the world.",
    trip: {
      title: "New Zealand Adventure Rush",
      destination: "Queenstown, New Zealand",
      duration: "8 days",
      budget: "$2,200",
      groupSize: "2-8",
      rating: 4.9,
      itinerary: [
        { day: 1, title: "Welcome to Queenstown", activities: ["Airport pickup", "Skyline Gondola & luge", "Fergburger dinner"] },
        { day: 2, title: "Extreme Day", activities: ["Bungee jumping at Kawarau Bridge", "Jet boat ride", "Onsen hot pools"] },
        { day: 3, title: "Milford Sound", activities: ["Scenic drive", "Milford Sound cruise", "Kayaking in the fjords"] },
        { day: 4, title: "Ski & Snow", activities: ["Coronet Peak skiing/snowboarding", "Après-ski", "Stargazing tour"] },
        { day: 5, title: "Skydiving Day", activities: ["15,000ft tandem skydive", "Recovery lunch", "Lake Wakatipu sunset cruise"] },
        { day: 6, title: "Hiking Paradise", activities: ["Routeburn Track day hike", "Waterfall discoveries", "Mountain hut lunch"] },
        { day: 7, title: "Water Adventures", activities: ["White water rafting", "Canyoning experience", "Farewell dinner"] },
        { day: 8, title: "Departure", activities: ["Sunrise hike", "Souvenir shopping", "Airport transfer"] },
      ],
      highlights: ["All adventure gear included", "Professional instructors", "Mountain lodge accommodation", "GoPro footage included"],
      image: "https://images.unsplash.com/photo-1469521669194-babb45599def?w=800",
    },
  },
};

const FALLBACK_RESPONSE = {
  reply:
    "Great choice! Based on your preferences, I've crafted a wonderful trip. Here's what I came up with:",
  trip: {
    title: "Wanderlust Explorer Package",
    destination: "Thailand & Cambodia",
    duration: "9 days",
    budget: "$1,600",
    groupSize: "2-6",
    rating: 4.7,
    itinerary: [
      { day: 1, title: "Bangkok Arrival", activities: ["Grand Palace visit", "Street food tour", "Rooftop bar sunset"] },
      { day: 2, title: "Temple Run", activities: ["Wat Pho & Wat Arun", "Floating market", "Thai massage"] },
      { day: 3, title: "Northern Escape", activities: ["Flight to Chiang Mai", "Night Bazaar", "Khao Soi dinner"] },
      { day: 4, title: "Nature Day", activities: ["Ethical elephant sanctuary", "Bamboo rafting", "Cooking class"] },
      { day: 5, title: "Island Vibes", activities: ["Flight to Krabi", "Railay Beach", "Rock climbing intro"] },
      { day: 6, title: "Island Hopping", activities: ["4 Islands tour", "Snorkeling", "Beach bonfire"] },
      { day: 7, title: "Cambodia Bound", activities: ["Flight to Siem Reap", "Pub Street exploration", "Apsara dance show"] },
      { day: 8, title: "Angkor Wat", activities: ["Sunrise at Angkor Wat", "Ta Prohm temple", "Bayon temple faces"] },
      { day: 9, title: "Farewell", activities: ["Floating village visit", "Last shopping", "Departure transfer"] },
    ],
    highlights: ["All domestic flights", "Mix of hotels & boutique stays", "Local guides", "Cooking class included"],
    image: "https://images.unsplash.com/photo-1528181304800-259b08848526?w=800",
  },
};

const SUGGESTIONS = [
  "Plan a beach vacation for 2 in Bali",
  "Create a European city-hopping adventure",
  "I want an extreme adventure trip",
  "Surprise me with something amazing!",
];

/* ── Helper: detect which preset to use ───────────────────────────────── */

function matchPreset(input: string): { reply: string; trip: GeneratedTrip } {
  const lower = input.toLowerCase();
  if (lower.includes("beach") || lower.includes("bali") || lower.includes("island") || lower.includes("tropical"))
    return AI_RESPONSES.beach;
  if (lower.includes("europe") || lower.includes("paris") || lower.includes("rome") || lower.includes("city"))
    return AI_RESPONSES.europe;
  if (lower.includes("adventure") || lower.includes("extreme") || lower.includes("hiking") || lower.includes("skydive"))
    return AI_RESPONSES.adventure;
  return FALLBACK_RESPONSE;
}

/* ── Component ─────────────────────────────────────────────────────────── */

interface AITripBuilderModalProps {
  open: boolean;
  onClose: () => void;
}

export default function AITripBuilderModal({ open, onClose }: AITripBuilderModalProps) {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "ai",
      content:
        "Hi! I'm your AI trip planner. Tell me about your dream trip — where you want to go, what you love doing, your budget, or just a vibe — and I'll create a personalized itinerary for you!",
    },
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [savedTrips, setSavedTrips] = useState<Set<string>>(new Set());
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  // Reset state when modal closes
  useEffect(() => {
    if (!open) {
      const timer = setTimeout(() => {
        setMessages([
          {
            id: "welcome",
            role: "ai",
            content:
              "Hi! I'm your AI trip planner. Tell me about your dream trip — where you want to go, what you love doing, your budget, or just a vibe — and I'll create a personalized itinerary for you!",
          },
        ]);
        setInput("");
        setIsTyping(false);
        setSavedTrips(new Set());
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [open]);

  function handleSend(text?: string) {
    const msg = (text ?? input).trim();
    if (!msg || isTyping) return;

    const userMsg: Message = { id: `user-${Date.now()}`, role: "user", content: msg };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    // Simulate AI "thinking" delay
    const delay = 1500 + Math.random() * 1500;
    setTimeout(() => {
      const preset = matchPreset(msg);
      const aiMsg: Message = {
        id: `ai-${Date.now()}`,
        role: "ai",
        content: preset.reply,
        tripCard: preset.trip,
      };
      setMessages((prev) => [...prev, aiMsg]);
      setIsTyping(false);
    }, delay);
  }

  function handleSave(title: string) {
    setSavedTrips((prev) => new Set(prev).add(title));
  }

  if (!open) return null;

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          {/* Backdrop */}
          <motion.div
            className="absolute inset-0 bg-black/30 backdrop-blur-sm"
            onClick={onClose}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />

          {/* Modal */}
          <motion.div
            className="relative z-10 w-full max-w-2xl h-[85vh] max-h-[700px] flex flex-col rounded-2xl overflow-hidden"
            initial={{ opacity: 0, y: 40, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 40, scale: 0.95 }}
            transition={{ type: "spring", damping: 25, stiffness: 300 }}
          >
            <GlassCard variant="strong" className="flex flex-col h-full !p-0 !rounded-2xl">
              {/* Header */}
              <div className="flex items-center justify-between px-5 py-4 border-b border-border">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-trippy-500 flex items-center justify-center">
                    <Sparkles size={18} className="text-white" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm">AI Trip Builder</h3>
                    <p className="text-xs text-muted">Powered by Trippy AI</p>
                  </div>
                </div>
                <button
                  onClick={onClose}
                  className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-surface transition-colors cursor-pointer"
                >
                  <X size={16} className="text-muted" />
                </button>
              </div>

              {/* Chat messages */}
              <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
                {messages.map((msg) => (
                  <motion.div
                    key={msg.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}
                  >
                    <div
                      className={`max-w-[85%] ${
                        msg.role === "user"
                          ? "bg-trippy-500 text-white rounded-2xl rounded-br-md px-4 py-2.5"
                          : "space-y-3"
                      }`}
                    >
                      {msg.role === "ai" && (
                        <div className="glass rounded-2xl rounded-bl-md px-4 py-2.5">
                          <p className="text-sm leading-relaxed">{msg.content}</p>
                        </div>
                      )}
                      {msg.role === "user" && (
                        <p className="text-sm leading-relaxed">{msg.content}</p>
                      )}

                      {/* Trip card */}
                      {msg.tripCard && (
                        <TripResultCard
                          trip={msg.tripCard}
                          saved={savedTrips.has(msg.tripCard.title)}
                          onSave={() => handleSave(msg.tripCard!.title)}
                        />
                      )}
                    </div>
                  </motion.div>
                ))}

                {/* Typing indicator */}
                {isTyping && (
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex justify-start"
                  >
                    <div className="glass rounded-2xl rounded-bl-md px-4 py-3 flex items-center gap-2">
                      <Loader2 size={14} className="animate-spin text-trippy-500" />
                      <span className="text-sm text-muted">Planning your perfect trip...</span>
                    </div>
                  </motion.div>
                )}

                <div ref={chatEndRef} />
              </div>

              {/* Suggestions (shown when only welcome message) */}
              {messages.length === 1 && (
                <div className="px-5 pb-2">
                  <p className="text-xs text-muted mb-2">Try one of these:</p>
                  <div className="flex flex-wrap gap-2">
                    {SUGGESTIONS.map((s) => (
                      <button
                        key={s}
                        onClick={() => handleSend(s)}
                        className="text-xs px-3 py-1.5 rounded-full glass hover:bg-surface-hover transition-colors cursor-pointer text-trippy-600 font-medium"
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Input */}
              <div className="px-5 py-4 border-t border-border">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSend();
                  }}
                  className="flex items-center gap-2"
                >
                  <input
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="Describe your dream trip..."
                    className="flex-1 bg-transparent text-sm placeholder:text-muted focus:outline-none"
                    disabled={isTyping}
                  />
                  <button
                    type="submit"
                    disabled={!input.trim() || isTyping}
                    className="w-9 h-9 rounded-xl bg-trippy-500 flex items-center justify-center text-white disabled:opacity-40 transition-opacity cursor-pointer"
                  >
                    <Send size={14} />
                  </button>
                </form>
              </div>
            </GlassCard>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ── Trip Result Card ──────────────────────────────────────────────────── */

function TripResultCard({
  trip,
  saved,
  onSave,
}: {
  trip: GeneratedTrip;
  saved: boolean;
  onSave: () => void;
}) {
  const [showItinerary, setShowItinerary] = useState(false);

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
      className="glass rounded-xl overflow-hidden"
    >
      {/* Hero image */}
      <div className="relative h-36 overflow-hidden">
        <img
          src={trip.image}
          alt={trip.destination}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        <div className="absolute bottom-3 left-3 right-3">
          <h4 className="text-white font-bold text-sm">{trip.title}</h4>
          <div className="flex items-center gap-1 mt-0.5">
            <MapPin size={11} className="text-white/80" />
            <span className="text-white/80 text-xs">{trip.destination}</span>
          </div>
        </div>
        <div className="absolute top-3 right-3 flex items-center gap-1 bg-black/40 backdrop-blur-sm rounded-full px-2 py-0.5">
          <Star size={10} className="text-amber-400" fill="currentColor" />
          <span className="text-white text-xs font-medium">{trip.rating}</span>
        </div>
      </div>

      {/* Info chips */}
      <div className="p-3 space-y-3">
        <div className="flex flex-wrap gap-2">
          <span className="flex items-center gap-1 text-xs bg-trippy-500/10 text-trippy-600 px-2 py-1 rounded-full">
            <Calendar size={10} /> {trip.duration}
          </span>
          <span className="flex items-center gap-1 text-xs bg-accent-400/10 text-accent-600 px-2 py-1 rounded-full">
            <DollarSign size={10} /> {trip.budget}
          </span>
          <span className="flex items-center gap-1 text-xs bg-trippy-500/10 text-trippy-600 px-2 py-1 rounded-full">
            <Users size={10} /> {trip.groupSize} people
          </span>
        </div>

        {/* Highlights */}
        <div className="flex flex-wrap gap-1.5">
          {trip.highlights.map((h) => (
            <span key={h} className="text-[10px] bg-surface px-2 py-0.5 rounded-full text-muted">
              {h}
            </span>
          ))}
        </div>

        {/* Itinerary toggle */}
        <button
          onClick={() => setShowItinerary(!showItinerary)}
          className="text-xs text-trippy-600 font-medium hover:underline cursor-pointer"
        >
          {showItinerary ? "Hide itinerary" : "View full itinerary"} →
        </button>

        <AnimatePresence>
          {showItinerary && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden"
            >
              <div className="space-y-2 pt-1">
                {trip.itinerary.map((day) => (
                  <div key={day.day} className="flex gap-2">
                    <div className="flex flex-col items-center">
                      <div className="w-6 h-6 rounded-full bg-trippy-500 text-white text-[10px] flex items-center justify-center font-bold">
                        {day.day}
                      </div>
                      {day.day < trip.itinerary.length && (
                        <div className="w-px flex-1 bg-trippy-200 my-0.5" />
                      )}
                    </div>
                    <div className="flex-1 pb-2">
                      <p className="text-xs font-semibold">{day.title}</p>
                      <ul className="mt-0.5 space-y-0.5">
                        {day.activities.map((a) => (
                          <li key={a} className="text-[10px] text-muted flex items-start gap-1">
                            <Check size={8} className="text-trippy-500 mt-0.5 shrink-0" />
                            {a}
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Actions */}
        <div className="flex gap-2 pt-1">
          <Button
            size="sm"
            className="flex-1 text-xs"
            onClick={onSave}
            disabled={saved}
          >
            {saved ? (
              <><Check size={12} /> Saved to My Trips</>
            ) : (
              <><Sparkles size={12} /> Save Trip</>
            )}
          </Button>
          <Button variant="secondary" size="sm" className="text-xs">
            Customize
          </Button>
        </div>
      </div>
    </motion.div>
  );
}
