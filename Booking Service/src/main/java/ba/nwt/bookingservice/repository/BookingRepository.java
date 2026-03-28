package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByFacilityId(Long facilityId);
    List<Booking> findByStatus(Booking.BookingStatus status);
}

