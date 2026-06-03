package pse.trippy.aiservice.service.fallback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestion;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FallbackDestinationCatalogue {

    public static final String SCOPE_GERMANY = "GERMANY";
    public static final String SCOPE_NEARBY_EUROPE = "NEARBY_EUROPE_FROM_GERMANY";
    public static final String SCOPE_WORLDWIDE = "WORLDWIDE_HEADLINE";

    private static final List<String> RESOURCE_PATHS = List.of(
            "fallback/destinations-germany.json",
            "fallback/destinations-nearby-europe.json",
            "fallback/destinations-worldwide.json");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int EXPECTED_TOTAL = 130;

    private final List<FallbackDestinationProfile> profiles;

    public FallbackDestinationCatalogue(ObjectMapper objectMapper) {
        this.profiles = List.copyOf(loadProfiles(objectMapper));
        validateCatalogue(this.profiles);
        log.info("Loaded fallback destination catalogue profiles={}", this.profiles.size());
    }

    public List<FallbackDestinationProfile> profiles() {
        return profiles;
    }

    public List<FallbackDestinationProfile> profilesByScope(String scope) {
        return profiles.stream()
                .filter(profile -> Objects.equals(profile.scope(), scope))
                .toList();
    }

    public Optional<FallbackDestinationProfile> findBestMatch(String rawInput) {
        String query = normalize(rawInput);
        if (query.isBlank()) {
            return Optional.empty();
        }

        return profiles.stream()
                .map(profile -> new ProfileMatch(profile, directMatchScore(query, profile)))
                .filter(match -> match.score() >= 0.78)
                .max(Comparator.comparingDouble(ProfileMatch::score))
                .map(ProfileMatch::profile);
    }

    public List<DestinationSuggestion> suggestDestinations(DestinationSuggestionRequest request, int limit) {
        int suggestionLimit = Math.max(1, Math.min(5, limit));
        List<FallbackDestinationProfile> selected = new ArrayList<>();
        Optional<FallbackDestinationProfile> primary = findBestMatch(request.city());

        primary.ifPresent(selected::add);
        rankProfiles(request, primary.map(FallbackDestinationProfile::id).orElse(null)).stream()
                .filter(profile -> selected.stream().noneMatch(existing -> existing.id().equals(profile.id())))
                .limit(suggestionLimit - selected.size())
                .forEach(selected::add);

        return selected.stream()
                .limit(suggestionLimit)
                .map(profile -> toSuggestion(profile, suggestionScore(profile, request, primary
                        .map(FallbackDestinationProfile::id)
                        .orElse(null))))
                .toList();
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String transliterated = value.trim().toLowerCase(Locale.ROOT)
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
                .replace("æ", "ae")
                .replace("ø", "o");
        String decomposed = Normalizer.normalize(transliterated, Normalizer.Form.NFD);
        String withoutMarks = DIACRITICS.matcher(decomposed).replaceAll("");
        String alphanumeric = NON_ALNUM.matcher(withoutMarks).replaceAll(" ");
        return WHITESPACE.matcher(alphanumeric).replaceAll(" ").trim();
    }

    private List<FallbackDestinationProfile> loadProfiles(ObjectMapper objectMapper) {
        List<FallbackDestinationProfile> loaded = new ArrayList<>();
        for (String path : RESOURCE_PATHS) {
            ClassPathResource resource = new ClassPathResource(path);
            try (var inputStream = resource.getInputStream()) {
                List<FallbackDestinationProfile> profilesFromFile = objectMapper.readValue(
                        inputStream,
                        new TypeReference<>() {
                        });
                loaded.addAll(profilesFromFile);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load fallback catalogue resource " + path, ex);
            }
        }
        return loaded;
    }

    private void validateCatalogue(List<FallbackDestinationProfile> loadedProfiles) {
        if (loadedProfiles.size() != EXPECTED_TOTAL) {
            throw new IllegalStateException("Fallback catalogue must contain exactly "
                    + EXPECTED_TOTAL + " profiles, found " + loadedProfiles.size());
        }

        validateScopeCount(loadedProfiles, SCOPE_GERMANY, 50);
        validateScopeCount(loadedProfiles, SCOPE_NEARBY_EUROPE, 50);
        validateScopeCount(loadedProfiles, SCOPE_WORLDWIDE, 30);

        Set<String> ids = new HashSet<>();
        Set<String> destinations = new HashSet<>();
        for (FallbackDestinationProfile profile : loadedProfiles) {
            if (!ids.add(profile.id())) {
                throw new IllegalStateException("Duplicate fallback profile id " + profile.id());
            }
            if (!destinations.add(profile.destination())) {
                throw new IllegalStateException("Duplicate fallback destination " + profile.destination());
            }
            validateProfile(profile);
        }
    }

    private void validateScopeCount(List<FallbackDestinationProfile> loadedProfiles, String scope, int expectedCount) {
        long count = loadedProfiles.stream()
                .filter(profile -> Objects.equals(profile.scope(), scope))
                .count();
        if (count != expectedCount) {
            throw new IllegalStateException("Fallback catalogue scope " + scope
                    + " must contain " + expectedCount + " profiles, found " + count);
        }
    }

    private void validateProfile(FallbackDestinationProfile profile) {
        int requiredActivities = Math.max(12, profile.suggestedMinimumActivityPoolSize());
        if (safeList(profile.activities()).size() < requiredActivities) {
            throw new IllegalStateException(profile.id() + " has fewer than " + requiredActivities + " activities");
        }
        if (safeList(profile.foodExperiences()).size() < 2
                || safeList(profile.eveningOptions()).size() < 2
                || safeList(profile.travelTips()).size() < 2
                || safeList(profile.packingTips()).size() < 2) {
            throw new IllegalStateException(profile.id() + " does not meet fallback profile completeness rules");
        }

        Set<String> mustSeeTitles = new HashSet<>(safeList(profile.activities()).stream()
                .filter(activity -> "MUST_SEE".equals(activity.priority()))
                .map(FallbackActivity::title)
                .toList());
        for (String anchor : safeList(profile.anchorHighlights())) {
            if (!mustSeeTitles.contains(anchor)) {
                throw new IllegalStateException(profile.id() + " is missing MUST_SEE anchor " + anchor);
            }
        }
    }

    private List<FallbackDestinationProfile> rankProfiles(DestinationSuggestionRequest request, String exactProfileId) {
        return profiles.stream()
                .sorted(Comparator
                        .comparingDouble((FallbackDestinationProfile profile) ->
                                suggestionScore(profile, request, exactProfileId))
                        .reversed()
                        .thenComparing(FallbackDestinationProfile::destination))
                .toList();
    }

    private double directMatchScore(String query, FallbackDestinationProfile profile) {
        double bestScore = 0.0;
        String compactQuery = compact(query);
        for (String term : searchableTerms(profile)) {
            String normalizedTerm = normalize(term);
            if (normalizedTerm.isBlank()) {
                continue;
            }
            String compactTerm = compact(normalizedTerm);
            if (query.equals(normalizedTerm) || compactQuery.equals(compactTerm)) {
                return 1.0;
            }
            if (containsMeaningful(query, normalizedTerm) || containsMeaningful(compactQuery, compactTerm)) {
                bestScore = Math.max(bestScore, 0.92);
            }
            bestScore = Math.max(bestScore, fuzzySimilarity(compactQuery, compactTerm));
        }
        return bestScore;
    }

    private List<String> searchableTerms(FallbackDestinationProfile profile) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(profile.destination());
        terms.add(profile.city());
        terms.add(profile.country());
        terms.add(profile.city() + " " + profile.country());
        terms.addAll(safeList(profile.aliases()));
        terms.addAll(safeList(profile.anchorHighlights()));
        return List.copyOf(terms);
    }

    private boolean containsMeaningful(String left, String right) {
        return left.length() >= 4 && right.length() >= 4 && (left.contains(right) || right.contains(left));
    }

    private double suggestionScore(FallbackDestinationProfile profile, DestinationSuggestionRequest request,
                                   String exactProfileId) {
        double score = Objects.equals(profile.id(), exactProfileId) ? 0.97 : 0.52;
        List<String> requestedInterests = safeList(request.interests()).stream()
                .map(FallbackDestinationCatalogue::normalize)
                .filter(value -> !value.isBlank())
                .toList();
        Set<String> profileTags = safeList(profile.tags()).stream()
                .map(FallbackDestinationCatalogue::normalize)
                .collect(java.util.stream.Collectors.toSet());

        for (String interest : requestedInterests) {
            if (profileTags.contains(interest)) {
                score += 0.08;
            }
        }

        String queryText = normalize(String.join(" ",
                safeString(request.prompt()),
                safeString(request.preferences()),
                safeString(request.customNotes()),
                safeString(request.region())));
        if (!queryText.isBlank()) {
            for (String tag : profileTags) {
                if (containsMeaningful(queryText, tag)) {
                    score += 0.04;
                }
            }
            if (containsMeaningful(queryText, normalize(profile.country()))
                    || containsMeaningful(queryText, normalize(profile.city()))) {
                score += 0.05;
            }
        }

        Integer month = parseMonth(request.month());
        if (month != null && safeList(profile.recommendedMonths()).contains(month)) {
            score += 0.04;
        }

        return Math.min(0.99, score);
    }

    private DestinationSuggestion toSuggestion(FallbackDestinationProfile profile, double score) {
        BigDecimal estimatedCost = profile.estimatedDailyCost() == null
                ? BigDecimal.ZERO
                : profile.estimatedDailyCost();
        return new DestinationSuggestion(
                profile.destination(),
                profile.country(),
                profile.description(),
                safeList(profile.highlights()).stream().limit(3).toList(),
                estimatedCost,
                profile.bestTimeToVisit(),
                Math.round(score * 100.0) / 100.0);
    }

    private Integer parseMonth(String rawMonth) {
        if (rawMonth == null || rawMonth.isBlank()) {
            return null;
        }
        String normalized = normalize(rawMonth);
        try {
            int numeric = Integer.parseInt(normalized);
            return numeric >= 1 && numeric <= 12 ? numeric : null;
        } catch (NumberFormatException ignored) {
            // Try month names below.
        }

        List<String> monthNames = List.of(
                "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december");
        int index = monthNames.indexOf(normalized);
        return index >= 0 ? index + 1 : null;
    }

    private double fuzzySimilarity(String left, String right) {
        if (left.length() < 4 || right.length() < 4) {
            return 0.0;
        }
        int distance = levenshtein(left, right);
        int maxLength = Math.max(left.length(), right.length());
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private String compact(String value) {
        return value.replace(" ", "");
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ProfileMatch(FallbackDestinationProfile profile, double score) {
    }
}
