import { NextResponse } from "next/server";
import { callGroqWithRetry } from "@/lib/groq-client";

interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

interface Activity {
  time?: string;
  title: string;
  description?: string;
  location?: string;
  category?: string;
  estimatedCost?: string;
  duration?: number;
}

interface DayPlan {
  dayNumber: number;
  date?: string;
  title: string;
  activities: Activity[];
}

interface ChatBody {
  messages: ChatMessage[];
  tripContext: string;
  currentItinerary?: DayPlan[];
  destination?: string;
}

/* ── Modification engine ──────────────────────────────────────────── */
function parseModificationIntent(msg: string): {
  action: "swap" | "add" | "remove" | "removeDay" | "vegetarian" | "budget" | "relax" | "adventure" | "nightlife" | "general";
  dayTarget?: number;
  keywords: string[];
} {
  const m = msg.toLowerCase();
  const dayMatch = m.match(/day\s*(\d+)/);
  const dayTarget = dayMatch ? parseInt(dayMatch[1]) : undefined;
  const keywords = m.split(/\s+/).filter(w => w.length > 2);

  if (m.includes("vegetarian") || m.includes("vegan") || m.includes("veg ")) return { action: "vegetarian", dayTarget, keywords };
  if (m.includes("swap") || m.includes("replace") || m.includes("change") || m.includes("instead")) return { action: "swap", dayTarget, keywords };
  if (m.includes("add") || m.includes("include") || m.includes("insert")) return { action: "add", dayTarget, keywords };
  // "remove day 7" → removes entire day; "remove lunch" → removes activity
  if (m.includes("remove") || m.includes("delete") || m.includes("skip") || m.includes("drop")) {
    // Check if user wants to remove an entire day (e.g. "remove day 7", "delete last day", "remove day 3")
    if (dayTarget && (m.includes("remove day") || m.includes("delete day") || m.includes("drop day") || m.includes("skip day"))) {
      return { action: "removeDay", dayTarget, keywords };
    }
    if (m.includes("last day") || m.includes("final day")) {
      return { action: "removeDay", dayTarget: -1, keywords }; // -1 = last day
    }
    return { action: "remove", dayTarget, keywords };
  }
  if (m.includes("budget") || m.includes("cheap") || m.includes("save") || m.includes("affordable")) return { action: "budget", dayTarget, keywords };
  if (m.includes("relax") || m.includes("chill") || m.includes("slow") || m.includes("easy")) return { action: "relax", dayTarget, keywords };
  if (m.includes("adventure") || m.includes("exciting") || m.includes("thrill")) return { action: "adventure", dayTarget, keywords };
  if (m.includes("night") || m.includes("bar") || m.includes("club") || m.includes("party")) return { action: "nightlife", dayTarget, keywords };
  return { action: "general", dayTarget, keywords };
}

const VEGGIE_SWAPS: Record<string, Activity> = {
  default: { time: "13:00", title: "Plant-based Lunch at Local Vegan Café", description: "Fresh seasonal dishes — try the Buddha bowl or falafel wrap.", location: "City Center", category: "FOOD", estimatedCost: "€15", duration: 60 },
};

const BUDGET_SWAPS: Activity[] = [
  { time: "08:00", title: "Free Walking Tour", description: "Join a tip-based walking tour to discover hidden gems with a local guide.", location: "Main Square, Meeting Point", category: "CULTURE", estimatedCost: "€0 (tips welcome)", duration: 120 },
  { time: "13:00", title: "Street Food Lunch", description: "Sample local street food from market stalls — authentic and incredibly affordable.", location: "Central Market Area", category: "FOOD", estimatedCost: "€5", duration: 45 },
  { time: "15:00", title: "Park & Gardens Walk", description: "Explore the city's beautiful public parks and gardens — totally free!", location: "City Park", category: "NATURE", estimatedCost: "€0", duration: 90 },
];

const ADVENTURE_SWAPS: Activity[] = [
  { time: "09:00", title: "Morning Bike Tour", description: "Explore the city on two wheels — cover more ground and find hidden spots.", location: "Bike Rental Near Old Town", category: "SIGHTSEEING", estimatedCost: "€25", duration: 150 },
  { time: "14:00", title: "Rooftop Climb & Observation", description: "Get your adrenaline pumping with panoramic views from the highest point.", location: "City Observation Tower", category: "SIGHTSEEING", estimatedCost: "€20", duration: 90 },
];

