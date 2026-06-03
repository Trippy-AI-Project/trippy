import { NextResponse } from "next/server";
import { buildFallbackItinerary } from "@/lib/server-ai-fallback";
import { errorMessage, postToAiService, readJson } from "@/lib/server-ai-proxy";

export const runtime = "nodejs";

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
  const dateValidationError = validateDateRange(body.constraints.startDate, body.constraints.endDate);
  if (dateValidationError) {
    return NextResponse.json({ error: dateValidationError }, { status: 400 });
  }

  const backendBody = {
    constraints: {
      destination: body.constraints.destination,
      startDate: body.constraints.startDate,
      endDate: body.constraints.endDate,
      budgetLevel: body.constraints.budgetLevel ?? "MODERATE",
      travelers: {
        adults: body.constraints.adults ?? 1,
        children: body.constraints.children ?? 0,
      },
    },
    userPrompt: buildUserPrompt(body),
    tone: body.tone,
    preferences: {
      includeTransport: true,
      includeMeals: true,
      includeAccommodation: true,
      pacePreference: body.preferences?.pacePreference,
      mustSeeAttractions: body.preferences?.mustSeeAttractions ?? [],
      avoidAttractions: body.preferences?.avoidAttractions ?? [],
    },
  };

  try {
    const response = await postToAiService(request, "/ai/itineraries", backendBody, 70_000);
    const data = await readJson(response);

    if (!response.ok) {
      if (shouldUseFallbackForStatus(response.status)) {
        return fallbackItineraryResponse(backendBody, `AI_SERVICE_HTTP_${response.status}`);
      }
      return NextResponse.json(
        { error: errorMessage(data, `AI itinerary request failed (${response.status})`) },
        { status: response.status },
      );
    }

    return NextResponse.json(data);
  } catch (err) {
    console.error("[AI Proxy] Itinerary generation failed:", err);
    return fallbackItineraryResponse(backendBody, "AI_SERVICE_UNAVAILABLE");
  }
}

function buildUserPrompt(body: ItineraryBody): string {
  const parts = [body.userPrompt];
  if (body.interests?.length) parts.push(`Interests: ${body.interests.join(", ")}`);
  if (body.diet) parts.push(`Dietary requirements: ${body.diet}`);
  if (body.customNotes) parts.push(`Custom notes: ${body.customNotes}`);
  return parts.filter(Boolean).join("\n");
}

function fallbackItineraryResponse(
  backendBody: Parameters<typeof buildFallbackItinerary>[0],
  fallbackReason: string,
) {
  try {
    return NextResponse.json(buildFallbackItinerary(backendBody, fallbackReason));
  } catch (fallbackError) {
    console.error("[AI Proxy] Itinerary fallback failed:", fallbackError);
    return NextResponse.json(
      { error: "Could not connect to AI service and fallback data is unavailable." },
      { status: 502 },
    );
  }
}

function shouldUseFallbackForStatus(status: number): boolean {
  return status === 429 || status >= 500;
}

function validateDateRange(startDate?: string, endDate?: string): string | undefined {
  if (!startDate || !endDate) return "startDate and endDate are required";
  const start = Date.parse(`${startDate}T00:00:00.000Z`);
  const end = Date.parse(`${endDate}T00:00:00.000Z`);
  if (Number.isNaN(start) || Number.isNaN(end)) return "startDate and endDate must be valid ISO dates";
  if (end < start) return "endDate must be on or after startDate";
  return undefined;
}
