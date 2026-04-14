package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.DocumentRequestDTO;
import ba.nwt.paymentservice.dto.DocumentResponseDTO;
import ba.nwt.paymentservice.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document management APIs")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "Get all documents")
    public ResponseEntity<List<DocumentResponseDTO>> getAll() {
        return ResponseEntity.ok(documentService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<DocumentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get documents by user ID")
    public ResponseEntity<List<DocumentResponseDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(documentService.getByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new document")
    public ResponseEntity<DocumentResponseDTO> create(@Valid @RequestBody DocumentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

