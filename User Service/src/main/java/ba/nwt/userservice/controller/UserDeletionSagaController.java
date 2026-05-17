package ba.nwt.userservice.controller;

import ba.nwt.userservice.service.UserDeletionSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * DELETE /api/users/{id}/saga
 *
 * Initiates the User Deletion Saga:
 * 1. User ACTIVE → DELETION_PENDING (local TX 1)
 * 2. Booking Service cancels all active bookings (local TX 2)
 *    → Cancel OK   → User DELETED              (final state)
 *    → Cancel KO   → User restored ACTIVE      (compensating action)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Deletion Saga", description = "Async user deletion with cascade booking cancellation via RabbitMQ")
public class UserDeletionSagaController {

    private final UserDeletionSagaService service;

    @DeleteMapping("/{id}/saga")
    @Operation(summary = "Delete user with async cascade booking cancellation (Saga Choreography)")
    public ResponseEntity<Void> deleteViaSaga(@PathVariable Long id) {
        service.initiate(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
