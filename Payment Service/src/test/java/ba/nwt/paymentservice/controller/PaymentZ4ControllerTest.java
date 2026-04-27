package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.NotificationRequestDTO;
import ba.nwt.paymentservice.dto.NotificationResponseDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.exception.GlobalExceptionHandler;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.service.NotificationService;
import ba.nwt.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentZ4ControllerTest {

    @Mock private PaymentService paymentService;
    @Mock private NotificationService notificationService;
    @InjectMocks private PaymentController paymentController;
    @InjectMocks private NotificationController notificationController;

    private MockMvc paymentMvc;
    private MockMvc notificationMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        notificationMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private MockMvc pageMockMvc() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule())
                .registerModule(new SpringDataJacksonConfiguration.PageModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(om))
                .build();
    }

    private PaymentResponseDTO sample() {
        PaymentResponseDTO r = new PaymentResponseDTO();
        r.setId(1L);
        r.setBookingId(10L);
        r.setAmount(new BigDecimal("50.00"));
        r.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        r.setStatus(Payment.PaymentStatus.PAID);
        return r;
    }

    @Test
    void search_paged_shouldReturn200() throws Exception {
        when(paymentService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 20), 1));
        pageMockMvc().perform(get("/api/payments?paged=true&page=0&size=20&sort=id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void patch_happyPath_shouldReturn200() throws Exception {
        when(paymentService.patch(eq(1L), any())).thenReturn(sample());
        String patch = "[{\"op\":\"replace\",\"path\":\"/amount\",\"value\":75.00}]";
        paymentMvc.perform(patch("/api/payments/1")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void patch_notFound_shouldReturn404() throws Exception {
        when(paymentService.patch(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 99"));
        String patch = "[{\"op\":\"replace\",\"path\":\"/amount\",\"value\":75.00}]";
        paymentMvc.perform(patch("/api/payments/99")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_malformedBody_shouldReturn400() throws Exception {
        paymentMvc.perform(patch("/api/payments/1")
                        .contentType("application/json-patch+json")
                        .content("{not-a-patch}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refund_shouldReturn200() throws Exception {
        when(paymentService.refund(eq(1L), eq(7L), any())).thenReturn(sample());
        paymentMvc.perform(post("/api/payments/1/refund")
                        .param("recipientUserId", "7")
                        .param("reason", "Customer request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void revenue_shouldReturn200() throws Exception {
        PaymentService.RevenueReport report = PaymentService.RevenueReport.builder()
                .from(LocalDateTime.now().minusDays(7))
                .to(LocalDateTime.now())
                .totalRevenue(new BigDecimal("123.45"))
                .byMethod(List.of(PaymentService.RevenueByMethod.builder()
                        .method(Payment.PaymentMethod.CREDIT_CARD)
                        .total(new BigDecimal("123.45"))
                        .count(2L).build()))
                .build();
        when(paymentService.getRevenueBetween(any(), any())).thenReturn(report);
        paymentMvc.perform(get("/api/payments/revenue")
                        .param("from", LocalDateTime.now().minusDays(7).toString())
                        .param("to", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(123.45))
                .andExpect(jsonPath("$.byMethod[0].method").value("CREDIT_CARD"));
    }

    @Test
    void notificationsBatch_shouldReturn201() throws Exception {
        NotificationResponseDTO resp = new NotificationResponseDTO();
        resp.setId(1L);
        when(notificationService.createBatch(anyList())).thenReturn(List.of(resp, resp, resp));
        List<NotificationRequestDTO> batch = List.of(
                NotificationRequestDTO.builder().userId(1L)
                        .type(Notification.NotificationType.BOOKING_CONFIRMATION)
                        .subject("S").message("M").build(),
                NotificationRequestDTO.builder().userId(2L)
                        .type(Notification.NotificationType.BOOKING_CONFIRMATION)
                        .subject("S").message("M").build(),
                NotificationRequestDTO.builder().userId(3L)
                        .type(Notification.NotificationType.BOOKING_CONFIRMATION)
                        .subject("S").message("M").build());
        notificationMvc.perform(post("/api/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
