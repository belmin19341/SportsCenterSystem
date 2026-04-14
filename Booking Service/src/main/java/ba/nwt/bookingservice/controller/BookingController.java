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
    @Operation(summary = "Get all bookings")
    public ResponseEntity<List<BookingResponseDTO>> getAll() {
        return ResponseEntity.ok(bookingService.getAll());
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

