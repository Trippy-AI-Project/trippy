import { NextResponse } from "next/server";
import { callGroqWithRetry, extractJson, type GroqError } from "@/lib/groq-client";

/* ── Types ─────────────────────────────────────────────────────────── */
interface SuggestionBody {
  prompt?: string;
  city?: string;
  budget?: string;
  durationDays?: number;
  interests?: string[];
  people?: number;
  diet?: string;
  preferences?: string;
  customNotes?: string;
}

/* ── POST handler ──────────────────────────────────────────────────── */
export async function POST(request: Request) {
  let body: SuggestionBody;

  try {
    body = (await request.json()) as SuggestionBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  try {
    const prompt = buildPrompt(body);

    const result = await callGroqWithRetry(
      [
        {
          role: "system",
          content:
            "You are Trippy AI, an expert travel planner. You create personalized, curated travel suggestions. " +
            "Always respond with valid JSON. Never include markdown code fences. " +
            "Every suggestion must be tailored to the user's specific preferences, budget, dietary needs, and travel style.",
        },
        { role: "user", content: prompt },
      ],
      { maxTokens: 4096, timeoutMs: 30_000 },
    );

    const json = extractJson(result.content);
    let parsed;
    try {
      parsed = JSON.parse(json);
    } catch (parseError) {
      console.error("[AI] Failed to parse JSON. Raw content:", result.content.substring(0, 500) + "...");
      return NextResponse.json(
        { error: "AI returned an invalid format. Please try again." },
        { status: 502 }
      );
    }
    const suggestions = Array.isArray(parsed?.suggestions) ? parsed.suggestions : [];

    if (suggestions.length === 0) {
      return NextResponse.json(
        { error: "AI could not generate suggestions for this query. Try different preferences." },
        { status: 422 }
      );
    }

    console.log(`[AI] Groq returned ${suggestions.length} suggestion(s)`);
    return NextResponse.json({ suggestions, generatedAt: new Date().toISOString(), cached: false });
  } catch (err) {
    const groqErr = err as GroqError;
    if (groqErr.status && groqErr.message) {
      console.error(`[AI] Groq error (${groqErr.status}):`, groqErr.message);
      return NextResponse.json(
        { error: groqErr.message },
        { status: groqErr.status >= 500 ? groqErr.status : 502 }
      );
    }
    console.error("[AI] Unexpected error:", err);
    return NextResponse.json(
      { error: "Something went wrong. Please try again." },
      { status: 500 }
    );
  }
}

/* ── Prompt builder — includes ALL user preferences ────────────────── */
function buildPrompt(body: SuggestionBody): string {
  const city = body.city?.trim();
  const parts: string[] = [];

  parts.push("You are an expert travel planner. Generate personalized travel destination suggestions.\n");

  if (city) {
    parts.push(`The user wants to travel specifically to: ${city}`);
    parts.push(`Focus ONLY on ${city} as the destination. Do NOT suggest other cities.\n`);
  } else if (body.prompt) {
    parts.push(`User request: ${body.prompt}\n`);
  } else {
    parts.push("Suggest 3-5 diverse, interesting travel destinations.\n");
  }

  // All user preferences — structured and clear for the LLM
  if (body.durationDays) parts.push(`Trip duration: ${body.durationDays} days`);
  if (body.people) parts.push(`Number of travelers: ${body.people}`);
  if (body.budget) parts.push(`Budget level: ${body.budget}`);
  if (body.diet) parts.push(`Dietary requirements: ${body.diet}`);
  if (body.interests && body.interests.length > 0) {
    parts.push(`Travel interests/style: ${body.interests.join(", ")}`);
  }
  if (body.preferences) parts.push(`Specific preferences: ${body.preferences}`);
  if (body.customNotes) parts.push(`Additional notes: ${body.customNotes}`);

  parts.push("\n--- INSTRUCTIONS ---");
  parts.push("Tailor every suggestion to match the user's preferences above.");
  if (body.diet) parts.push(`All food/restaurant suggestions must respect: ${body.diet} dietary needs.`);
  if (body.budget) parts.push(`Costs should match a ${body.budget} budget level.`);
  if (body.interests && body.interests.length > 0) {
    parts.push(`Highlights and reasons should emphasize: ${body.interests.join(", ")}.`);
  }

  parts.push(
    `\nRespond ONLY with valid JSON:\n` +
    `{"suggestions":[{"city":"string","country":"string","estimatedDailyCost":"€XX–€XX","bestTimeToVisit":"string","highlights":["string"],"reason":"string","matchScore":0.95}]}\n` +
    `Return ${city ? "1 suggestion for the specified city" : "3-5 suggestions"}. Costs in EUR (€). ` +
    `Each reason must explain WHY this destination matches the user's specific preferences.`
  );

  return parts.join("\n");
}
