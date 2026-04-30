/**
 * Shared Groq API client with automatic retry + backoff for rate limits (429).
 *
 * Groq free-tier limits:
 *   - 30 requests/min, 14 400 tokens/min for llama-3.3-70b-versatile
 *
 * This helper retries up to 3 times with exponential backoff when rate-limited.
 */

const GROQ_BASE = "https://api.groq.com/openai/v1/chat/completions";
const MAX_RETRIES = 3;

export interface GroqMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export interface GroqOptions {
  model?: string;
  temperature?: number;
  maxTokens?: number;
  timeoutMs?: number;
}

export interface GroqResult {
  content: string;
}

export interface GroqError {
  status: number;
  message: string;
  retryable: boolean;
}

/**
 * Call Groq with automatic retry on 429 rate limits.
 * Returns either a successful result or throws a descriptive error.
 */
export async function callGroqWithRetry(
  messages: GroqMessage[],
  options?: GroqOptions,
): Promise<GroqResult> {
  const apiKey = process.env.GROQ_API_KEY;
  if (!apiKey || apiKey === "your_groq_api_key_here") {
    throw makeError(503, "AI service is not configured. Please set GROQ_API_KEY in your .env.local file.", false);
  }

  const {
    model = "llama-3.3-70b-versatile",
    temperature = 0.7,
    maxTokens = 4096,
    timeoutMs = 45_000,
  } = options ?? {};

  let lastError: GroqError | null = null;

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
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
          model,
          messages,
          temperature,
          max_tokens: maxTokens,
        }),
        signal: controller.signal,
      });

      // Rate limited — retry with backoff
      if (res.status === 429) {
        const retryAfter = parseRetryAfter(res.headers.get("retry-after"));
        const backoff = retryAfter ?? Math.min(2000 * Math.pow(2, attempt), 15_000);
        console.warn(`[Groq] Rate limited (429). Retry ${attempt + 1}/${MAX_RETRIES} in ${backoff}ms`);

        if (attempt < MAX_RETRIES) {
          await sleep(backoff);
          continue;
        }

        // Final attempt also rate limited
        lastError = makeError(429,
          "AI service is temporarily busy (rate limited). Please wait a few seconds and try again.",
          true,
        );
        break;
      }

      // Server error (500/502/503) — retry
      if (res.status >= 500 && attempt < MAX_RETRIES) {
        const backoff = 1000 * Math.pow(2, attempt);
        console.warn(`[Groq] Server error (${res.status}). Retry ${attempt + 1}/${MAX_RETRIES} in ${backoff}ms`);
        await sleep(backoff);
        continue;
      }

      // Other non-OK status
      if (!res.ok) {
        const errText = await res.text().catch(() => "");
        console.error(`[Groq] API error (${res.status}):`, errText);
        throw makeError(res.status, `AI service error (${res.status}). Please try again.`, false);
      }

      // Success
      const data = await res.json();
      const content = data?.choices?.[0]?.message?.content;
      if (!content || typeof content !== "string" || content.trim().length === 0) {
        throw makeError(502, "AI returned an empty response. Please try again.", true);
      }

      return { content };
    } catch (err) {
      if ((err as GroqError).status) throw err; // Already a GroqError, re-throw

      if (err instanceof Error && err.name === "AbortError") {
        throw makeError(504, "AI request timed out. Please try again with a simpler query.", false);
      }
      console.error(`[Groq] Fetch error (attempt ${attempt + 1}):`, err instanceof Error ? err.message : err);
      lastError = makeError(503, "Could not connect to AI service. Check your internet connection.", true);

      if (attempt < MAX_RETRIES) {
        await sleep(1000 * Math.pow(2, attempt));
        continue;
      }
    } finally {
      clearTimeout(timer);
    }
  }

  throw lastError ?? makeError(503, "AI service is unavailable. Please try again later.", false);
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

  // Find the first { or [
  const braceStart = trimmed.indexOf("{");
  const bracketStart = trimmed.indexOf("[");
  let startIdx = -1;
  if (braceStart >= 0 && bracketStart >= 0) {
    startIdx = Math.min(braceStart, bracketStart);
  } else if (braceStart >= 0) {
    startIdx = braceStart;
  } else if (bracketStart >= 0) {
    startIdx = bracketStart;
  }

  // Find the last } or ]
  const braceEnd = trimmed.lastIndexOf("}");
  const bracketEnd = trimmed.lastIndexOf("]");
  let endIdx = -1;
  if (braceEnd >= 0 && bracketEnd >= 0) {
    endIdx = Math.max(braceEnd, bracketEnd);
  } else if (braceEnd >= 0) {
    endIdx = braceEnd;
  } else if (bracketEnd >= 0) {
    endIdx = bracketEnd;
  }

  if (startIdx >= 0 && endIdx > startIdx) {
    return trimmed.substring(startIdx, endIdx + 1);
  }

  return trimmed;
}

/* ── Helpers ───────────────────────────────────────────────────────── */

function makeError(status: number, message: string, retryable: boolean): GroqError {
  return { status, message, retryable };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Parse the Retry-After header (seconds or HTTP-date).
 * Returns milliseconds to wait, or null if unparseable.
 */
function parseRetryAfter(header: string | null): number | null {
  if (!header) return null;
  const seconds = Number(header);
  if (!isNaN(seconds) && seconds > 0) {
    return Math.ceil(seconds * 1000);
  }
  // HTTP-date format
  const date = new Date(header);
  if (!isNaN(date.getTime())) {
    return Math.max(0, date.getTime() - Date.now());
  }
  return null;
}
