package pse.trippy.tripservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.tripservice.dto.request.CastVoteRequest;
import pse.trippy.tripservice.dto.request.VotingSettingsRequest;
import pse.trippy.tripservice.dto.response.ActivityVoteSummaryResponse;
import pse.trippy.tripservice.dto.response.VoteSummaryResponse;
import pse.trippy.tripservice.service.VotingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/itinerary")
@RequiredArgsConstructor
public class VotingController {

    private final VotingService votingService;

    @PostMapping("/days/{dayNumber}/vote")
    public ResponseEntity<VoteSummaryResponse> castVote(
            @PathVariable UUID tripId,
            @PathVariable int dayNumber,
            @RequestBody @Valid CastVoteRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        VoteSummaryResponse response = votingService.castVote(tripId, dayNumber, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/days/{dayNumber}/vote")
    public ResponseEntity<VoteSummaryResponse> removeVote(
            @PathVariable UUID tripId,
            @PathVariable int dayNumber,
            @RequestHeader("X-User-Id") UUID userId) {

        VoteSummaryResponse response = votingService.removeVote(tripId, dayNumber, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/days/{dayNumber}/votes")
    public ResponseEntity<VoteSummaryResponse> getVoteSummary(
            @PathVariable UUID tripId,
            @PathVariable int dayNumber,
            @RequestHeader("X-User-Id") UUID userId) {

        VoteSummaryResponse response = votingService.getVoteSummary(tripId, dayNumber, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/voting-settings")
    public ResponseEntity<List<VoteSummaryResponse>> updateVotingSettings(
            @PathVariable UUID tripId,
            @RequestBody @Valid VotingSettingsRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        List<VoteSummaryResponse> response = votingService.updateVotingSettings(tripId, request, userId);
        return ResponseEntity.ok(response);
    }

    // ── Activity-level voting ───────────────────────────────────────────

    @PostMapping("/activities/{activityId}/vote")
    public ResponseEntity<ActivityVoteSummaryResponse> castActivityVote(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @RequestBody @Valid CastVoteRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        ActivityVoteSummaryResponse response = votingService.castActivityVote(tripId, activityId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/activities/{activityId}/vote")
    public ResponseEntity<ActivityVoteSummaryResponse> removeActivityVote(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID userId) {

        ActivityVoteSummaryResponse response = votingService.removeActivityVote(tripId, activityId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activities/{activityId}/votes")
    public ResponseEntity<ActivityVoteSummaryResponse> getActivityVoteSummary(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID userId) {

        ActivityVoteSummaryResponse response = votingService.getActivityVoteSummary(tripId, activityId, userId);
        return ResponseEntity.ok(response);
    }
}
