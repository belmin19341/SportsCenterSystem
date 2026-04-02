package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingUserRequestDTO;
import ba.nwt.bookingservice.dto.BookingUserResponseDTO;
import ba.nwt.bookingservice.service.BookingUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking-users")
@RequiredArgsConstructor
@Tag(name = "BookingUser", description = "Booking user (group booking) management APIs")
public class BookingUserController {

    private final BookingUserService bookingUserService;

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get users for a booking")
    public ResponseEntity<List<BookingUserResponseDTO>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingUserService.getByBookingId(bookingId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get booking participations for a user")
    public ResponseEntity<List<BookingUserResponseDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingUserService.getByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Add user to a booking")
    public ResponseEntity<BookingUserResponseDTO> create(@Valid @RequestBody BookingUserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingUserService.create(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove user from a booking")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

