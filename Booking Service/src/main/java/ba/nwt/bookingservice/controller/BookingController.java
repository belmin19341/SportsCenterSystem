package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all bookings (paginated/sortable/filterable when any query param is set)")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(defaultValue = "false") boolean paged,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "id") org.springframework.data.domain.Pageable pageable) {
        if (paged || userId != null || facilityId != null || status != null || from != null || to != null) {
            return ResponseEntity.ok(bookingService.search(userId, facilityId, status, from, to, pageable));
        }
        return ResponseEntity.ok(bookingService.getAll());
    }

    @org.springframework.web.bind.annotation.PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    @Operation(summary = "Partially update a booking (RFC 6902 JSON Patch)")
    public ResponseEntity<BookingResponseDTO> patch(@PathVariable Long id,
                                                    @RequestBody com.github.fge.jsonpatch.JsonPatch patch) {
        return ResponseEntity.ok(bookingService.patch(id, patch));
    }

    @GetMapping("/facility/{facilityId}/conflicting")
    @Operation(summary = "Find PENDING/CONFIRMED bookings overlapping the given window for a facility")
    public ResponseEntity<List<BookingResponseDTO>> findConflicting(
            @PathVariable Long facilityId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime start,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime end) {
        return ResponseEntity.ok(bookingService.findConflicting(facilityId, start, end));
    }

    @PostMapping("/recurring")
    @Operation(summary = "Create N recurring bookings in a single transaction (DAILY|WEEKLY|MONTHLY)")
    public ResponseEntity<List<BookingResponseDTO>> createRecurring(
            @Valid @RequestBody BookingRequestDTO base,
            @RequestParam(defaultValue = "WEEKLY") String pattern,
            @RequestParam(defaultValue = "4") int occurrences) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createRecurring(base, pattern, occurrences));
    }

    @PostMapping("/group")
    @Operation(summary = "Create a group booking with split amounts atomically")
    public ResponseEntity<BookingResponseDTO> createGroup(
            @Valid @RequestBody GroupBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createGroup(request.getBooking(), request.getParticipantUserIds()));
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class GroupBookingRequest {
        @jakarta.validation.Valid
        @jakarta.validation.constraints.NotNull(message = "Booking payload is required")
        private BookingRequestDTO booking;
        @jakarta.validation.constraints.NotEmpty(message = "Participant user IDs are required")
        private List<Long> participantUserIds;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get bookings by user ID")
    public ResponseEntity<List<BookingResponseDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getByUserId(userId));
    }

    @GetMapping("/facility/{facilityId}")
    @Operation(summary = "Get bookings by facility ID")
    public ResponseEntity<List<BookingResponseDTO>> getByFacilityId(@PathVariable Long facilityId) {
        return ResponseEntity.ok(bookingService.getByFacilityId(facilityId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get bookings by status")
    public ResponseEntity<List<BookingResponseDTO>> getByStatus(@PathVariable Booking.BookingStatus status) {
        return ResponseEntity.ok(bookingService.getByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<BookingResponseDTO> create(@Valid @RequestBody BookingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a booking")
    public ResponseEntity<BookingResponseDTO> update(@PathVariable Long id, @Valid @RequestBody BookingRequestDTO dto) {
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a booking")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

