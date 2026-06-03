import { randomUUID } from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const FALLBACK_FILES = [
  "destinations-germany.json",
  "destinations-nearby-europe.json",
  "destinations-worldwide.json",
];
const ACTIVITY_TIMES = ["09:00", "10:45", "12:45", "15:00", "17:30", "19:30"];

interface FallbackActivity {
  title: string;
  description: string;
  location: string;
  category: string;
  priority: string;
  minimumTripDays: number;
  durationMinutes: number;
  estimatedCost: string;
  preferredTime: string;
  bookingRecommended: boolean;
  suitableInRain: boolean;
}

interface FallbackProfile {
  id: string;
  destination: string;
  city: string;
  country: string;
  aliases: string[];
  tags: string[];
  anchorHighlights: string[];
  description: string;
  highlights: string[];
  estimatedDailyCost: number;
  bestTimeToVisit: string;
  recommendedMonths: number[];
  activities: FallbackActivity[];
  foodExperiences: FallbackActivity[];
  eveningOptions: FallbackActivity[];
  dayTrips: FallbackActivity[];
  travelTips: string[];
  packingTips: string[];
}

export interface FallbackSuggestionRequest {
  prompt?: string;
  city?: string;
  interests?: string[];
  budget?: string;
  duration?: number;
  region?: string;
  month?: string;
  people?: number;
  diet?: string;
  preferences?: string;
  customNotes?: string;
}

export interface FallbackItineraryRequest {
  constraints: {
    destination: string;
    startDate: string;
    endDate: string;
    budgetLevel?: string;
    travelers?: {
      adults: number;
      children: number;
    };
  };
  userPrompt?: string;
  tone?: string;
  preferences?: {
    includeTransport: boolean;
    includeMeals: boolean;
    includeAccommodation: boolean;
    pacePreference?: string;
    mustSeeAttractions: string[];
    avoidAttractions: string[];
  };
}

interface FallbackItineraryActivity {
  time: string;
  durationMinutes: number;
  title: string;
  description: string;
  location: string;
  category: string;
  estimatedCost: string;
  tips: string;
  bookingRequired: boolean;
}

let cachedProfiles: FallbackProfile[] | null = null;

export function buildFallbackDestinationSuggestions(
  request: FallbackSuggestionRequest,
  limit = 5,
) {
  const profiles = loadFallbackProfiles();
  const selected: FallbackProfile[] = [];
  const primary = request.city ? findBestProfile(profiles, request.city) : undefined;
  if (primary) selected.push(primary);

  profiles
    .map((profile) => ({
      profile,
      score: suggestionScore(profile, request, primary?.id),
    }))
    .sort((left, right) => right.score - left.score || left.profile.destination.localeCompare(right.profile.destination))
    .forEach(({ profile }) => {
      if (selected.length >= limit) return;
      if (!selected.some((existing) => existing.id === profile.id)) selected.push(profile);
    });

  return selected.slice(0, limit).map((profile) => ({
    destination: profile.destination,
    country: profile.country,
    description: profile.description,
    highlights: profile.highlights.slice(0, 3),
    estimatedDailyCost: profile.estimatedDailyCost,
    bestTimeToVisit: profile.bestTimeToVisit,
    matchScore: roundScore(suggestionScore(profile, request, primary?.id)),
  }));
}

