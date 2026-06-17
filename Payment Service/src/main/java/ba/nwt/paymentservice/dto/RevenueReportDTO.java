package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RevenueReportDTO {

    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal totalRevenue;
    private List<RevenueByMethodDTO> byMethod;

    @Data
    @Builder
    public static class RevenueByMethodDTO {
        private Payment.PaymentMethod method;
        private BigDecimal total;
        private long count;
    }
}
