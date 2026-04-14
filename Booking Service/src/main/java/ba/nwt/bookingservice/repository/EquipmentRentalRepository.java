package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.EquipmentRental;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRentalRepository extends JpaRepository<EquipmentRental, Long> {

    List<EquipmentRental> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"booking"})
    List<EquipmentRental> findByBookingId(Long bookingId);

    List<EquipmentRental> findByStatus(EquipmentRental.RentalStatus status);
}
