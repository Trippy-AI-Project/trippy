/**
 * Direct Groq API client for Next.js API routes.
 *
 * Calls Groq directly — no Java backend needed.
 * The GROQ_API_KEY env var is read from .env.local or the root .env.
 */

const GROQ_BASE = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_MODEL = "llama-3.3-70b-versatile";

function getApiKey(): string {
  const key = process.env.GROQ_API_KEY;
  if (!key || key === "your_groq_api_key_here") {
    throw new Error("GROQ_API_KEY is not set. Add it to frontend/.env.local");
  }
  return key;
}

export interface GroqMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

/**
 * Call Groq chat completions format.
 */
export async function callGroq(
  messages: GroqMessage[],
  options?: { temperature?: number; maxTokens?: number; timeoutMs?: number }
): Promise<string> {
  const apiKey = getApiKey();
  const { temperature = 0.7, maxTokens = 4096, timeoutMs = 40_000 } = options ?? {};

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(GROQ_BASE, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: GROQ_MODEL,
        messages,
        temperature,
        max_tokens: maxTokens,
      }),
      signal: controller.signal,
    });

    if (!res.ok) {
      const errText = await res.text().catch(() => "");
      throw new Error(`Groq API error (${res.status}): ${errText}`);
    }

    const data = await res.json();
    const content = data?.choices?.[0]?.message?.content;
    if (!content || typeof content !== "string" || content.trim().length === 0) {
      throw new Error("Groq returned empty content");
    }
    return content;
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Extract JSON from LLM output that may include markdown fences or preamble text.
 */
export function extractJson(raw: string): string {
  let trimmed = raw.trim();

  // Strip markdown code fences
  if (trimmed.startsWith("```")) {
    const start = trimmed.indexOf("\n") + 1;
    const end = trimmed.lastIndexOf("```");
    if (end > start) {
      trimmed = trimmed.substring(start, end).trim();
    }
  }

  // Find the first { and last }
  const braceStart = trimmed.indexOf("{");
  const braceEnd = trimmed.lastIndexOf("}");
  if (braceStart >= 0 && braceEnd > braceStart) {
    return trimmed.substring(braceStart, braceEnd + 1);
  }

  return trimmed;
}

