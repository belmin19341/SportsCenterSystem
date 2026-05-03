package ba.nwt.resourceservice.controller;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@Tag(name = "Facility", description = "Facility management APIs")
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping
    @Operation(summary = "Get all facilities (paginated, sortable, filterable)")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Facility.FacilityType type,
            @RequestParam(required = false) Facility.FacilityStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean paged,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "id") org.springframework.data.domain.Pageable pageable) {
        if (paged || type != null || status != null || q != null) {
            return ResponseEntity.ok(facilityService.search(type, status, q, pageable));
        }
        return ResponseEntity.ok(facilityService.getAll());
    }

    @org.springframework.web.bind.annotation.PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    @Operation(summary = "Partially update a facility (RFC 6902 JSON Patch)")
    public ResponseEntity<FacilityResponseDTO> patch(@PathVariable Long id,
                                                     @RequestBody com.github.fge.jsonpatch.JsonPatch patch) {
        return ResponseEntity.ok(facilityService.patch(id, patch));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get facility by ID")
    public ResponseEntity<FacilityResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(facilityService.getById(id));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get facilities by type")
    public ResponseEntity<List<FacilityResponseDTO>> getByType(@PathVariable Facility.FacilityType type) {
        return ResponseEntity.ok(facilityService.getByType(type));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get facilities by status")
    public ResponseEntity<List<FacilityResponseDTO>> getByStatus(@PathVariable Facility.FacilityStatus status) {
        return ResponseEntity.ok(facilityService.getByStatus(status));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get facilities by owner")
    public ResponseEntity<List<FacilityResponseDTO>> getByOwnerId(@PathVariable Long ownerId) {
        return ResponseEntity.ok(facilityService.getByOwnerId(ownerId));
    }

    @PostMapping
    @Operation(summary = "Create a new facility")
    public ResponseEntity<FacilityResponseDTO> create(@Valid @RequestBody FacilityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facilityService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a facility")
    public ResponseEntity<FacilityResponseDTO> update(@PathVariable Long id, @Valid @RequestBody FacilityRequestDTO dto) {
        return ResponseEntity.ok(facilityService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a facility")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facilityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

