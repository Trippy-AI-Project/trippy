import { NextResponse } from "next/server";
import { errorMessage, postToAiService, readJson } from "@/lib/server-ai-proxy";

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
      return NextResponse.json(
        { error: errorMessage(data, `AI itinerary request failed (${response.status})`) },
        { status: response.status },
      );
    }

    return NextResponse.json(data);
  } catch (err) {
    console.error("[AI Proxy] Itinerary generation failed:", err);
    return NextResponse.json(
      { error: "Could not connect to AI service. Please make sure the backend is running." },
      { status: 502 },
    );
  }
}

function buildUserPrompt(body: ItineraryBody): string {
  const parts = [body.userPrompt];
  if (body.interests?.length) parts.push(`Interests: ${body.interests.join(", ")}`);
  if (body.diet) parts.push(`Dietary requirements: ${body.diet}`);
  if (body.customNotes) parts.push(`Custom notes: ${body.customNotes}`);
  return parts.filter(Boolean).join("\n");
}
