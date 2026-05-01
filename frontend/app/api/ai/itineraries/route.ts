import { NextResponse } from "next/server";
import { callGroqWithRetry, extractJson, type GroqError } from "@/lib/groq-client";

/* ── Types ─────────────────────────────────────────────────────────── */
interface Constraints {
  destination: string;
  startDate: string;
  endDate: string;
  budgetLevel?: string;
  adults?: number;
  children?: number;
}

interface ItineraryBody {
  constraints: Constraints;
  userPrompt?: string;
  tone?: string;
  diet?: string;
  interests?: string[];
  customNotes?: string;
  preferences?: {
    pacePreference?: string;
    mustSeeAttractions?: string[];
    avoidAttractions?: string[];
  };
}

/* ── POST handler ──────────────────────────────────────────────────── */
export async function POST(request: Request) {
  let body: ItineraryBody;

  try {
    body = (await request.json()) as ItineraryBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  if (!body.constraints?.destination) {
    return NextResponse.json({ error: "destination is required" }, { status: 400 });
  }

  const c = body.constraints;

  try {
    const prompt = buildItineraryPrompt(body);

    const result = await callGroqWithRetry(
      [
        {
          role: "system",
          content:
            "You are Trippy AI, an expert travel planner that creates detailed, personalized day-by-day itineraries. " +
            "Always respond with valid JSON. No markdown fences. " +
            "Use REAL place names, restaurants, and attractions that actually exist at the destination. " +
            "Tailor every activity to the user's preferences, dietary needs, budget, and travel style.",
        },
        { role: "user", content: prompt },
      ],
      { maxTokens: 8192, timeoutMs: 60_000 },
    );

    const json = extractJson(result.content);
    let parsed;
    try {
      parsed = JSON.parse(json);
    } catch (parseError) {
      console.error("[AI Itinerary] Failed to parse JSON. Raw content:", result.content.substring(0, 500) + "...");
      return NextResponse.json(
        { error: "AI returned an invalid format. Please try again." },
        { status: 502 }
      );
    }

    if (!parsed.dailyPlan?.length) {
      return NextResponse.json(
        { error: "AI could not generate an itinerary for this destination. Try different preferences." },
        { status: 422 }
      );
    }

    parsed.generatedAt = new Date().toISOString();
    console.log(`[AI Itinerary] Groq returned ${parsed.dailyPlan.length} day(s) for ${c.destination}`);
    return NextResponse.json(parsed);
  } catch (err) {
    const groqErr = err as GroqError;
    if (groqErr.status && groqErr.message) {
      console.error(`[AI Itinerary] Groq error (${groqErr.status}):`, groqErr.message);
      return NextResponse.json(
        { error: groqErr.message },
        { status: groqErr.status >= 500 ? groqErr.status : 502 }
      );
    }
    console.error("[AI Itinerary] Unexpected error:", err);
    return NextResponse.json(
      { error: "Something went wrong generating the itinerary. Please try again." },
      { status: 500 }
    );
  }
}

/* ── Prompt builder — uses ALL user preferences ────────────────────── */
function buildItineraryPrompt(body: ItineraryBody): string {
  const c = body.constraints;
  const parts: string[] = [];

  parts.push("Create a detailed, personalized day-by-day travel itinerary.\n");

  // Core trip info
  parts.push(`Destination: ${c.destination}`);
  parts.push(`Travel dates: ${c.startDate} to ${c.endDate}`);
  parts.push(`Travelers: ${c.adults ?? 1} adult(s)${c.children ? `, ${c.children} child(ren)` : ""}`);
  parts.push(`Budget level: ${c.budgetLevel ?? "MODERATE"}`);

  // User preferences — all of them
  if (body.diet) parts.push(`Dietary requirements: ${body.diet} — ALL food suggestions MUST respect this.`);
  if (body.interests && body.interests.length > 0) {
    parts.push(`Travel interests: ${body.interests.join(", ")} — prioritize these types of activities.`);
  }
  if (body.preferences?.pacePreference) parts.push(`Pace: ${body.preferences.pacePreference}`);
  if (body.preferences?.mustSeeAttractions?.length) {
    parts.push(`Must-see attractions: ${body.preferences.mustSeeAttractions.join(", ")}`);
  }
  if (body.preferences?.avoidAttractions?.length) {
    parts.push(`Avoid: ${body.preferences.avoidAttractions.join(", ")}`);
  }
  if (body.customNotes) parts.push(`Custom notes: ${body.customNotes}`);
  if (body.userPrompt) parts.push(`User notes: ${body.userPrompt}`);

  parts.push("\n--- INSTRUCTIONS ---");
  parts.push("1. Use REAL place names, restaurants, and attractions that actually exist in " + c.destination + ".");
  parts.push("2. Include specific addresses/locations where possible.");
  parts.push("3. Tailor activities to match the user's interests, budget, and pace.");
  if (body.diet) parts.push(`4. IMPORTANT: All food/restaurant activities MUST be ${body.diet}-friendly.`);
  parts.push("5. Provide realistic cost estimates in EUR (€).");
  parts.push("6. Include 5-8 activities per day with varied categories.");

  parts.push(
    `\nRespond ONLY with valid JSON:\n` +
    `{"tripTitle":"string","summary":"string","totalEstimatedCost":"€XX–€XX",` +
    `"dailyPlan":[{"dayNumber":1,"date":"YYYY-MM-DD","title":"string",` +
    `"activities":[{"time":"HH:mm","duration":60,"title":"string","description":"string",` +
    `"location":"string","category":"SIGHTSEEING|FOOD|TRANSPORT|SHOPPING|CULTURE|NIGHTLIFE|NATURE|WELLNESS",` +
    `"estimatedCost":"€XX","tips":"string","bookingRequired":false}]}],` +
    `"packingTips":["string"],"travelTips":["string"]}`
  );

  return parts.join("\n");
}
