package ba.nwt.paymentservice.repository;

import ba.nwt.paymentservice.model.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {
    List<SavedCard> findByUserIdOrderByCreatedAtDesc(Long userId);
}
