package ba.nwt.paymentservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserView {
    private Long id;
    private String username;
    private String email;
    private String role;
}
