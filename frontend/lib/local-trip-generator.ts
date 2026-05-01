/**
 * Local trip data generator — works offline without any external AI API.
 * Uses curated destination data to generate realistic suggestions & itineraries.
 */

interface DestinationData {
  city: string;
  country: string;
  dailyCost: string;
  bestTime: string;
  highlights: string[];
  reason: string;
  activities: ActivityTemplate[];
}

interface ActivityTemplate {
  time: string;
  title: string;
  description: string;
  location: string;
  category: string;
  cost: string;
  duration: number;
}

const DESTINATIONS: DestinationData[] = [
  {
    city: "Paris", country: "France", dailyCost: "€120–180", bestTime: "April–June, September–October",
    highlights: ["Eiffel Tower", "Louvre Museum", "Montmartre", "Seine River Cruise", "Champs-Élysées"],
    reason: "The City of Light offers world-class art, cuisine, and romantic ambiance.",
    activities: [
      { time: "08:00", title: "Croissant & Coffee at Café de Flore", description: "Start your day like a Parisian at this iconic Left Bank café.", location: "172 Bd Saint-Germain, 6th arr.", category: "FOOD", cost: "€15", duration: 60 },
      { time: "09:30", title: "Louvre Museum Visit", description: "See the Mona Lisa and Venus de Milo. Arrive early to beat crowds.", location: "Rue de Rivoli, 1st arr.", category: "CULTURE", cost: "€17", duration: 180 },
      { time: "13:00", title: "Lunch at Le Bouillon Chartier", description: "Affordable traditional French cuisine in a stunning Belle Époque dining room.", location: "7 Rue du Faubourg Montmartre, 9th arr.", category: "FOOD", cost: "€20", duration: 75 },
      { time: "14:30", title: "Stroll through Tuileries Garden", description: "Beautiful formal garden between the Louvre and Place de la Concorde.", location: "Jardin des Tuileries, 1st arr.", category: "NATURE", cost: "€0", duration: 45 },
      { time: "15:30", title: "Eiffel Tower Visit", description: "Take the elevator to the summit for panoramic views of Paris.", location: "Champ de Mars, 7th arr.", category: "SIGHTSEEING", cost: "€26", duration: 120 },
      { time: "18:00", title: "Seine River Cruise", description: "Golden hour cruise past Notre-Dame, Musée d'Orsay, and Grand Palais.", location: "Port de la Bourdonnais, 7th arr.", category: "SIGHTSEEING", cost: "€15", duration: 60 },
      { time: "19:30", title: "Dinner at Le Petit Cler", description: "Cozy bistro on Rue Cler with excellent duck confit and crème brûlée.", location: "29 Rue Cler, 7th arr.", category: "FOOD", cost: "€35", duration: 90 },
    ],
  },
  {
    city: "Tokyo", country: "Japan", dailyCost: "€100–160", bestTime: "March–May, October–November",
    highlights: ["Shibuya Crossing", "Senso-ji Temple", "Tsukiji Market", "Meiji Shrine", "Akihabara"],
    reason: "A perfect blend of ancient traditions and cutting-edge technology.",
    activities: [
      { time: "07:30", title: "Tsukiji Outer Market Breakfast", description: "Fresh sushi and tamagoyaki at the world's most famous fish market area.", location: "Tsukiji 4-chome, Chuo-ku", category: "FOOD", cost: "€15", duration: 75 },
      { time: "09:00", title: "Senso-ji Temple & Nakamise Street", description: "Tokyo's oldest temple with a vibrant shopping street leading to it.", location: "2-3-1 Asakusa, Taito-ku", category: "CULTURE", cost: "€0", duration: 90 },
      { time: "11:00", title: "Tokyo Skytree Observation", description: "360° views from Japan's tallest structure at 634 meters.", location: "1-1-2 Oshiage, Sumida-ku", category: "SIGHTSEEING", cost: "€18", duration: 60 },
      { time: "12:30", title: "Ramen at Ichiran Shibuya", description: "Famous tonkotsu ramen with individual flavor-booth dining.", location: "1-22-7 Jinnan, Shibuya-ku", category: "FOOD", cost: "€12", duration: 45 },
      { time: "14:00", title: "Meiji Shrine & Harajuku", description: "Serene Shinto shrine in a forested park, then quirky Takeshita Street.", location: "1-1 Yoyogikamizonocho, Shibuya-ku", category: "CULTURE", cost: "€0", duration: 120 },
      { time: "16:30", title: "Shibuya Crossing & Shopping", description: "Experience the world's busiest pedestrian crossing, then explore Shibuya 109.", location: "Shibuya Station area", category: "SIGHTSEEING", cost: "€0", duration: 90 },
      { time: "18:30", title: "Dinner at Gonpachi Nishi-Azabu", description: "The restaurant that inspired Kill Bill — excellent yakitori and soba.", location: "1-13-11 Nishi-Azabu, Minato-ku", category: "FOOD", cost: "€30", duration: 90 },
    ],
  },
  {
    city: "Barcelona", country: "Spain", dailyCost: "€90–140", bestTime: "May–June, September–October",
    highlights: ["Sagrada Família", "Park Güell", "La Rambla", "Gothic Quarter", "Beach"],
    reason: "Gaudí's architectural masterpieces meet Mediterranean beaches and vibrant nightlife.",
    activities: [
      { time: "08:30", title: "Breakfast at Federal Café", description: "Excellent brunch spot in the Gothic Quarter with great coffee.", location: "Passatge de la Pau 11, Ciutat Vella", category: "FOOD", cost: "€12", duration: 60 },
      { time: "10:00", title: "Sagrada Família", description: "Gaudí's unfinished masterpiece — book tickets in advance.", location: "C/ de Mallorca 401, Eixample", category: "SIGHTSEEING", cost: "€26", duration: 120 },
      { time: "12:30", title: "Tapas at Bar Cañete", description: "Premium tapas bar on La Rambla — try the Iberian ham and patatas bravas.", location: "C/ de la Unió 17", category: "FOOD", cost: "€25", duration: 75 },
      { time: "14:00", title: "Gothic Quarter Walking Tour", description: "Wander medieval streets, discover hidden squares and Roman ruins.", location: "Barri Gòtic, Ciutat Vella", category: "CULTURE", cost: "€0", duration: 90 },
      { time: "16:00", title: "Park Güell", description: "Gaudí's colorful mosaic park with panoramic city views.", location: "C/ d'Olot, Gràcia", category: "SIGHTSEEING", cost: "€10", duration: 90 },
      { time: "18:00", title: "Barceloneta Beach", description: "Relax on the sand or walk the promenade as the sun sets.", location: "Passeig Marítim, Barceloneta", category: "NATURE", cost: "€0", duration: 90 },
      { time: "20:00", title: "Dinner at Can Paixano", description: "Legendary cava bar with amazing cured meats and sparkling wine.", location: "C/ de la Reina Cristina 7", category: "FOOD", cost: "€20", duration: 90 },
    ],
  },
  {
    city: "Rome", country: "Italy", dailyCost: "€100–150", bestTime: "April–June, September–October",
    highlights: ["Colosseum", "Vatican Museums", "Trevi Fountain", "Pantheon", "Trastevere"],
    reason: "The Eternal City offers ancient history, incredible food, and timeless beauty.",
    activities: [
      { time: "08:00", title: "Espresso & Cornetto at Sant'Eustachio", description: "The best coffee in Rome since 1938 — try their gran caffè.", location: "Piazza di S. Eustachio 82", category: "FOOD", cost: "€5", duration: 30 },
      { time: "09:00", title: "Colosseum & Roman Forum", description: "Explore the iconic amphitheater and ancient civic center. Book skip-the-line.", location: "Piazza del Colosseo", category: "SIGHTSEEING", cost: "€18", duration: 180 },
      { time: "12:30", title: "Lunch at Roscioli", description: "Famous bakery-deli with incredible pasta carbonara and Roman supplies.", location: "Via dei Giubbonari 21", category: "FOOD", cost: "€25", duration: 75 },
      { time: "14:00", title: "Pantheon & Piazza Navona", description: "Marvel at the 2000-year-old dome, then stroll to the baroque square.", location: "Piazza della Rotonda", category: "CULTURE", cost: "€5", duration: 90 },
      { time: "16:00", title: "Trevi Fountain & Spanish Steps", description: "Toss a coin in the fountain, then climb the famous steps.", location: "Piazza di Trevi", category: "SIGHTSEEING", cost: "€0", duration: 60 },
      { time: "17:30", title: "Gelato at Giolitti", description: "Rome's most famous gelateria since 1900 — try pistachio and stracciatella.", location: "Via degli Uffici del Vicario 40", category: "FOOD", cost: "€5", duration: 30 },
      { time: "19:00", title: "Dinner in Trastevere", description: "Atmospheric neighborhood — try cacio e pepe at Da Enzo al 29.", location: "Via dei Vascellari 29, Trastevere", category: "FOOD", cost: "€30", duration: 90 },
    ],
  },
  {
    city: "Delhi", country: "India", dailyCost: "€30–60", bestTime: "October–March",
    highlights: ["Red Fort", "Qutub Minar", "India Gate", "Chandni Chowk", "Humayun's Tomb"],
    reason: "India's capital blends Mughal heritage, street food culture, and modern vibrancy.",
    activities: [
      { time: "08:00", title: "Paranthe Wali Gali Breakfast", description: "Legendary stuffed paratha street in Old Delhi — try aloo and paneer varieties.", location: "Chandni Chowk, Old Delhi", category: "FOOD", cost: "€3", duration: 60 },
      { time: "09:30", title: "Red Fort (Lal Qila)", description: "UNESCO World Heritage Mughal fortress with stunning architecture.", location: "Netaji Subhash Marg, Old Delhi", category: "SIGHTSEEING", cost: "€5", duration: 120 },
      { time: "12:00", title: "Chandni Chowk Market Walk", description: "Vibrant bazaar — explore spice market, silver market, and textile lanes.", location: "Chandni Chowk, Old Delhi", category: "SHOPPING", cost: "€0", duration: 90 },
      { time: "13:30", title: "Lunch at Karim's", description: "Iconic Mughlai restaurant near Jama Masjid since 1913 — try the mutton korma.", location: "16, Gali Kababian, Jama Masjid", category: "FOOD", cost: "€8", duration: 60 },
      { time: "15:00", title: "Humayun's Tomb", description: "Precursor to the Taj Mahal — stunning Mughal garden tomb.", location: "Mathura Road, Nizamuddin", category: "CULTURE", cost: "€5", duration: 90 },
      { time: "17:00", title: "India Gate & Rajpath", description: "War memorial and grand boulevard — perfect for an evening stroll.", location: "Rajpath, New Delhi", category: "SIGHTSEEING", cost: "€0", duration: 60 },
      { time: "19:00", title: "Dinner at Bukhara, ITC Maurya", description: "World-famous North Indian restaurant — iconic dal bukhara and tandoori dishes.", location: "ITC Maurya, Sardar Patel Marg", category: "FOOD", cost: "€25", duration: 90 },
    ],
  },
  {
    city: "Istanbul", country: "Turkey", dailyCost: "€60–100", bestTime: "April–May, September–November",
    highlights: ["Hagia Sophia", "Blue Mosque", "Grand Bazaar", "Bosphorus Cruise", "Topkapi Palace"],
    reason: "Where East meets West — stunning mosques, bazaars, and incredible Turkish cuisine.",
    activities: [
      { time: "08:00", title: "Turkish Breakfast at Van Kahvaltı Evi", description: "Lavish traditional spread with cheeses, honey, eggs, and fresh bread.", location: "Kılıçali Paşa Mah., Beyoğlu", category: "FOOD", cost: "€10", duration: 75 },
      { time: "09:30", title: "Hagia Sophia", description: "6th-century architectural marvel — once a church, then mosque, now mosque again.", location: "Sultanahmet, Fatih", category: "CULTURE", cost: "€0", duration: 90 },
      { time: "11:30", title: "Blue Mosque (Sultan Ahmed)", description: "Iconic mosque with six minarets and stunning blue İznik tiles inside.", location: "Sultanahmet Meydanı, Fatih", category: "SIGHTSEEING", cost: "€0", duration: 60 },
      { time: "13:00", title: "Lunch at Sultanahmet Köftecisi", description: "No-frills spot famous for the best köfte (meatballs) in Istanbul since 1920.", location: "Divanyolu Cd. 12, Sultanahmet", category: "FOOD", cost: "€8", duration: 45 },
      { time: "14:00", title: "Grand Bazaar Shopping", description: "One of the oldest covered markets — 4,000+ shops for carpets, spices, jewelry.", location: "Beyazıt, Fatih", category: "SHOPPING", cost: "€0", duration: 120 },
      { time: "16:30", title: "Bosphorus Strait Cruise", description: "Scenic ferry between Europe and Asia — see palaces and Ottoman mansions.", location: "Eminönü Ferry Terminal", category: "SIGHTSEEING", cost: "€5", duration: 90 },
      { time: "19:00", title: "Dinner at Mikla", description: "Rooftop dining with panoramic views — modern Turkish-Scandinavian fusion.", location: "The Marmara Pera, Beyoğlu", category: "FOOD", cost: "€40", duration: 90 },
    ],
  },
  {
    city: "New York", country: "United States", dailyCost: "€150–250", bestTime: "April–June, September–November",
    highlights: ["Central Park", "Statue of Liberty", "Times Square", "Brooklyn Bridge", "MET Museum"],
    reason: "The city that never sleeps offers unmatched culture, dining, and iconic landmarks.",
    activities: [
      { time: "08:00", title: "Bagel at Russ & Daughters", description: "NYC institution since 1914 — smoked salmon bagel is a must.", location: "179 E Houston St, Lower East Side", category: "FOOD", cost: "€15", duration: 45 },
      { time: "09:30", title: "Statue of Liberty & Ellis Island", description: "Take the ferry to Lady Liberty — book crown tickets in advance.", location: "Battery Park, Lower Manhattan", category: "SIGHTSEEING", cost: "€24", duration: 180 },
      { time: "13:00", title: "Lunch at Joe's Pizza", description: "Classic New York slice since 1975 — cash only, always a line.", location: "7 Carmine St, Greenwich Village", category: "FOOD", cost: "€8", duration: 30 },
      { time: "14:00", title: "Walk the High Line", description: "Elevated park on a former railway with art installations and city views.", location: "Gansevoort St to 34th St, Chelsea", category: "NATURE", cost: "€0", duration: 90 },
      { time: "16:00", title: "Times Square & Broadway", description: "Experience the neon spectacle and grab discounted Broadway tickets at TKTS.", location: "Times Square, Midtown", category: "SIGHTSEEING", cost: "€0", duration: 60 },
      { time: "17:30", title: "Central Park Stroll", description: "Walk through Bethesda Fountain, Bow Bridge, and Strawberry Fields.", location: "Central Park, Manhattan", category: "NATURE", cost: "€0", duration: 90 },
      { time: "19:30", title: "Dinner at Carbone", description: "Italian-American fine dining — try the spicy rigatoni vodka.", location: "181 Thompson St, Greenwich Village", category: "FOOD", cost: "€60", duration: 90 },
    ],
  },
  {
    city: "Bangkok", country: "Thailand", dailyCost: "€40–80", bestTime: "November–February",
    highlights: ["Grand Palace", "Wat Arun", "Chatuchak Market", "Street Food", "Khao San Road"],
    reason: "Temples, street food paradise, and vibrant nightlife at incredible value.",
    activities: [
      { time: "08:00", title: "Breakfast at Or Tor Kor Market", description: "Premium fresh market — try mango sticky rice and fresh coconut juice.", location: "Kamphaeng Phet Rd, Chatuchak", category: "FOOD", cost: "€5", duration: 60 },
      { time: "09:30", title: "Grand Palace & Wat Phra Kaew", description: "Thailand's most sacred temple with the Emerald Buddha. Dress modestly.", location: "Na Phra Lan Rd, Phra Nakhon", category: "SIGHTSEEING", cost: "€15", duration: 120 },
      { time: "12:00", title: "Boat to Wat Arun", description: "Cross the Chao Phraya River to the stunning Temple of Dawn.", location: "158 Wang Doem Rd, Bangkok Yai", category: "CULTURE", cost: "€3", duration: 90 },
      { time: "13:30", title: "Pad Thai at Thip Samai", description: "The most famous pad thai in Bangkok — wrapped in an egg crepe.", location: "313 Maha Chai Rd, Phra Nakhon", category: "FOOD", cost: "€4", duration: 45 },
      { time: "15:00", title: "Chatuchak Weekend Market", description: "15,000+ stalls — clothing, art, antiques, pets, and street snacks.", location: "Kamphaeng Phet 2 Rd, Chatuchak", category: "SHOPPING", cost: "€0", duration: 120 },
      { time: "17:30", title: "Rooftop Drinks at Sky Bar", description: "Iconic open-air rooftop bar from 'The Hangover II' — stunning sunset views.", location: "Lebua State Tower, Silom", category: "NIGHTLIFE", cost: "€15", duration: 60 },
      { time: "19:00", title: "Chinatown Street Food Tour", description: "Yaowarat Road comes alive at night — grilled seafood, dim sum, and mango desserts.", location: "Yaowarat Rd, Samphanthawong", category: "FOOD", cost: "€10", duration: 90 },
    ],
  },
];

