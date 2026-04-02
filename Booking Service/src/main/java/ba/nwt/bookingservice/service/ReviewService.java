package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.ReviewRequestDTO;
import ba.nwt.bookingservice.dto.ReviewResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Review;
import ba.nwt.bookingservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ModelMapper modelMapper;

    public List<ReviewResponseDTO> getAll() {
        return reviewRepository.findAll().stream()
                .map(r -> modelMapper.map(r, ReviewResponseDTO.class))
                .collect(Collectors.toList());
    }

    public ReviewResponseDTO getById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return modelMapper.map(review, ReviewResponseDTO.class);
    }

    public List<ReviewResponseDTO> getByReviewerId(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId).stream()
                .map(r -> modelMapper.map(r, ReviewResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<ReviewResponseDTO> getByEntity(Review.ReviewedEntityType type, Long entityId) {
        return reviewRepository.findByReviewedEntityTypeAndReviewedEntityId(type, entityId).stream()
                .map(r -> modelMapper.map(r, ReviewResponseDTO.class))
                .collect(Collectors.toList());
    }

    public ReviewResponseDTO create(ReviewRequestDTO dto) {
        Review review = Review.builder()
                .reviewerId(dto.getReviewerId())
                .reviewedEntityId(dto.getReviewedEntityId())
                .reviewedEntityType(dto.getReviewedEntityType())
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();
        Review saved = reviewRepository.save(review);
        return modelMapper.map(saved, ReviewResponseDTO.class);
    }

    public ReviewResponseDTO update(Long id, ReviewRequestDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review saved = reviewRepository.save(review);
        return modelMapper.map(saved, ReviewResponseDTO.class);
    }

    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Review not found with id: " + id);
        }
        reviewRepository.deleteById(id);
    }
}

