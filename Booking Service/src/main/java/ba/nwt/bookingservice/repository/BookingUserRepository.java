package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.BookingUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingUserRepository extends JpaRepository<BookingUser, Long> {

    @EntityGraph(attributePaths = {"booking"})
    List<BookingUser> findByBookingId(Long bookingId);

    @EntityGraph(attributePaths = {"booking"})
    List<BookingUser> findByUserId(Long userId);
}
