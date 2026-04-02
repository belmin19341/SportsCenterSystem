package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.ReviewRequestDTO;
import ba.nwt.bookingservice.dto.ReviewResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Review;
import ba.nwt.bookingservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ReviewService reviewService;

    private Review review;
    private ReviewResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        review = Review.builder()
                .id(1L).reviewerId(5L).reviewedEntityId(4L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(5).comment("Odlicno!").build();

        responseDTO = ReviewResponseDTO.builder()
                .id(1L).reviewerId(5L).rating(5).comment("Odlicno!").build();
    }

    @Test
    void getAll_shouldReturnList() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));
        when(modelMapper.map(any(Review.class), eq(ReviewResponseDTO.class))).thenReturn(responseDTO);

        List<ReviewResponseDTO> result = reviewService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(5);
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldCreateReview() {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .reviewerId(5L).reviewedEntityId(4L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(5).comment("Great!").build();

        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(modelMapper.map(any(Review.class), eq(ReviewResponseDTO.class))).thenReturn(responseDTO);

        ReviewResponseDTO result = reviewService.create(request);

        assertThat(result).isNotNull();
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void delete_shouldThrowNotFound() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

