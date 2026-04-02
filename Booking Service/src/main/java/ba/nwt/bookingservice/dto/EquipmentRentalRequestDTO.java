package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.EquipmentRental;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentRentalRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    private Long bookingId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
    private BigDecimal totalPrice;

    @DecimalMin(value = "0.00", message = "Deposit cannot be negative")
    private BigDecimal depositPaid;

    private EquipmentRental.RentalStatus status;
}

