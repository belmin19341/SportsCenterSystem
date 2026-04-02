package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.ReviewRequestDTO;
import ba.nwt.bookingservice.dto.ReviewResponseDTO;
import ba.nwt.bookingservice.model.Review;
import ba.nwt.bookingservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review management APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get all reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ReviewResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @GetMapping("/reviewer/{reviewerId}")
    @Operation(summary = "Get reviews by reviewer")
    public ResponseEntity<List<ReviewResponseDTO>> getByReviewerId(@PathVariable Long reviewerId) {
        return ResponseEntity.ok(reviewService.getByReviewerId(reviewerId));
    }

    @GetMapping("/entity/{type}/{entityId}")
    @Operation(summary = "Get reviews by entity type and ID")
    public ResponseEntity<List<ReviewResponseDTO>> getByEntity(
            @PathVariable Review.ReviewedEntityType type, @PathVariable Long entityId) {
        return ResponseEntity.ok(reviewService.getByEntity(type, entityId));
    }

    @PostMapping
    @Operation(summary = "Create a new review")
    public ResponseEntity<ReviewResponseDTO> create(@Valid @RequestBody ReviewRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a review")
    public ResponseEntity<ReviewResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ReviewRequestDTO dto) {
        return ResponseEntity.ok(reviewService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

