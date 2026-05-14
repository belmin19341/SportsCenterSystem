package ba.nwt.bookingservice.integration;

import ba.nwt.bookingservice.dto.ReviewRequestDTO;
import ba.nwt.bookingservice.dto.ReviewResponseDTO;
import ba.nwt.bookingservice.model.Review;
import ba.nwt.bookingservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Review CRUD operations.
 * Review has no Feign dependencies — full HTTP + H2 stack, no mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReviewCrudIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void cleanUp() {
        reviewRepository.deleteAll();
    }

    private ReviewRequestDTO buildRequest(Long reviewerId, Long entityId,
                                          Review.ReviewedEntityType type, int rating) {
        return ReviewRequestDTO.builder()
                .reviewerId(reviewerId)
                .reviewedEntityId(entityId)
                .reviewedEntityType(type)
                .rating(rating)
                .comment("Test comment rating=" + rating)
                .build();
    }

    @Test
    void createReview_shouldReturn201AndPersistToDatabase() {
        ReviewRequestDTO request = buildRequest(1L, 10L, Review.ReviewedEntityType.FACILITY, 5);

        ResponseEntity<ReviewResponseDTO> response =
                restTemplate.postForEntity("/api/reviews", request, ReviewResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getReviewerId()).isEqualTo(1L);
        assertThat(response.getBody().getReviewedEntityId()).isEqualTo(10L);
        assertThat(response.getBody().getRating()).isEqualTo(5);
        assertThat(response.getBody().getReviewedEntityType()).isEqualTo(Review.ReviewedEntityType.FACILITY);
        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    @Test
    void getReviewById_shouldReturn200WithPersistedData() {
        ReviewResponseDTO created = restTemplate.postForObject(
                "/api/reviews", buildRequest(2L, 20L, Review.ReviewedEntityType.EQUIPMENT, 4), ReviewResponseDTO.class);

        ResponseEntity<ReviewResponseDTO> response =
                restTemplate.getForEntity("/api/reviews/" + created.getId(), ReviewResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getReviewerId()).isEqualTo(2L);
        assertThat(response.getBody().getRating()).isEqualTo(4);
        assertThat(response.getBody().getReviewedEntityType()).isEqualTo(Review.ReviewedEntityType.EQUIPMENT);
    }

    @Test
    void getReviewsByReviewer_shouldReturnOnlyThatReviewersReviews() {
        restTemplate.postForObject("/api/reviews", buildRequest(7L, 10L, Review.ReviewedEntityType.FACILITY, 5), ReviewResponseDTO.class);
        restTemplate.postForObject("/api/reviews", buildRequest(7L, 11L, Review.ReviewedEntityType.FACILITY, 3), ReviewResponseDTO.class);
        restTemplate.postForObject("/api/reviews", buildRequest(8L, 10L, Review.ReviewedEntityType.FACILITY, 4), ReviewResponseDTO.class);

        ResponseEntity<List<ReviewResponseDTO>> response = restTemplate.exchange(
                "/api/reviews/reviewer/7",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ReviewResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(r -> r.getReviewerId().equals(7L));
    }

    @Test
    void getReviewsByEntity_shouldReturnReviewsForThatEntity() {
        restTemplate.postForObject("/api/reviews", buildRequest(1L, 50L, Review.ReviewedEntityType.FACILITY, 5), ReviewResponseDTO.class);
        restTemplate.postForObject("/api/reviews", buildRequest(2L, 50L, Review.ReviewedEntityType.FACILITY, 3), ReviewResponseDTO.class);
        restTemplate.postForObject("/api/reviews", buildRequest(3L, 99L, Review.ReviewedEntityType.FACILITY, 4), ReviewResponseDTO.class);

        ResponseEntity<List<ReviewResponseDTO>> response = restTemplate.exchange(
                "/api/reviews/entity/FACILITY/50",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ReviewResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(r -> r.getReviewedEntityId().equals(50L));
    }

    @Test
    void updateReview_shouldReturn200WithModifiedData() {
        ReviewResponseDTO created = restTemplate.postForObject(
                "/api/reviews", buildRequest(1L, 10L, Review.ReviewedEntityType.FACILITY, 3), ReviewResponseDTO.class);

        ReviewRequestDTO updateRequest = ReviewRequestDTO.builder()
                .reviewerId(1L)
                .reviewedEntityId(10L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(5)
                .comment("Updated — excellent facility!")
                .build();

        ResponseEntity<ReviewResponseDTO> response = restTemplate.exchange(
                "/api/reviews/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                ReviewResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRating()).isEqualTo(5);
        assertThat(response.getBody().getComment()).isEqualTo("Updated — excellent facility!");
    }

    @Test
    void deleteReview_shouldReturn204AndSubsequentGetReturns404() {
        ReviewResponseDTO created = restTemplate.postForObject(
                "/api/reviews", buildRequest(1L, 10L, Review.ReviewedEntityType.FACILITY, 4), ReviewResponseDTO.class);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/reviews/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<String> getResponse =
                restTemplate.getForEntity("/api/reviews/" + created.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reviewRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void createReview_shouldReturn400_whenRatingExceedsMaximum() {
        ReviewRequestDTO invalid = ReviewRequestDTO.builder()
                .reviewerId(1L)
                .reviewedEntityId(10L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(6)   // max is 5
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/reviews", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void createReview_shouldReturn400_whenRatingIsBelowMinimum() {
        ReviewRequestDTO invalid = ReviewRequestDTO.builder()
                .reviewerId(1L)
                .reviewedEntityId(10L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(0)   // min is 1
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/reviews", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
