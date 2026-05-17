package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.EquipmentRentalRequestDTO;
import ba.nwt.bookingservice.dto.EquipmentRentalResponseDTO;
import ba.nwt.bookingservice.service.RentalSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/rentals/saga
 *
 * Initiates the Equipment Rental + Payment Saga:
 * 1. Rental saved as RESERVED (local TX 1)
 * 2. Payment Service creates payment (local TX 2)
 *    → Payment OK  → Rental stays RESERVED (confirmed, final state)
 *    → Payment KO  → Rental CANCELLED (compensating action)
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Saga", description = "Async equipment rental with payment via RabbitMQ")
public class RentalSagaController {

    private final RentalSagaService rentalSagaService;

    @PostMapping("/saga")
    @Operation(summary = "Create equipment rental with async payment (Saga Choreography)")
    public ResponseEntity<EquipmentRentalResponseDTO> createViaSaga(
            @Valid @RequestBody EquipmentRentalRequestDTO dto,
            @RequestParam(defaultValue = "CREDIT_CARD") String paymentMethod,
            @RequestParam(defaultValue = "false") boolean simulateFailure) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(rentalSagaService.initiate(dto, paymentMethod, simulateFailure));
    }
}