export function buildFallbackItinerary(
  request: FallbackItineraryRequest,
  fallbackReason: string,
) {
  const profiles = loadFallbackProfiles();
  const profile = findBestProfile(profiles, request.constraints.destination);
  const startDate = parseDate(request.constraints.startDate);
  const endDate = parseDate(request.constraints.endDate);
  if (!startDate || !endDate || endDate.getTime() < startDate.getTime()) {
    throw new Error("Invalid itinerary date range");
  }

  if (!profile) {
    return buildUnsupportedFallback(request, startDate, endDate, fallbackReason);
  }

  const dayCount = daysBetween(startDate, endDate);
  const preferences = request.preferences;
  const includeMeals = preferences?.includeMeals ?? true;
  const usedTitles = new Set<string>();
  const pool = prioritizedActivities(profile, dayCount, preferences);
  const dailyPlan = Array.from({ length: dayCount }, (_, index) => {
    const dayNumber = index + 1;
    const activities: FallbackItineraryActivity[] = [];
    const dayTripDay = dayNumber === 5 && dayCount >= 5 && profile.dayTrips.length > 0;

    if (dayTripDay) {
      addActivity(activities, profile.dayTrips[0], usedTitles);
    } else {
      const targetCount = targetCoreActivityCount(preferences?.pacePreference, dayNumber, dayCount);
      while (coreActivityCount(activities) < targetCount) {
        const next = nextActivity(pool, usedTitles, dayNumber);
        if (!next) {
          activities.push(flexibleActivity(profile, dayNumber, activities.length));
          break;
        }
        addActivity(activities, next, usedTitles);
      }
    }

    if (includeMeals) {
      const food = firstAvailable(profile.foodExperiences, preferences, usedTitles);
      if (food) addActivity(activities, food, usedTitles);
    }

    if (shouldAddEvening(dayNumber, dayCount, preferences?.pacePreference)) {
      const evening = firstAvailable(profile.eveningOptions, preferences, usedTitles);
      if (evening) addActivity(activities, evening, usedTitles);
    }

    activities.forEach((activity, activityIndex) => {
      activity.time = ACTIVITY_TIMES[Math.min(activityIndex, ACTIVITY_TIMES.length - 1)];
    });

    return {
      dayNumber,
      date: formatDate(addDays(startDate, index)),
      title: dayTitle(profile, dayNumber, dayCount, dayTripDay),
      weather: unavailableWeather(),
      transportRecommendations: [],
      activities,
    };
  });

  return {
    generationId: randomUUID(),
    tripTitle: `${dayCount} Days in ${profile.destination}`,
    summary: `Fallback itinerary generated from Trippy's static destination catalogue for ${profile.destination}.`,
    totalEstimatedCost: `Approx. €${profile.estimatedDailyCost * dayCount} daily local cost estimate, excluding flights and accommodation`,
    dailyPlan,
    packingTips: profile.packingTips,
    travelTips: profile.travelTips,
    generatedAt: new Date().toISOString(),
    tokensUsed: 0,
    fallbackUsed: true,
    fallbackReason,
  };
}

function loadFallbackProfiles(): FallbackProfile[] {
  if (cachedProfiles) return cachedProfiles;

  const fallbackDir = path.resolve(process.cwd(), "../services/ai-service/src/main/resources/fallback");
  cachedProfiles = FALLBACK_FILES.flatMap((fileName) => {
    const filePath = path.join(fallbackDir, fileName);
    const raw = fs.readFileSync(filePath, "utf8");
    return JSON.parse(raw) as FallbackProfile[];
  });
  return cachedProfiles;
}

function findBestProfile(profiles: FallbackProfile[], rawInput: string): FallbackProfile | undefined {
  const query = normalize(rawInput);
  if (!query) return undefined;
  const compactQuery = compact(query);

  let best: { profile: FallbackProfile; score: number } | undefined;
  for (const profile of profiles) {
    const score = searchableTerms(profile).reduce((bestScore, term) => {
      const normalizedTerm = normalize(term);
      if (!normalizedTerm) return bestScore;
      const compactTerm = compact(normalizedTerm);
      if (query === normalizedTerm || compactQuery === compactTerm) return 1;
      if (containsMeaningful(query, normalizedTerm) || containsMeaningful(compactQuery, compactTerm)) {
        return Math.max(bestScore, 0.92);
      }
      return Math.max(bestScore, fuzzySimilarity(compactQuery, compactTerm));
    }, 0);

    if (score >= 0.78 && (!best || score > best.score)) best = { profile, score };
  }
  return best?.profile;
}

function searchableTerms(profile: FallbackProfile): string[] {
  return [
    profile.destination,
    profile.city,
    `${profile.city} ${profile.country}`,
    ...profile.aliases,
    ...profile.anchorHighlights,
  ];
}

function prioritizedActivities(
  profile: FallbackProfile,
  dayCount: number,
  preferences?: FallbackItineraryRequest["preferences"],
): FallbackActivity[] {
  const requested = profile.activities.filter((activity) =>
    isRequested(activity.title, preferences) && !isAvoided(activity.title, preferences),
  );
  const mustSee = profile.activities
    .filter((activity) => activity.priority === "MUST_SEE")
    .filter((activity) => dayCount > 1 || !isDayTripActivity(activity))
    .filter((activity) => !isAvoided(activity.title, preferences));
  const recommended = profile.activities
    .filter((activity) => activity.priority !== "MUST_SEE")
    .filter((activity) => dayCount > 1 || !isDayTripActivity(activity))
    .filter((activity) => !isAvoided(activity.title, preferences))
    .sort((left, right) => left.minimumTripDays - right.minimumTripDays || left.title.localeCompare(right.title));

  return uniqueActivities([...requested, ...mustSee, ...recommended]);
}

function nextActivity(
  pool: FallbackActivity[],
  usedTitles: Set<string>,
  dayNumber: number,
): FallbackActivity | undefined {
  return (
    pool.find((activity) => !usedTitles.has(normalize(activity.title)) && activity.minimumTripDays <= dayNumber) ??
    pool.find((activity) => !usedTitles.has(normalize(activity.title)))
  );
}

