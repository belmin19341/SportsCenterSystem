package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.Equipment;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentRequestDTO {

    private Long facilityId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @NotNull(message = "Equipment type is required")
    private Equipment.EquipmentType type;

    @Size(max = 100)
    private String category;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer quantityTotal;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer quantityAvailable;

    @NotNull(message = "Price per day is required")
    @DecimalMin(value = "0.01", message = "Price per day must be greater than 0")
    private BigDecimal pricePerDay;

    private Equipment.EquipmentCondition equipmentCondition;

    @DecimalMin(value = "0.00", message = "Deposit cannot be negative")
    private BigDecimal depositRequired;
}