const NIGHTLIFE_SWAPS: Activity[] = [
  { time: "20:00", title: "Sunset Rooftop Drinks", description: "Start the evening with craft cocktails and golden hour views.", location: "Rooftop Bar, City Center", category: "NIGHTLIFE", estimatedCost: "€18", duration: 90 },
  { time: "22:00", title: "Live Music & Jazz Bar", description: "Enjoy local live music in an intimate jazz bar setting.", location: "Jazz Quarter", category: "NIGHTLIFE", estimatedCost: "€12", duration: 120 },
];

function applyModification(
  itinerary: DayPlan[],
  intent: ReturnType<typeof parseModificationIntent>,
): { updatedItinerary: DayPlan[]; changes: Change[] } {
  const changes: Change[] = [];
  const updated = JSON.parse(JSON.stringify(itinerary)) as DayPlan[];

  // Handle dayTarget: -1 means last day
  const resolvedDayTarget = intent.dayTarget === -1 ? Math.max(...updated.map(d => d.dayNumber)) : intent.dayTarget;

  const targetDays = resolvedDayTarget
    ? updated.filter(d => d.dayNumber === resolvedDayTarget)
    : updated;

  if (!targetDays.length && intent.action !== "removeDay") {
    return { updatedItinerary: updated, changes: [{ type: "info", summary: "No matching day found. Try specifying 'Day 1', 'Day 2', etc." }] };
  }

  switch (intent.action) {
    case "vegetarian": {
      for (const day of targetDays) {
        day.activities = day.activities.map(a => {
          if (a.category === "FOOD" && a.title.toLowerCase().match(/meat|steak|burger|bbq|pork|mutton|korma|kebab|chicken/)) {
            const veg = { ...VEGGIE_SWAPS.default, time: a.time };
            changes.push({ type: "swap", dayNumber: day.dayNumber, removed: a.title, added: veg.title, summary: `Swapped "${a.title}" → "${veg.title}"` });
            return veg;
          }
          return a;
        });
      }
      if (!changes.length) {
        // Still make food items vegetarian-friendly
        for (const day of targetDays) {
          const foodIdx = day.activities.findIndex(a => a.category === "FOOD");
          if (foodIdx >= 0) {
            const old = day.activities[foodIdx];
            const veg = { ...VEGGIE_SWAPS.default, time: old.time };
            changes.push({ type: "swap", dayNumber: day.dayNumber, removed: old.title, added: veg.title, summary: `Swapped "${old.title}" → vegetarian option` });
            day.activities[foodIdx] = veg;
            break;
          }
        }
      }
      break;
    }

    case "budget": {
      for (const day of targetDays) {
        // Replace the most expensive activity
        let maxCostIdx = -1;
        let maxCost = 0;
        day.activities.forEach((a, i) => {
          const cost = parseInt((a.estimatedCost || "€0").replace(/[^0-9]/g, "")) || 0;
          if (cost > maxCost) { maxCost = cost; maxCostIdx = i; }
        });
        if (maxCostIdx >= 0 && maxCost > 10) {
          const old = day.activities[maxCostIdx];
          const swap = { ...BUDGET_SWAPS[Math.floor(Math.random() * BUDGET_SWAPS.length)], time: old.time };
          changes.push({ type: "swap", dayNumber: day.dayNumber, removed: old.title, added: swap.title, summary: `Replaced expensive "${old.title}" (${old.estimatedCost}) → "${swap.title}" (${swap.estimatedCost})` });
          day.activities[maxCostIdx] = swap;
          break; // One change at a time
        }
      }
      break;
    }

    case "adventure": {
      const day = targetDays[0];
      const sightseeingIdx = day.activities.findIndex(a => a.category === "SIGHTSEEING" || a.category === "CULTURE");
      if (sightseeingIdx >= 0) {
        const old = day.activities[sightseeingIdx];
        const swap = { ...ADVENTURE_SWAPS[Math.floor(Math.random() * ADVENTURE_SWAPS.length)], time: old.time };
        changes.push({ type: "swap", dayNumber: day.dayNumber, removed: old.title, added: swap.title, summary: `Replaced "${old.title}" → adventurous "${swap.title}"` });
        day.activities[sightseeingIdx] = swap;
      }
      break;
    }

    case "nightlife": {
      const day = targetDays[0];
      const lastAct = day.activities[day.activities.length - 1];
      if (lastAct && lastAct.category !== "NIGHTLIFE") {
        const nightAct = { ...NIGHTLIFE_SWAPS[0], time: "21:00" };
        day.activities.push(nightAct);
        changes.push({ type: "add", dayNumber: day.dayNumber, added: nightAct.title, summary: `Added "${nightAct.title}" to the evening` });
      }
      break;
    }

    case "relax": {
      const day = targetDays[0];
      if (day.activities.length > 5) {
        const removed = day.activities.splice(3, 1)[0];
        changes.push({ type: "remove", dayNumber: day.dayNumber, removed: removed.title, summary: `Removed "${removed.title}" for a more relaxed pace` });
      }
      // Add a wellness activity
      const wellness: Activity = { time: "15:00", title: "Spa & Relaxation Break", description: "Unwind with a local spa treatment or hammam experience.", location: "Wellness Center, City Center", category: "WELLNESS", estimatedCost: "€30", duration: 90 };
      day.activities.push(wellness);
      day.activities.sort((a, b) => (a.time || "").localeCompare(b.time || ""));
      changes.push({ type: "add", dayNumber: day.dayNumber, added: wellness.title, summary: `Added "${wellness.title}" for relaxation` });
      break;
    }

    case "add": {
      const day = targetDays[0];
      const newAct: Activity = { time: "16:00", title: "Custom Activity", description: intent.keywords.slice(0, 6).join(" "), location: "To be determined", category: "SIGHTSEEING", estimatedCost: "€15", duration: 60 };
      day.activities.push(newAct);
      day.activities.sort((a, b) => (a.time || "").localeCompare(b.time || ""));
      changes.push({ type: "add", dayNumber: day.dayNumber, added: newAct.title, summary: `Added new activity to Day ${day.dayNumber}` });
      break;
    }

    case "removeDay": {
      if (updated.length <= 1) {
        changes.push({ type: "info", summary: "Can't remove the last remaining day." });
        break;
      }
      const dayNum = resolvedDayTarget || updated[updated.length - 1].dayNumber;
      const idx = updated.findIndex(d => d.dayNumber === dayNum);
      if (idx >= 0) {
        const removedDay = updated.splice(idx, 1)[0];
        // Renumber remaining days
        updated.forEach((d, i) => {
          d.dayNumber = i + 1;
          // Recalculate dates if available
          if (updated[0]?.date) {
            const baseDate = new Date(updated[0].date);
            baseDate.setDate(baseDate.getDate() + i);
            d.date = baseDate.toISOString().slice(0, 10);
          }
        });
        changes.push({ type: "remove", dayNumber: dayNum, removed: removedDay.title, summary: `Removed entire Day ${dayNum} ("${removedDay.title}") — trip is now ${updated.length} days` });
      } else {
        changes.push({ type: "info", summary: `Day ${dayNum} not found. Your trip has ${updated.length} days.` });
      }
      break;
    }

    case "remove": {
      const day = targetDays[0];
      if (day.activities.length > 2) {
        const match = day.activities.findIndex(a =>
          intent.keywords.some(kw => a.title.toLowerCase().includes(kw))
        );
        const idx = match >= 0 ? match : day.activities.length - 1;
        const removed = day.activities.splice(idx, 1)[0];
        changes.push({ type: "remove", dayNumber: day.dayNumber, removed: removed.title, summary: `Removed "${removed.title}" from Day ${day.dayNumber}` });
      }
      break;
    }

    case "swap": {
      const day = targetDays[0];
      const matchIdx = day.activities.findIndex(a =>
        intent.keywords.some(kw => a.title.toLowerCase().includes(kw))
      );
      if (matchIdx >= 0) {
        const old = day.activities[matchIdx];
        const newAct: Activity = { ...old, title: "Updated: " + old.title, description: "Modified based on your preference." };
        day.activities[matchIdx] = newAct;
        changes.push({ type: "swap", dayNumber: day.dayNumber, removed: old.title, added: newAct.title, summary: `Updated "${old.title}" in Day ${day.dayNumber}` });
      } else {
        changes.push({ type: "info", summary: "Couldn't find a matching activity to swap. Try being more specific (e.g., 'swap lunch on Day 1')." });
      }
      break;
    }

    default:
      changes.push({ type: "info", summary: "I can help you modify your trip! Try: 'make it more budget-friendly', 'add nightlife to Day 2', 'swap lunch to vegetarian', or 'make Day 1 more relaxed'." });
  }

  if (!changes.length) {
    changes.push({ type: "info", summary: "No changes were needed. Your itinerary looks great as is!" });
  }

  return { updatedItinerary: updated, changes };
}

