package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.EquipmentRental;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentRentalResponseDTO {
    private Long id;
    private Long userId;
    private Long equipmentId;
    private Long bookingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal depositPaid;
    private EquipmentRental.ConditionOnReturn conditionOnReturn;
    private EquipmentRental.RentalStatus status;
    private LocalDateTime createdAt;
}

