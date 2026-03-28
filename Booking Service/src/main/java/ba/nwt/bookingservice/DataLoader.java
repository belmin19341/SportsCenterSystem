package ba.nwt.bookingservice;

import ba.nwt.bookingservice.model.*;
import ba.nwt.bookingservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final BookingRepository bookingRepository;
    private final BookingUserRepository bookingUserRepository;
    private final EquipmentRentalRepository equipmentRentalRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public void run(String... args) {
        if (bookingRepository.count() > 0) {
            log.info(">>> Podaci već postoje, preskačem DataLoader.");
            return;
        }

        log.info(">>> Unosim početne podatke u Booking Service bazu...");

        // ── Bookings ──
        Booking booking1 = bookingRepository.save(Booking.builder()
                .userId(3L)  // belmin_d
                .facilityId(1L)  // Mali teren A
                .startTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(19).withMinute(0))
                .totalPrice(new BigDecimal("60.00"))
                .isRecurring(false)
                .status(Booking.BookingStatus.CONFIRMED)
                .build());

        Booking booking2 = bookingRepository.save(Booking.builder()
                .userId(4L)  // harun_g
                .facilityId(3L)  // Padel Court 1
                .startTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(3).withHour(11).withMinute(30))
                .totalPrice(new BigDecimal("60.00"))
                .isRecurring(false)
                .status(Booking.BookingStatus.PENDING)
                .build());

        Booking booking3 = bookingRepository.save(Booking.builder()
                .userId(3L)  // belmin_d
                .facilityId(1L)  // Mali teren A
                .startTime(LocalDateTime.now().plusDays(9).withHour(18).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(9).withHour(19).withMinute(0))
                .totalPrice(new BigDecimal("60.00"))
                .isRecurring(true)
                .recurringPattern("WEEKLY")
                .status(Booking.BookingStatus.CONFIRMED)
                .build());

        Booking pastBooking = bookingRepository.save(Booking.builder()
                .userId(5L)  // amar_h
                .facilityId(4L)  // Teniski teren 1
                .startTime(LocalDateTime.now().minusDays(5).withHour(16).withMinute(0))
                .endTime(LocalDateTime.now().minusDays(5).withHour(17).withMinute(0))
                .totalPrice(new BigDecimal("35.00"))
                .status(Booking.BookingStatus.COMPLETED)
                .build());

        // ── Booking Users (grupna rezervacija) ──
        bookingUserRepository.save(BookingUser.builder()
                .booking(booking1)
                .userId(3L)
                .amountDue(new BigDecimal("30.00"))
                .paymentStatus(BookingUser.PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusHours(2))
                .build());

        bookingUserRepository.save(BookingUser.builder()
                .booking(booking1)
                .userId(4L)
                .amountDue(new BigDecimal("30.00"))
                .paymentStatus(BookingUser.PaymentStatus.PENDING)
                .invitedAt(LocalDateTime.now().minusHours(1))
                .build());

        // ── Equipment Rentals ──
        equipmentRentalRepository.save(EquipmentRental.builder()
                .userId(4L)
                .equipmentId(2L)  // Bullpadel reket
                .booking(booking2)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(3))
                .quantity(2)
                .totalPrice(new BigDecimal("30.00"))
                .depositPaid(new BigDecimal("50.00"))
                .status(EquipmentRental.RentalStatus.RESERVED)
                .build());

        equipmentRentalRepository.save(EquipmentRental.builder()
                .userId(5L)
                .equipmentId(4L)  // Sobni bicikl — samostalni najam
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .quantity(1)
                .totalPrice(new BigDecimal("140.00"))
                .depositPaid(new BigDecimal("100.00"))
                .status(EquipmentRental.RentalStatus.ACTIVE)
                .build());

        // ── Reviews ──
        reviewRepository.save(Review.builder()
                .reviewerId(5L)
                .reviewedEntityId(4L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(5)
                .comment("Odličan teniski teren, super podloga!")
                .build());

        reviewRepository.save(Review.builder()
                .reviewerId(3L)
                .reviewedEntityId(1L)
                .reviewedEntityType(Review.ReviewedEntityType.FACILITY)
                .rating(4)
                .comment("Dobar mali teren, moglo bi bolje osvjetljenje.")
                .build());

        reviewRepository.save(Review.builder()
                .reviewerId(4L)
                .reviewedEntityId(2L)
                .reviewedEntityType(Review.ReviewedEntityType.EQUIPMENT)
                .rating(5)
                .comment("Bullpadel reket je fantastičan, preporučujem!")
                .build());

        log.info(">>> Booking Service DataLoader završen — {} bookinga, {} rental, {} review.",
                bookingRepository.count(), equipmentRentalRepository.count(), reviewRepository.count());
    }
}

