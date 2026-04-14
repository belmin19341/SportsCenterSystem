package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.Review;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewRequestDTO {

    @NotNull(message = "Reviewer ID is required")
    private Long reviewerId;

    @NotNull(message = "Reviewed entity ID is required")
    private Long reviewedEntityId;

    @NotNull(message = "Reviewed entity type is required")
    private Review.ReviewedEntityType reviewedEntityType;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String comment;
}

