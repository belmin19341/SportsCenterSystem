package ba.nwt.bookingservice.repository;

import ba.nwt.bookingservice.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByFacilityId(Long facilityId);
    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("""
           SELECT b FROM Booking b
           WHERE (:userId IS NULL OR b.userId = :userId)
             AND (:facilityId IS NULL OR b.facilityId = :facilityId)
             AND (:status IS NULL OR b.status = :status)
             AND (:from IS NULL OR b.startTime >= :from)
             AND (:to IS NULL OR b.endTime <= :to)
           """)
    Page<Booking> search(@Param("userId") Long userId,
                         @Param("facilityId") Long facilityId,
                         @Param("status") Booking.BookingStatus status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         Pageable pageable);

    /**
     * Returns bookings on the same facility whose time range overlaps the given window
     * and whose status would be considered blocking (PENDING or CONFIRMED).
     */
    @Query("""
           SELECT b FROM Booking b
           WHERE b.facilityId = :facilityId
             AND b.status IN :statuses
             AND b.startTime < :end
             AND b.endTime > :start
           """)
    List<Booking> findConflictingByStatuses(@Param("facilityId") Long facilityId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("statuses") List<Booking.BookingStatus> statuses);

    default List<Booking> findConflicting(Long facilityId, LocalDateTime start, LocalDateTime end) {
        return findConflictingByStatuses(facilityId, start, end,
                List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED));
    }
}