function firstAvailable(
  pool: FallbackActivity[],
  preferences: FallbackItineraryRequest["preferences"] | undefined,
  usedTitles: Set<string>,
): FallbackActivity | undefined {
  return pool.find((activity) => !usedTitles.has(normalize(activity.title)) && !isAvoided(activity.title, preferences));
}

function addActivity(
  activities: FallbackItineraryActivity[],
  activity: FallbackActivity,
  usedTitles: Set<string>,
) {
  usedTitles.add(normalize(activity.title));
  activities.push({
    time: "",
    durationMinutes: activity.durationMinutes,
    title: activity.title,
    description: activity.description,
    location: activity.location,
    category: activity.category,
    estimatedCost: activity.estimatedCost,
    tips: activity.bookingRecommended
      ? "Booking may be useful; confirm current availability before departure."
      : activity.suitableInRain
        ? "Works as a weather-safe fallback if outdoor plans change."
        : "Keep timing flexible and check current local conditions.",
    bookingRequired: activity.bookingRecommended,
  });
}

function flexibleActivity(profile: FallbackProfile, dayNumber: number, activityIndex: number): FallbackItineraryActivity {
  return {
    time: ACTIVITY_TIMES[Math.min(activityIndex, ACTIVITY_TIMES.length - 1)],
    durationMinutes: 120,
    title: `Slow exploration block in ${profile.city} - Day ${dayNumber}`,
    description: "Use this lower-pressure block for a neighbourhood walk, cafe pause, rest or weather-safe indoor alternative.",
    location: profile.city,
    category: "FREE_TIME",
    estimatedCost: "Flexible",
    tips: "Keep this block adaptable instead of forcing another landmark.",
    bookingRequired: false,
  };
}

function buildUnsupportedFallback(
  request: FallbackItineraryRequest,
  startDate: Date,
  endDate: Date,
  fallbackReason: string,
) {
  const dayCount = daysBetween(startDate, endDate);
  const destination = request.constraints.destination;
  return {
    generationId: randomUUID(),
    tripTitle: `${dayCount} Days in ${destination}`,
    summary: "Fallback generated because AI was unavailable, but this destination is not in the static fallback catalogue.",
    totalEstimatedCost: "Estimate unavailable",
    dailyPlan: Array.from({ length: dayCount }, (_, index) => ({
      dayNumber: index + 1,
      date: formatDate(addDays(startDate, index)),
      title: `Day ${index + 1} in ${destination}`,
      weather: unavailableWeather(),
      transportRecommendations: [],
      activities: [
        {
          time: "09:30",
          durationMinutes: 120,
          title: `Flexible orientation in ${destination}`,
          description: "Start with a central, well-reviewed area and regenerate when AI is reachable.",
          location: destination,
          category: "FREE_TIME",
          estimatedCost: "Varies",
          tips: "Use this as a temporary plan.",
          bookingRequired: false,
        },
      ],
    })),
    packingTips: ["Comfortable walking shoes", "Weather-appropriate layers"],
    travelTips: [
      "Confirm current opening days and tickets before visiting major attractions.",
      "Use this as a temporary plan and regenerate when AI is reachable.",
    ],
    generatedAt: new Date().toISOString(),
    tokensUsed: 0,
    fallbackUsed: true,
    fallbackReason,
  };
}

function suggestionScore(
  profile: FallbackProfile,
  request: FallbackSuggestionRequest,
  exactProfileId?: string,
): number {
  let score = profile.id === exactProfileId ? 0.97 : 0.52;
  const profileTags = new Set(profile.tags.map(normalize));
  for (const interest of request.interests ?? []) {
    if (profileTags.has(normalize(interest))) score += 0.08;
  }

  const query = normalize([
    request.prompt,
    request.preferences,
    request.customNotes,
    request.region,
  ].filter(Boolean).join(" "));
  for (const tag of profileTags) {
    if (containsMeaningful(query, tag)) score += 0.04;
  }
  if (containsMeaningful(query, normalize(profile.city)) || containsMeaningful(query, normalize(profile.country))) {
    score += 0.05;
  }

  const month = parseMonth(request.month);
  if (month && profile.recommendedMonths.includes(month)) score += 0.04;
  return Math.min(0.99, score);
}

function targetCoreActivityCount(pacePreference: string | undefined, dayNumber: number, dayCount: number): number {
  const pace = pacePreference?.trim().toUpperCase() ?? "MODERATE";
  if (pace === "SLOW") return dayCount >= 8 ? 1 : 2;
  if (pace === "PACKED") return dayNumber === 1 ? 4 : 5;
  return dayCount >= 8 ? 2 : 3;
}

