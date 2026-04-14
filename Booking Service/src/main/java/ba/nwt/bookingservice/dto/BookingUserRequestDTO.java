package ba.nwt.bookingservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingUserRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount due is required")
    @DecimalMin(value = "0.01", message = "Amount due must be greater than 0")
    private BigDecimal amountDue;
}