/**
 * Build a generic DestinationData for any city/country that isn't in the curated list.
 * Uses the actual destination name so itineraries are correctly branded.
 */
function buildGenericDestination(cityName: string, countryName?: string): DestinationData {
  const city = cityName.trim() || "Your Destination";
  const country = countryName?.trim() || "";
  const label = country ? `${city}, ${country}` : city;

  return {
    city,
    country: country || "—",
    dailyCost: "€80–€120",
    bestTime: "September to November and March to May",
    highlights: [
      `${city} Old Town`,
      `${city} City Center`,
      `Local Markets`,
      `Museums & Galleries`,
      `Parks & Gardens`,
    ],
    reason: `Explore ${label} — discover local culture, cuisine, historic landmarks, and hidden gems.`,
    activities: [
      { time: "08:00", title: `Breakfast at a local café in ${city}`, description: `Start your day with a traditional breakfast in the heart of ${city}.`, location: `${city} City Center`, category: "FOOD", cost: "€12", duration: 60 },
      { time: "09:30", title: `${city} Old Town Walking Tour`, description: `Explore the historic old town, admire the architecture, and learn about local history.`, location: `${city} Old Town`, category: "SIGHTSEEING", cost: "€15", duration: 120 },
      { time: "12:00", title: `Local Market Visit`, description: `Browse the local market for regional specialties, fresh produce, and unique souvenirs.`, location: `${city} Central Market`, category: "SHOPPING", cost: "€0", duration: 60 },
      { time: "13:00", title: `Lunch at a local restaurant`, description: `Enjoy regional cuisine at a popular local restaurant.`, location: `${city} City Center`, category: "FOOD", cost: "€18", duration: 75 },
      { time: "14:30", title: `Museum & Gallery Visit`, description: `Visit a top-rated museum or gallery to learn about the region's art and history.`, location: `${city} Museum Quarter`, category: "CULTURE", cost: "€12", duration: 120 },
      { time: "17:00", title: `Park & Garden Stroll`, description: `Unwind in one of ${city}'s beautiful parks or botanical gardens.`, location: `${city} Main Park`, category: "NATURE", cost: "€0", duration: 60 },
      { time: "19:00", title: `Dinner in ${city}`, description: `End the day with a delicious dinner featuring local specialties.`, location: `${city} Restaurant District`, category: "FOOD", cost: "€30", duration: 90 },
    ],
  };
}

