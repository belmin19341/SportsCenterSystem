package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.DisputeRequestDTO;
import ba.nwt.paymentservice.dto.DisputeResponseDTO;
import ba.nwt.paymentservice.model.Dispute;
import ba.nwt.paymentservice.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Tag(name = "Dispute", description = "Dispute management APIs")
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @Operation(summary = "Get all disputes")
    public ResponseEntity<List<DisputeResponseDTO>> getAll() {
        return ResponseEntity.ok(disputeService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispute by ID")
    public ResponseEntity<DisputeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getById(id));
    }

    @GetMapping("/reporter/{reporterId}")
    @Operation(summary = "Get disputes by reporter ID")
    public ResponseEntity<List<DisputeResponseDTO>> getByReporterId(@PathVariable Long reporterId) {
        return ResponseEntity.ok(disputeService.getByReporterId(reporterId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get disputes by status")
    public ResponseEntity<List<DisputeResponseDTO>> getByStatus(@PathVariable Dispute.DisputeStatus status) {
        return ResponseEntity.ok(disputeService.getByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new dispute")
    public ResponseEntity<DisputeResponseDTO> create(@Valid @RequestBody DisputeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disputeService.create(dto));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve a dispute")
    public ResponseEntity<DisputeResponseDTO> resolve(@PathVariable Long id, @RequestParam String resolutionNote) {
        return ResponseEntity.ok(disputeService.resolve(id, resolutionNote));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dispute")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        disputeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

