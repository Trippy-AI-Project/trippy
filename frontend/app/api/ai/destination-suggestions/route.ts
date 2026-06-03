import { NextResponse } from "next/server";
import { buildFallbackDestinationSuggestions } from "@/lib/server-ai-fallback";
import { errorMessage, postToAiService, readJson } from "@/lib/server-ai-proxy";

export const runtime = "nodejs";

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

interface BackendSuggestion {
  destination?: string;
  country?: string;
  description?: string;
  highlights?: string[];
  estimatedDailyCost?: number | string;
  bestTimeToVisit?: string;
  googleMapsUrl?: string;
  matchScore?: number;
}

export async function POST(request: Request) {
  let body: SuggestionBody;

  try {
    body = (await request.json()) as SuggestionBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body" }, { status: 400 });
  }

  const backendBody = {
    prompt: body.prompt,
    city: body.city,
    interests: body.interests?.length ? body.interests : ["travel"],
    budget: body.budget,
    travelStyle: body.preferences,
    duration: clampDuration(body.durationDays),
    region: body.city,
    people: body.people,
    diet: body.diet,
    preferences: body.preferences,
    customNotes: body.customNotes,
  };

  try {
    const response = await postToAiService(request, "/ai/destination-suggestions", backendBody, 35_000);
    const data = await readJson(response);

    if (!response.ok) {
      if (shouldUseFallbackForStatus(response.status)) {
        return fallbackSuggestionResponse(body, `AI_SERVICE_HTTP_${response.status}`);
      }
      return NextResponse.json(
        { error: errorMessage(data, `AI service request failed (${response.status})`) },
        { status: response.status },
      );
    }

    const suggestions = Array.isArray(data.suggestions)
      ? data.suggestions.map(normalizeSuggestion)
      : [];

    if (!suggestions.length) {
      return fallbackSuggestionResponse(body, "AI_UNUSABLE_RESPONSE");
    }

    return NextResponse.json({
      suggestions,
      generatedAt: data.generatedAt ?? new Date().toISOString(),
      cached: Boolean(data.cached),
    });
  } catch (err) {
    console.error("[AI Proxy] Destination suggestions failed:", err);
    return fallbackSuggestionResponse(body, "AI_SERVICE_UNAVAILABLE");
  }
}

function clampDuration(durationDays?: number): number {
  if (!durationDays || !Number.isFinite(durationDays)) return 3;
  return Math.min(30, Math.max(1, Math.round(durationDays)));
}

function normalizeSuggestion(suggestion: BackendSuggestion) {
  const destination = suggestion.destination ?? "";
  const city = destination.includes(",")
    ? destination.split(",")[0]?.trim()
    : destination || undefined;

  return {
    city,
    destination,
    country: suggestion.country,
    estimatedDailyCost: formatCost(suggestion.estimatedDailyCost),
    bestTimeToVisit: suggestion.bestTimeToVisit,
    googleMapsUrl: suggestion.googleMapsUrl,
    highlights: suggestion.highlights ?? [],
    reason: suggestion.description,
    matchScore: suggestion.matchScore,
  };
}

function formatCost(cost?: number | string): string | undefined {
  if (cost === undefined || cost === null || cost === "") return undefined;
  if (typeof cost === "number") return `€${Math.round(cost)}`;
  return cost.startsWith("€") ? cost : `€${cost}`;
}

function fallbackSuggestionResponse(body: SuggestionBody, fallbackReason: string) {
  try {
    const fallbackSuggestions = buildFallbackDestinationSuggestions({
      prompt: body.prompt,
      city: body.city,
      interests: body.interests?.length ? body.interests : ["travel"],
      budget: body.budget,
      duration: clampDuration(body.durationDays),
      region: undefined,
      people: body.people,
      diet: body.diet,
      preferences: body.preferences,
      customNotes: body.customNotes,
    });

    if (!fallbackSuggestions.length) {
      return NextResponse.json(
        { error: fallbackUnavailableMessage(body.city) },
        { status: 422 },
      );
    }

    return NextResponse.json({
      suggestions: fallbackSuggestions.map(normalizeSuggestion),
      generatedAt: new Date().toISOString(),
      cached: false,
      fallbackUsed: true,
      fallbackReason,
    });
  } catch (fallbackError) {
    console.error("[AI Proxy] Destination fallback failed:", fallbackError);
    return NextResponse.json(
      { error: "Could not connect to AI service and fallback data is unavailable." },
      { status: 502 },
    );
  }
}

function shouldUseFallbackForStatus(status: number): boolean {
  return status === 429 || status >= 500;
}

function fallbackUnavailableMessage(city?: string): string {
  const destination = city?.trim();
  if (destination) {
    return `Fallback is not available for ${destination}. Please start the AI service or try a supported fallback destination.`;
  }
  return "Could not connect to AI service and fallback data is unavailable.";
}
