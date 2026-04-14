package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<List<PaymentResponseDTO>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payments by booking ID")
    public ResponseEntity<List<PaymentResponseDTO>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment by transaction ID")
    public ResponseEntity<PaymentResponseDTO> getByTransactionId(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getByTransactionId(transactionId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status")
    public ResponseEntity<List<PaymentResponseDTO>> getByStatus(@PathVariable Payment.PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new payment")
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a payment")
    public ResponseEntity<PaymentResponseDTO> update(@PathVariable Long id, @Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.ok(paymentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

