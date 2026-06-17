package ba.nwt.paymentservice.integration;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.dto.RevenueReportDTO;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: agregatni revenue izvještaj.
 * Testira da se broje samo PAID plaćanja, ispravno sumira po metodi
 * i poštuje vremenski opseg.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentRevenueIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private void createPaidPayment(BigDecimal amount, Payment.PaymentMethod method) {
        restTemplate.postForObject("/api/payments",
                PaymentRequestDTO.builder()
                        .bookingId(1L).amount(amount)
                        .depositAmount(BigDecimal.ZERO)
                        .paymentMethod(method)
                        .status(Payment.PaymentStatus.PAID)
                        .build(),
                PaymentResponseDTO.class);
    }

    private RevenueReportDTO getRevenue(LocalDateTime from, LocalDateTime to) {
        String url = UriComponentsBuilder.fromPath("/api/payments/revenue")
                .queryParam("from", from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .toUriString();
        return restTemplate.getForObject(url, RevenueReportDTO.class);
    }

    @Test
    void revenueReport_shouldSumOnlyPaidPayments_ignoringPending() {
        createPaidPayment(new BigDecimal("100.00"), Payment.PaymentMethod.CREDIT_CARD);
        createPaidPayment(new BigDecimal("200.00"), Payment.PaymentMethod.PAYPAL);
        // PENDING — ne smije biti u prihodima
        restTemplate.postForObject("/api/payments",
                PaymentRequestDTO.builder().bookingId(2L).amount(new BigDecimal("999.00"))
                        .paymentMethod(Payment.PaymentMethod.DEBIT_CARD)
                        .status(Payment.PaymentStatus.PENDING).build(),
                PaymentResponseDTO.class);

        RevenueReportDTO report = getRevenue(
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("300.00");
    }

    @Test
    void revenueReport_shouldBreakdownTotalByPaymentMethod() {
        createPaidPayment(new BigDecimal("50.00"),  Payment.PaymentMethod.CREDIT_CARD);
        createPaidPayment(new BigDecimal("70.00"),  Payment.PaymentMethod.CREDIT_CARD);
        createPaidPayment(new BigDecimal("120.00"), Payment.PaymentMethod.PAYPAL);

        RevenueReportDTO report = getRevenue(
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(5));

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("240.00");
        assertThat(report.getByMethod()).hasSize(2);

        RevenueReportDTO.RevenueByMethodDTO card = report.getByMethod().stream()
                .filter(r -> r.getMethod() == Payment.PaymentMethod.CREDIT_CARD)
                .findFirst().orElseThrow();
        assertThat(card.getTotal()).isEqualByComparingTo("120.00");
        assertThat(card.getCount()).isEqualTo(2);

        RevenueReportDTO.RevenueByMethodDTO paypal = report.getByMethod().stream()
                .filter(r -> r.getMethod() == Payment.PaymentMethod.PAYPAL)
                .findFirst().orElseThrow();
        assertThat(paypal.getTotal()).isEqualByComparingTo("120.00");
        assertThat(paypal.getCount()).isEqualTo(1);
    }

    @Test
    void revenueReport_withNoPaymentsInRange_returnsTotalZero() {
        createPaidPayment(new BigDecimal("500.00"), Payment.PaymentMethod.CREDIT_CARD);

        // Traži u opsegu koji je daleko u prošlosti
        RevenueReportDTO report = getRevenue(
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(29));

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("0.00");
        assertThat(report.getByMethod()).isEmpty();
    }

    @Test
    void revenueReport_invalidRange_toBeforeFrom_shouldReturn400() {
        String url = UriComponentsBuilder.fromPath("/api/payments/revenue")
                .queryParam("from", LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("to",   LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .toUriString();

        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
