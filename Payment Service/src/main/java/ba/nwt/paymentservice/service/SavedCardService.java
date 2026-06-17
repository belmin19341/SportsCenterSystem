package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.SavedCardResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.SavedCard;
import ba.nwt.paymentservice.repository.SavedCardRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedCardService {

    private final SavedCardRepository savedCardRepository;
    private final ModelMapper modelMapper;

    public List<SavedCardResponseDTO> getForUser(Long userId) {
        return savedCardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(card -> modelMapper.map(card, SavedCardResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public SavedCard save(Long userId, String last4, String brand, String stripeCustomerId) {
        return savedCardRepository.save(SavedCard.builder()
                .userId(userId)
                .last4(last4)
                .brand(brand)
                .stripeCustomerId(stripeCustomerId)
                .build());
    }

    public SavedCard getById(Long id) {
        return savedCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saved card not found with id: " + id));
    }

    @Transactional
    public void delete(Long id) {
        if (!savedCardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Saved card not found with id: " + id);
        }
        savedCardRepository.deleteById(id);
    }
}
