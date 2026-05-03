package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.client.PaymentServiceClient;
import ba.nwt.bookingservice.client.ResourceServiceClient;
import ba.nwt.bookingservice.client.UserServiceClient;
import ba.nwt.bookingservice.client.dto.FacilityView;
import ba.nwt.bookingservice.client.dto.PaymentCreateView;
import ba.nwt.bookingservice.client.dto.PaymentView;
import ba.nwt.bookingservice.client.dto.PriceQuoteView;
import ba.nwt.bookingservice.config.JsonPatchUtil;
import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.dto.BookingUserRequestDTO;
import ba.nwt.bookingservice.dto.BookingUserResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.model.BookingUser;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import com.github.fge.jsonpatch.JsonPatch;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingUserRepository bookingUserRepository;
    private final ModelMapper modelMapper;
    private final JsonPatchUtil jsonPatchUtil;
    private final ResourceServiceClient resourceServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final UserServiceClient userServiceClient;

    public List<BookingResponseDTO> getAll() {
        return bookingRepository.findAll().stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public Page<BookingResponseDTO> search(Long userId, Long facilityId,
                                           Booking.BookingStatus status,
                                           LocalDateTime from, LocalDateTime to,
                                           Pageable pageable) {
        return bookingRepository.search(userId, facilityId, status, from, to, pageable)
                .map(b -> modelMapper.map(b, BookingResponseDTO.class));
    }

    public List<BookingResponseDTO> findConflicting(Long facilityId, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        return bookingRepository.findConflicting(facilityId, start, end).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO patch(Long id, JsonPatch patch) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        BookingRequestDTO current = modelMapper.map(booking, BookingRequestDTO.class);
        BookingRequestDTO patched = jsonPatchUtil.apply(patch, current, BookingRequestDTO.class);
        if (patched.getEndTime().isBefore(patched.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        // Reject reschedule onto a conflicting slot (excluding self).
        List<Booking> conflicts = bookingRepository.findConflicting(
                patched.getFacilityId(), patched.getStartTime(), patched.getEndTime());
        conflicts.removeIf(b -> b.getId().equals(id));
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Patched booking would conflict with existing booking id=" + conflicts.get(0).getId());
        }
        booking.setUserId(patched.getUserId());
        booking.setFacilityId(patched.getFacilityId());
        booking.setStartTime(patched.getStartTime());
        booking.setEndTime(patched.getEndTime());
        booking.setTotalPrice(patched.getTotalPrice());
        booking.setIsRecurring(patched.getIsRecurring() != null ? patched.getIsRecurring() : false);
        booking.setRecurringPattern(patched.getRecurringPattern());
        if (patched.getStatus() != null) {
            booking.setStatus(patched.getStatus());
        }
        return modelMapper.map(bookingRepository.save(booking), BookingResponseDTO.class);
    }

    @Transactional
    public List<BookingResponseDTO> createRecurring(BookingRequestDTO base,
                                                    String pattern, int occurrences) {
        if (occurrences < 1 || occurrences > 52) {
            throw new IllegalArgumentException("Occurrences must be between 1 and 52");
        }
        if (!base.getEndTime().isAfter(base.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        ChronoUnit step = switch (pattern == null ? "WEEKLY" : pattern.toUpperCase()) {
            case "DAILY"   -> ChronoUnit.DAYS;
            case "WEEKLY"  -> ChronoUnit.WEEKS;
            case "MONTHLY" -> ChronoUnit.MONTHS;
            default        -> throw new IllegalArgumentException("Unknown recurring pattern: " + pattern);
        };
        List<Booking> entities = new ArrayList<>(occurrences);
        for (int i = 0; i < occurrences; i++) {
            LocalDateTime s = base.getStartTime().plus(i, step);
            LocalDateTime e = base.getEndTime().plus(i, step);
            List<Booking> conflicts = bookingRepository.findConflicting(base.getFacilityId(), s, e);
            if (!conflicts.isEmpty()) {
                throw new IllegalArgumentException(
                        "Recurring slot " + s + " conflicts with booking id=" + conflicts.get(0).getId());
            }
            entities.add(Booking.builder()
                    .userId(base.getUserId())
                    .facilityId(base.getFacilityId())
                    .startTime(s).endTime(e)
                    .totalPrice(base.getTotalPrice())
                    .isRecurring(true)
                    .recurringPattern(pattern == null ? "WEEKLY" : pattern.toUpperCase())
                    .status(Booking.BookingStatus.PENDING)
                    .build());
        }
        return bookingRepository.saveAll(entities).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Atomically creates a Booking + N BookingUser rows (split equally among participants).
     * Rolls back the whole graph on any single failure.
     */
    @Transactional
    public BookingResponseDTO createGroup(BookingRequestDTO bookingDto, List<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            throw new IllegalArgumentException("Group booking requires at least one participant");
        }
        if (!bookingDto.getEndTime().isAfter(bookingDto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        List<Booking> conflicts = bookingRepository.findConflicting(
                bookingDto.getFacilityId(), bookingDto.getStartTime(), bookingDto.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Slot conflicts with booking id=" + conflicts.get(0).getId());
        }
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(bookingDto.getUserId())
                .facilityId(bookingDto.getFacilityId())
                .startTime(bookingDto.getStartTime())
                .endTime(bookingDto.getEndTime())
                .totalPrice(bookingDto.getTotalPrice())
                .isRecurring(false)
                .status(Booking.BookingStatus.PENDING)
                .build());

        BigDecimal split = bookingDto.getTotalPrice()
                .divide(BigDecimal.valueOf(participantUserIds.size()), 2, RoundingMode.HALF_UP);
        for (Long uid : participantUserIds) {
            bookingUserRepository.save(BookingUser.builder()
                    .booking(booking)
                    .userId(uid)
                    .amountDue(split)
                    .paymentStatus(BookingUser.PaymentStatus.PENDING)
                    .invitedAt(LocalDateTime.now())
                    .build());
        }
        return modelMapper.map(booking, BookingResponseDTO.class);
    }

    public BookingResponseDTO getById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return modelMapper.map(booking, BookingResponseDTO.class);
    }

    public List<BookingResponseDTO> getByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getByFacilityId(Long facilityId) {
        return bookingRepository.findByFacilityId(facilityId).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO create(BookingRequestDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Booking booking = Booking.builder()
                .userId(dto.getUserId())
                .facilityId(dto.getFacilityId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalPrice(dto.getTotalPrice())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurringPattern(dto.getRecurringPattern())
                .status(dto.getStatus() != null ? dto.getStatus() : Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        return modelMapper.map(saved, BookingResponseDTO.class);
    }

    @Transactional
    public BookingResponseDTO update(Long id, BookingRequestDTO dto) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        booking.setUserId(dto.getUserId());
        booking.setFacilityId(dto.getFacilityId());
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setTotalPrice(dto.getTotalPrice());
        booking.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);
        booking.setRecurringPattern(dto.getRecurringPattern());
        if (dto.getStatus() != null) {
            booking.setStatus(dto.getStatus());
        }

        Booking saved = bookingRepository.save(booking);
        return modelMapper.map(saved, BookingResponseDTO.class);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    /**
     * Z5 — Synchronous orchestration of a Booking across 4 services:
     *   1. Resource Service: validate facility (must exist and be ACTIVE).
     *   2. Resource Service: compute authoritative price (overrides client-supplied total).
     *   3. Payment Service: charge amount; on success booking is CONFIRMED.
     *   4. User Service: credit loyalty points (best-effort; failure does NOT roll back booking).
     *
     * Failure semantics:
     *   - Resource down OR facility inactive  → 503/400, NO booking persisted.
     *   - Payment down OR payment FAILED      → booking persisted as CANCELLED, exception bubbles.
     *   - User Service down                   → booking stays CONFIRMED, warning logged.
     */
    @Transactional
    public BookingResponseDTO createOrchestrated(BookingRequestDTO dto, String paymentMethod) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Step 1 — facility validation (mandatory)
        FacilityView facility = resourceServiceClient.getFacility(dto.getFacilityId());
        if (facility == null || facility.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(facility.getStatus())) {
            throw new IllegalArgumentException(
                    "Facility " + dto.getFacilityId() + " is not bookable (status="
                            + (facility == null ? "null" : facility.getStatus()) + ")");
        }

        // Step 2 — authoritative price (mandatory)
        PriceQuoteView quote = resourceServiceClient.calculatePrice(
                dto.getFacilityId(), dto.getStartTime(), dto.getEndTime());
        if (quote == null || quote.getTotalPrice() == null) {
            throw new IllegalArgumentException("Pricing service returned no quote for facility "
                    + dto.getFacilityId());
        }
        java.math.BigDecimal authoritativePrice = quote.getTotalPrice();

        // Persist booking PENDING — happens before payment so we have an ID to reference.
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(dto.getUserId())
                .facilityId(dto.getFacilityId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalPrice(authoritativePrice)
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurringPattern(dto.getRecurringPattern())
                .status(Booking.BookingStatus.PENDING)
                .build());

        // Step 3 — charge (mandatory; on any failure mark booking CANCELLED and rethrow)
        PaymentView payment;
        try {
            payment = paymentServiceClient.createPayment(PaymentCreateView.builder()
                    .bookingId(booking.getId())
                    .amount(authoritativePrice)
                    .paymentMethod(paymentMethod == null ? "CREDIT_CARD" : paymentMethod)
                    .build());
        } catch (RuntimeException ex) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw ex;
        }
        if (payment == null || !"PAID".equalsIgnoreCase(payment.getStatus())) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            throw new IllegalArgumentException("Payment did not succeed (status="
                    + (payment == null ? "null" : payment.getStatus()) + ")");
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Step 4 — loyalty (best-effort; never blocks confirmation)
        try {
            int points = authoritativePrice.setScale(0, java.math.RoundingMode.DOWN).intValueExact();
            if (points > 0) {
                userServiceClient.addLoyaltyPoints(dto.getUserId(), points);
            }
        } catch (RuntimeException ex) {
            log.warn("Loyalty credit failed for booking {} (user {}): {}",
                    booking.getId(), dto.getUserId(), ex.toString());
        }

        return modelMapper.map(booking, BookingResponseDTO.class);
    }
}

