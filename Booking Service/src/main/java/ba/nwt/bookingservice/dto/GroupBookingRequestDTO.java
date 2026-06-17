package ba.nwt.bookingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupBookingRequestDTO {

    @Valid
    @NotNull(message = "Booking payload is required")
    private BookingRequestDTO booking;

    @NotEmpty(message = "Participant user IDs are required")
    private List<Long> participantUserIds;
}
