package pse.trippy.aiservice.service.fallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pse.trippy.aiservice.dto.request.DestinationSuggestionRequest;
import pse.trippy.aiservice.dto.response.DestinationSuggestion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackDestinationCatalogue")
class FallbackDestinationCatalogueTest {

    private FallbackDestinationCatalogue catalogue;

    @BeforeEach
    void setUp() {
        catalogue = new FallbackDestinationCatalogue(new ObjectMapper());
    }

    @Test
    @DisplayName("loads exactly 130 fixed fallback profiles with required scope counts")
    void loadsExpectedProfileCounts() {
        assertThat(catalogue.profiles()).hasSize(130);
        assertThat(catalogue.profilesByScope(FallbackDestinationCatalogue.SCOPE_GERMANY)).hasSize(50);
        assertThat(catalogue.profilesByScope(FallbackDestinationCatalogue.SCOPE_NEARBY_EUROPE)).hasSize(50);
        assertThat(catalogue.profilesByScope(FallbackDestinationCatalogue.SCOPE_WORLDWIDE)).hasSize(30);
    }

    @Test
    @DisplayName("keeps IDs and canonical destination names unique")
    void keepsIdsAndCanonicalNamesUnique() {
        assertThat(catalogue.profiles().stream().map(FallbackDestinationProfile::id).toList())
                .doesNotHaveDuplicates();
        assertThat(catalogue.profiles().stream().map(FallbackDestinationProfile::destination).toList())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("all profiles meet fallback completeness rules")
    void profilesMeetCompletenessRules() {
        for (FallbackDestinationProfile profile : catalogue.profiles()) {
            assertThat(profile.activities())
                    .as(profile.id() + " activity pool")
                    .hasSizeGreaterThanOrEqualTo(Math.max(12, profile.suggestedMinimumActivityPoolSize()));
            assertThat(profile.foodExperiences()).as(profile.id() + " food").hasSizeGreaterThanOrEqualTo(2);
            assertThat(profile.eveningOptions()).as(profile.id() + " evening").hasSizeGreaterThanOrEqualTo(2);
            assertThat(profile.travelTips()).as(profile.id() + " travel tips").hasSizeGreaterThanOrEqualTo(2);
            assertThat(profile.packingTips()).as(profile.id() + " packing tips").hasSizeGreaterThanOrEqualTo(2);
            assertThat(profile.activities().stream()
                    .filter(activity -> "MUST_SEE".equals(activity.priority()))
                    .map(FallbackActivity::title)
                    .toList())
                    .containsAll(profile.anchorHighlights());
        }
    }

    @Test
    @DisplayName("matches required aliases and typo examples")
    void matchesRequiredAliasesAndTypos() {
        assertMatch("Muchen", "Munich, Germany");
        assertMatch("München", "Munich, Germany");
        assertMatch("Muenchen", "Munich, Germany");
        assertMatch("Koln", "Cologne, Germany");
        assertMatch("Koeln", "Cologne, Germany");
        assertMatch("Köln", "Cologne, Germany");
        assertMatch("Bodensee", "Lake Constance, Germany");
        assertMatch("Strasburg", "Strasbourg, France");
        assertMatch("Straßburg", "Strasbourg, France");
        assertMatch("Prauge", "Prague, Czechia");
        assertMatch("Praha", "Prague, Czechia");
        assertMatch("Lisbom", "Lisbon, Portugal");
        assertMatch("Lisboa", "Lisbon, Portugal");
        assertMatch("Neuschwanstain", "Füssen and Neuschwanstein, Germany");
    }

    @Test
    @DisplayName("regex-like input does not crash or match every profile")
    void regexLikeInputDoesNotMatchEverything() {
        assertThat(catalogue.findBestMatch(".*")).isEmpty();

        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, ".*", List.of("culture"), "MODERATE", null, 3, null, null,
                null, null, null, null);
        List<DestinationSuggestion> suggestions = catalogue.suggestDestinations(request, 5);

        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("explicit unsupported destinations do not return unrelated fallback profiles")
    void unsupportedExplicitDestinationReturnsNoSuggestions() {
        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, "Delhi", List.of("culture"), "MODERATE", null, 3, null, null,
                null, null, null, null);

        assertThat(catalogue.suggestDestinations(request, 5)).isEmpty();
    }

    @Test
    @DisplayName("fallback destination suggestions include Google Maps direction URLs")
    void suggestionsIncludeGoogleMapsUrls() {
        DestinationSuggestionRequest request = new DestinationSuggestionRequest(
                null, "Berlin", List.of("culture"), "MODERATE", null, 3, null, null,
                null, null, null, null);

        List<DestinationSuggestion> suggestions = catalogue.suggestDestinations(request, 5);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).googleMapsUrl())
                .startsWith("https://www.google.com/maps/dir/?api=1&destination=");
    }

    private void assertMatch(String query, String expectedDestination) {
        assertThat(catalogue.findBestMatch(query))
                .as(query)
                .hasValueSatisfying(profile -> assertThat(profile.destination()).isEqualTo(expectedDestination));
    }
}
