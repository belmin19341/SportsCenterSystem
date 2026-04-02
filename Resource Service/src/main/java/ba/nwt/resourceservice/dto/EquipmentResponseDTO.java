package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.Equipment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentResponseDTO {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private String name;
    private Equipment.EquipmentType type;
    private String category;
    private Integer quantityTotal;
    private Integer quantityAvailable;
    private BigDecimal pricePerDay;
    private Equipment.EquipmentCondition equipmentCondition;
    private BigDecimal depositRequired;
    private Integer rentalCount;
    private LocalDate lastMaintenance;
    private LocalDateTime createdAt;
}

