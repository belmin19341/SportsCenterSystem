package ba.nwt.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentView {
    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String status;
    private String transactionId;
}
