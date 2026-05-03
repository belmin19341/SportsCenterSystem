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
    @Operation(summary = "List payments — paginated/filterable when any query param is set")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Payment.PaymentStatus status,
            @RequestParam(required = false) Payment.PaymentMethod method,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(defaultValue = "false") boolean paged,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "id") org.springframework.data.domain.Pageable pageable) {
        if (paged || status != null || method != null || bookingId != null
                || minAmount != null || maxAmount != null || from != null || to != null) {
            return ResponseEntity.ok(paymentService.search(status, method, bookingId,
                    minAmount, maxAmount, from, to, pageable));
        }
        return ResponseEntity.ok(paymentService.getAll());
    }

    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    @Operation(summary = "Partially update a payment (RFC 6902 JSON Patch)")
    public ResponseEntity<PaymentResponseDTO> patch(@PathVariable Long id,
                                                    @RequestBody com.github.fge.jsonpatch.JsonPatch patch) {
        return ResponseEntity.ok(paymentService.patch(id, patch));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a PAID payment and notify the recipient (atomic)")
    public ResponseEntity<PaymentResponseDTO> refund(@PathVariable Long id,
                                                     @RequestParam Long recipientUserId,
                                                     @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.refund(id, recipientUserId, reason));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Aggregate revenue (PAID) between two timestamps, total + per method")
    public ResponseEntity<PaymentService.RevenueReport> revenue(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return ResponseEntity.ok(paymentService.getRevenueBetween(from, to));
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

