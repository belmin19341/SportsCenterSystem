package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.service.BookingSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes the asynchronous Saga-based booking creation endpoint.
 *
 * POST /api/bookings/saga
 *   → Saves booking as PENDING and publishes BookingCreatedEvent to RabbitMQ.
 *   → Returns 202 Accepted immediately; confirmation arrives asynchronously.
 *
 * POST /api/bookings/saga?simulateFailure=true
 *   → Forces Payment Service to simulate a failure → tests compensating transaction.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Saga", description = "Asynchronous booking via RabbitMQ Saga Choreography")
public class BookingSagaController {

    private final BookingSagaService bookingSagaService;

    @PostMapping("/saga")
    @Operation(
        summary = "Z7 — Create booking via Saga Choreography (async RabbitMQ)",
        description = """
            Initiates the Booking-Payment Saga:
            1. Saves Booking with status PENDING (local transaction 1).
            2. Publishes BookingCreatedEvent to RabbitMQ exchange.
            3. Returns 202 Accepted immediately.

            Payment Service (local transaction 2) processes payment asynchronously:
            - On success → publishes PaymentCompletedEvent → Booking becomes CONFIRMED.
            - On failure → publishes PaymentFailedEvent → Booking becomes CANCELLED (compensating action).

            Use simulateFailure=true to test the compensating transaction path.
            """
    )
    public ResponseEntity<BookingResponseDTO> createViaSaga(
            @Valid @RequestBody BookingRequestDTO dto,
            @RequestParam(defaultValue = "CREDIT_CARD") String paymentMethod,
            @RequestParam(defaultValue = "false") boolean simulateFailure) {

        BookingResponseDTO response = bookingSagaService.initiate(dto, paymentMethod, simulateFailure);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