/**
 * Try to extract a specific city name from the search query/prompt.
 * Looks for patterns like "to Stuttgart", "for Stuttgart", "trip to Stuttgart, Germany", etc.
 */
function extractCityFromPrompt(query: string): { city: string; country: string } | null {
  // Try "Plan a trip specifically to <City>" pattern first (from promptPreview)
  const specificMatch = query.match(/specifically to\s+([^.]+)/i);
  if (specificMatch) {
    const parts = specificMatch[1].trim().split(",").map(s => s.trim());
    return { city: parts[0], country: parts[1] || "" };
  }

  // Try "Plan a trip for <City>" pattern
  const forMatch = query.match(/trip for\s+([^.]+)/i);
  if (forMatch) {
    const raw = forMatch[1].trim().replace(/\[.*?\]/g, "").trim();
    if (raw && raw.length > 1 && !raw.startsWith("[")) {
      const parts = raw.split(",").map(s => s.trim());
      return { city: parts[0], country: parts[1] || "" };
    }
  }

  return null;
}

function findBestMatch(query: string): DestinationData[] {
  const q = query.toLowerCase();

  // 1. Check curated destinations first
  const exact = DESTINATIONS.filter(d => q.includes(d.city.toLowerCase()) || q.includes(d.country.toLowerCase()));
  if (exact.length > 0) return exact;

  // 2. Check keywords for theme-based matching
  const keywords: Record<string, string[]> = {
    beach: ["Bangkok", "Barcelona"],
    adventure: ["Tokyo", "Istanbul"],
    romantic: ["Paris", "Rome"],
    food: ["Tokyo", "Bangkok", "Delhi"],
    culture: ["Rome", "Istanbul", "Delhi"],
    budget: ["Bangkok", "Delhi", "Istanbul"],
    city: ["New York", "Tokyo", "Paris"],
    family: ["Barcelona", "Rome", "Paris"],
    nature: ["Barcelona", "Bangkok"],
  };

  // Only use keyword matching if there's no specific city in the query
  const extracted = extractCityFromPrompt(query);
  if (!extracted) {
    for (const [kw, cities] of Object.entries(keywords)) {
      if (q.includes(kw)) {
        const matches = DESTINATIONS.filter(d => cities.includes(d.city));
        if (matches.length) return matches;
      }
    }
  }

  // 3. If a specific city was requested but isn't in our curated list,
  //    generate a generic destination for that city instead of returning Paris
  if (extracted && extracted.city) {
    return [buildGenericDestination(extracted.city, extracted.country)];
  }

  // 4. No specific city and no keyword match — return 3 random diverse destinations
  const shuffled = [...DESTINATIONS].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, 3);
}

