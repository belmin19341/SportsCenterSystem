package ba.nwt.resourceservice.controller;

import ba.nwt.resourceservice.dto.PricingRuleRequestDTO;
import ba.nwt.resourceservice.dto.PricingRuleResponseDTO;
import ba.nwt.resourceservice.service.PricingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pricing-rules")
@RequiredArgsConstructor
@Tag(name = "PricingRule", description = "Pricing rule management APIs")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @GetMapping
    @Operation(summary = "Get all pricing rules")
    public ResponseEntity<List<PricingRuleResponseDTO>> getAll() {
        return ResponseEntity.ok(pricingRuleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pricing rule by ID")
    public ResponseEntity<PricingRuleResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pricingRuleService.getById(id));
    }

    @GetMapping("/facility/{facilityId}")
    @Operation(summary = "Get pricing rules by facility")
    public ResponseEntity<List<PricingRuleResponseDTO>> getByFacility(@PathVariable Long facilityId) {
        return ResponseEntity.ok(pricingRuleService.getByFacilityId(facilityId));
    }

    @PostMapping
    @Operation(summary = "Create a pricing rule")
    public ResponseEntity<PricingRuleResponseDTO> create(@Valid @RequestBody PricingRuleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingRuleService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pricing rule")
    public ResponseEntity<PricingRuleResponseDTO> update(@PathVariable Long id,
                                                         @Valid @RequestBody PricingRuleRequestDTO dto) {
        return ResponseEntity.ok(pricingRuleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pricing rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pricingRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calculate")
    @Operation(summary = "Calculate facility rental price for a time window (combines base price + matching rules)")
    public ResponseEntity<PricingRuleService.PriceQuote> calculate(
            @RequestParam Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(pricingRuleService.calculatePrice(facilityId, start, end));
    }
}