function shouldAddEvening(dayNumber: number, dayCount: number, pacePreference?: string): boolean {
  if (dayCount < 3) return false;
  const pace = pacePreference?.trim().toUpperCase() ?? "MODERATE";
  if (pace === "PACKED") return dayNumber % 2 === 1;
  if (pace === "SLOW") return dayNumber === 3 || dayNumber === dayCount;
  return dayNumber === 3 || dayNumber === dayCount || dayNumber % 4 === 0;
}

function coreActivityCount(activities: FallbackItineraryActivity[]): number {
  return activities.filter((activity) => activity.category !== "FOOD" && activity.category !== "NIGHTLIFE").length;
}

function dayTitle(profile: FallbackProfile, dayNumber: number, dayCount: number, dayTripDay: boolean): string {
  if (dayTripDay) return `Day ${dayNumber}: Regional day trip from ${profile.city}`;
  if (dayNumber === 1) return `Day 1: ${profile.city} essentials`;
  if (dayCount >= 8 && dayNumber > 7) return `Day ${dayNumber}: Slower local exploration`;
  return `Day ${dayNumber}: ${profile.city} at a comfortable pace`;
}

function unavailableWeather() {
  return {
    condition: "Forecast unavailable",
    temperatureCelsius: null,
    advice: "Check the local forecast closer to departure.",
  };
}

function isRequested(title: string, preferences?: FallbackItineraryRequest["preferences"]): boolean {
  const normalizedTitle = normalize(title);
  return (preferences?.mustSeeAttractions ?? [])
    .map(normalize)
    .some((value) => value && (normalizedTitle.includes(value) || value.includes(normalizedTitle)));
}

function isAvoided(title: string, preferences?: FallbackItineraryRequest["preferences"]): boolean {
  const normalizedTitle = normalize(title);
  return (preferences?.avoidAttractions ?? [])
    .map(normalize)
    .some((value) => value && (normalizedTitle.includes(value) || value.includes(normalizedTitle)));
}

function isDayTripActivity(activity: FallbackActivity): boolean {
  return activity.title.toLowerCase().includes("day trip") || activity.preferredTime === "FULL_DAY" || activity.durationMinutes >= 360;
}

function uniqueActivities(activities: FallbackActivity[]): FallbackActivity[] {
  const seen = new Set<string>();
  return activities.filter((activity) => {
    const key = normalize(activity.title);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function parseDate(value: string): Date | undefined {
  const date = new Date(`${value}T00:00:00.000Z`);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

function daysBetween(startDate: Date, endDate: Date): number {
  return Math.floor((endDate.getTime() - startDate.getTime()) / 86_400_000) + 1;
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86_400_000);
}

function formatDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function normalize(value: string | undefined): string {
  if (!value) return "";
  return value
    .trim()
    .toLowerCase()
    .replaceAll("ä", "ae")
    .replaceAll("ö", "oe")
    .replaceAll("ü", "ue")
    .replaceAll("ß", "ss")
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .replace(/[^\p{Letter}\p{Number}]+/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function compact(value: string): string {
  return value.replaceAll(" ", "");
}

function containsMeaningful(left: string, right: string): boolean {
  return left.length >= 4 && right.length >= 4 && (left.includes(right) || right.includes(left));
}

function fuzzySimilarity(left: string, right: string): number {
  if (left.length < 4 || right.length < 4) return 0;
  const distance = levenshtein(left, right);
  return 1 - distance / Math.max(left.length, right.length);
}

function levenshtein(left: string, right: string): number {
  let previous = Array.from({ length: right.length + 1 }, (_, index) => index);
  let current = new Array(right.length + 1).fill(0);
  for (let i = 1; i <= left.length; i += 1) {
    current[0] = i;
    for (let j = 1; j <= right.length; j += 1) {
      const cost = left[i - 1] === right[j - 1] ? 0 : 1;
      current[j] = Math.min(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost);
    }
    [previous, current] = [current, previous];
  }
  return previous[right.length];
}

function parseMonth(rawMonth?: string): number | undefined {
  if (!rawMonth) return undefined;
  const normalized = normalize(rawMonth);
  const numeric = Number.parseInt(normalized, 10);
  if (Number.isInteger(numeric) && numeric >= 1 && numeric <= 12) return numeric;
  const index = [
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december",
  ].indexOf(normalized);
  return index >= 0 ? index + 1 : undefined;
}

function roundScore(score: number): number {
  return Math.round(score * 100) / 100;
}
