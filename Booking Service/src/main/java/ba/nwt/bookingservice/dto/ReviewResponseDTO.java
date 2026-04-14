package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.Review;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponseDTO {
    private Long id;
    private Long reviewerId;
    private Long reviewedEntityId;
    private Review.ReviewedEntityType reviewedEntityType;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}