interface Change {
  type: "swap" | "add" | "remove" | "info";
  dayNumber?: number;
  removed?: string;
  added?: string;
  summary: string;
}

export async function POST(request: Request) {
  let body: ChatBody;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  const lastMessage = body.messages[body.messages.length - 1]?.content || "";
  const intent = parseModificationIntent(lastMessage);

  // If we have a current itinerary, apply modifications
  if (body.currentItinerary?.length && intent.action !== "general") {
    const { updatedItinerary, changes } = applyModification(body.currentItinerary, intent);

    const replyParts = ["✨ **Trip Updated!**\n"];
    for (const c of changes) {
      if (c.type === "swap") replyParts.push(`🔄 **Day ${c.dayNumber}**: ${c.summary}`);
      else if (c.type === "add") replyParts.push(`➕ **Day ${c.dayNumber}**: ${c.summary}`);
      else if (c.type === "remove") replyParts.push(`🗑️ **Day ${c.dayNumber}**: ${c.summary}`);
      else replyParts.push(`💡 ${c.summary}`);
    }

    return NextResponse.json({
      reply: replyParts.join("\n"),
      updatedItinerary,
      changes,
      hasModification: true,
    });
  }

  // General chat — try Groq, fallback to smart local
  try {
    const groqReply = await tryGroqChat(body);
    if (groqReply) return NextResponse.json({ reply: groqReply, hasModification: false });
  } catch { /* fall through */ }

  // Smart local response
  const reply = getSmartReply(lastMessage, body.destination);
  return NextResponse.json({ reply, hasModification: false });
}

