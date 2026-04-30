package pse.trippy.tripservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.tripservice.dto.request.UpdateItineraryRequest;
import pse.trippy.tripservice.dto.response.ItineraryResponse;
import pse.trippy.tripservice.service.ItineraryService;

import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @GetMapping
    public ResponseEntity<ItineraryResponse> getItinerary(
            @PathVariable UUID tripId,
            @RequestHeader("X-User-Id") UUID userId) {

        ItineraryResponse response = itineraryService.getItinerary(tripId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ItineraryResponse> updateItinerary(
            @PathVariable UUID tripId,
            @RequestBody @Valid UpdateItineraryRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        ItineraryResponse response = itineraryService.updateItinerary(tripId, request, userId);
        return ResponseEntity.ok(response);
    }
}