export function generateSuggestions(prompt: string, interests?: string[]): {
  suggestions: Array<{
    city: string; country: string; estimatedDailyCost: string;
    bestTimeToVisit: string; highlights: string[]; reason: string; matchScore: number;
  }>;
  generatedAt: string;
} {
  const searchQuery = [prompt, ...(interests || [])].join(" ");
  const matches = findBestMatch(searchQuery);

  return {
    suggestions: matches.map((d, i) => ({
      city: d.city,
      country: d.country,
      estimatedDailyCost: d.dailyCost,
      bestTimeToVisit: d.bestTime,
      highlights: d.highlights,
      reason: d.reason,
      matchScore: Math.round((0.95 - i * 0.05) * 100) / 100,
    })),
    generatedAt: new Date().toISOString(),
  };
}

export function generateItinerary(
  destination: string, startDate: string, endDate: string,
  adults: number, budgetLevel: string, userPrompt?: string,
) {
  const days = Math.max(1, Math.ceil((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000) + 1);
  const q = destination.toLowerCase();

  // Find curated match, or generate a destination-specific placeholder
  let match = DESTINATIONS.find(d => q.includes(d.city.toLowerCase()));
  if (!match) {
    // Parse "Stuttgart, Germany" format from the destination string
    const parts = destination.split(",").map(s => s.trim());
    match = buildGenericDestination(parts[0] || destination, parts[1]);
  }

  const activities = match.activities;

  const dailyPlan = Array.from({ length: days }, (_, i) => {
    const date = new Date(startDate);
    date.setDate(date.getDate() + i);
    // Rotate and shuffle activities slightly per day
    const dayActivities = activities.map((a, idx) => ({
      ...a,
      time: a.time,
      title: i === 0 ? a.title : `${a.title}${idx === 0 ? " (Day " + (i + 1) + ")" : ""}`,
    }));
    const themes = ["Historic Landmarks & Local Cuisine", "Cultural Immersion & Hidden Gems", "Markets, Food & Scenic Views", "Art, Architecture & Evening Delights", "Nature, Relaxation & Farewell Feast"];
    return {
      dayNumber: i + 1,
      date: date.toISOString().slice(0, 10),
      title: themes[i % themes.length],
      activities: dayActivities.map(a => ({
        time: a.time, duration: a.duration, title: a.title,
        description: a.description, location: a.location,
        category: a.category, estimatedCost: a.cost,
        tips: "", bookingRequired: false,
      })),
    };
  });

  return {
    tripTitle: `${match.city} — ${days}-Day Adventure`,
    summary: `A curated ${days}-day trip to ${match.city}, ${match.country} for ${adults} traveler(s). ${match.reason}`,
    totalEstimatedCost: `€${days * 120}–€${days * 180}`,
    dailyPlan,
    packingTips: ["Comfortable walking shoes", "Light layers", "Universal power adapter", "Reusable water bottle"],
    travelTips: ["Book major attractions in advance", "Learn a few local phrases", "Use public transport"],
    generatedAt: new Date().toISOString(),
  };
}