function getSmartReply(msg: string, destination?: string): string {
  const m = msg.toLowerCase();
  const dest = destination || "your destination";

  if (m.includes("restaurant") || m.includes("food") || m.includes("eat"))
    return `🍽️ **Great food tips for ${dest}:**\n\n• Look for restaurants rated 4.5+ on Google Maps\n• Ask locals — hotel staff always know the best spots\n• Lunch menus are often 30-50% cheaper than dinner\n• Markets are perfect for affordable, authentic meals\n\n*Try saying "make lunch vegetarian" or "swap dinner to something budget-friendly" to modify your trip!*`;

  if (m.includes("hotel") || m.includes("stay") || m.includes("accommodation"))
    return `🏨 **Accommodation tips for ${dest}:**\n\n• Book 2-3 weeks ahead for best rates\n• City center = walkable but pricier\n• Use Booking.com, Airbnb, or Hostelworld\n• Read recent reviews (last 3 months)\n\n*I can also modify your itinerary — try "make the trip more relaxed" or "add more culture"!*`;

  if (m.includes("weather") || m.includes("pack") || m.includes("wear"))
    return `🧳 **Packing essentials:**\n\n• Comfortable walking shoes (you'll walk 10-15km/day!)\n• Light layers for temperature changes\n• Rain jacket or compact umbrella\n• Universal power adapter\n• Refillable water bottle\n\n*Want to modify activities? Try "add a spa day" or "swap to outdoor activities"!*`;

  return `🌍 **I can modify your trip!** Try:\n\n• "Make Day 1 more relaxed"\n• "Swap lunch to a vegetarian place"\n• "Add nightlife to Day 2"\n• "Make it more budget-friendly"\n• "Add an adventure activity"\n• "Remove shopping from Day 3"\n\nJust describe what you want changed and I'll update your itinerary! ✨`;
}

async function tryGroqChat(body: ChatBody): Promise<string | null> {
  try {
    const result = await callGroqWithRetry(
      [
        { role: "system", content: `You are Trippy AI, a travel assistant.\n\nTrip context:\n${body.tripContext}\n\nRules: Only travel topics. Concise. Use EUR (€).` },
        ...body.messages.map(m => ({ role: m.role as "user" | "assistant", content: m.content })),
      ],
      { maxTokens: 2048, timeoutMs: 20_000 },
    );
    return result.content;
  } catch {
    return null;
  }
}

