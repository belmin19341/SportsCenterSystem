package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.EquipmentRentalRequestDTO;
import ba.nwt.bookingservice.dto.EquipmentRentalResponseDTO;
import ba.nwt.bookingservice.service.EquipmentRentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "EquipmentRental", description = "Equipment rental management APIs")
public class EquipmentRentalController {

    private final EquipmentRentalService equipmentRentalService;

    @GetMapping
    @Operation(summary = "Get all equipment rentals")
    public ResponseEntity<List<EquipmentRentalResponseDTO>> getAll() {
        return ResponseEntity.ok(equipmentRentalService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment rental by ID")
    public ResponseEntity<EquipmentRentalResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentRentalService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get rentals by user ID")
    public ResponseEntity<List<EquipmentRentalResponseDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(equipmentRentalService.getByUserId(userId));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get rentals by booking ID")
    public ResponseEntity<List<EquipmentRentalResponseDTO>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(equipmentRentalService.getByBookingId(bookingId));
    }

    @PostMapping
    @Operation(summary = "Create a new equipment rental")
    public ResponseEntity<EquipmentRentalResponseDTO> create(@Valid @RequestBody EquipmentRentalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentRentalService.create(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an equipment rental")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentRentalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

