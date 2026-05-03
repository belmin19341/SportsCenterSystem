package ba.nwt.resourceservice.controller;

import ba.nwt.resourceservice.dto.EquipmentRequestDTO;
import ba.nwt.resourceservice.dto.EquipmentResponseDTO;
import ba.nwt.resourceservice.model.Equipment;
import ba.nwt.resourceservice.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "Equipment management APIs")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @Operation(summary = "Get all equipment")
    public ResponseEntity<List<EquipmentResponseDTO>> getAll() {
        return ResponseEntity.ok(equipmentService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by ID")
    public ResponseEntity<EquipmentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getById(id));
    }

    @GetMapping("/facility/{facilityId}")
    @Operation(summary = "Get equipment by facility ID")
    public ResponseEntity<List<EquipmentResponseDTO>> getByFacilityId(@PathVariable Long facilityId) {
        return ResponseEntity.ok(equipmentService.getByFacilityId(facilityId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get equipment by type")
    public ResponseEntity<List<EquipmentResponseDTO>> getByType(@PathVariable Equipment.EquipmentType type) {
        return ResponseEntity.ok(equipmentService.getByType(type));
    }

    @PostMapping
    @Operation(summary = "Create new equipment")
    public ResponseEntity<EquipmentResponseDTO> create(@Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(dto));
    }

    @PostMapping("/batch")
    @Operation(summary = "Create equipment in batch (transactional)")
    public ResponseEntity<List<EquipmentResponseDTO>> createBatch(
            @Valid @RequestBody List<EquipmentRequestDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.createBatch(dtos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment")
    public ResponseEntity<EquipmentResponseDTO> update(@PathVariable Long id, @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.ok(equipmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

