package pse.trippy.tripservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.tripservice.model.entity.Activity;
import pse.trippy.tripservice.model.entity.ActivityComment;
import pse.trippy.tripservice.repository.ActivityCommentRepository;
import pse.trippy.tripservice.repository.ActivityRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/trips/{tripId}/activities/{activityId}/comments")
@RequiredArgsConstructor
public class ActivityCommentController {

    private final ActivityCommentRepository commentRepository;
    private final ActivityRepository activityRepository;

    public record CreateCommentRequest(
            @NotBlank @Size(max = 1000) String content
    ) {}

    public record CommentResponse(
            String id,
            String activityId,
            String userId,
            String content,
            Instant createdAt
    ) {}

    @GetMapping
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId) {
        log.info("GET /trips/{}/activities/{}/comments", tripId, activityId);
        List<CommentResponse> comments = commentRepository.findByActivityIdOrderByCreatedAtAsc(activityId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateCommentRequest request) {
        log.info("POST /trips/{}/activities/{}/comments — user={}", tripId, activityId, userId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        ActivityComment comment = ActivityComment.builder()
                .activity(activity)
                .userId(userId)
                .content(request.content())
                .build();
        comment = commentRepository.save(comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(comment));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("DELETE /trips/{}/activities/{}/comments/{} — user={}", tripId, activityId, commentId, userId);

        ActivityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        commentRepository.delete(comment);
        return ResponseEntity.noContent().build();
    }

    private CommentResponse toResponse(ActivityComment c) {
        return new CommentResponse(
                c.getId().toString(),
                c.getActivity().getId().toString(),
                c.getUserId().toString(),
                c.getContent(),
                c.getCreatedAt()
        );
    }
}
