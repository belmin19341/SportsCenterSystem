package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewerId(Long reviewerId);
    List<Review> findByReviewedEntityTypeAndReviewedEntityId(Review.ReviewedEntityType type, Long entityId);
}

