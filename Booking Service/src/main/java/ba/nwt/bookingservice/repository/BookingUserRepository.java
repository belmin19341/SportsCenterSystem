package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.BookingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingUserRepository extends JpaRepository<BookingUser, Long> {
    List<BookingUser> findByBookingId(Long bookingId);
    List<BookingUser> findByUserId(Long userId);
}

