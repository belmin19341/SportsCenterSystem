package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.service.BookingCancellationSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/bookings/{id}/cancel
 *
 * Initiates the Booking Cancellation Saga:
 * 1. Booking CONFIRMED → CANCELLATION_PENDING (local TX 1)
 * 2. Payment Service processes refund (local TX 2)
 *    → Refund OK  → Booking CANCELLED           (final state)
 *    → Refund KO  → Booking restored CONFIRMED  (compensating action)
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Cancellation Saga", description = "Async cancellation with refund via RabbitMQ")
public class BookingCancellationSagaController {

    private final BookingCancellationSagaService service;

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a confirmed booking with async refund (Saga Choreography)")
    public ResponseEntity<BookingResponseDTO> cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean simulateFailure) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.initiate(id, simulateFailure));
    }
}
