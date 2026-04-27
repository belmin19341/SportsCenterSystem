package ba.nwt.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacilityView {
    private Long id;
    private Long ownerId;
    private String name;
    private String type;
    private String status;
}
