package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.exception.GlobalExceptionHandler;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .id(1L).bookingId(1L).amount(new BigDecimal("60.00"))
                .status(Payment.PaymentStatus.PAID).build();

        when(paymentService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(60.00));
    }

    @Test
    void getById_shouldReturn404() throws Exception {
        when(paymentService.getById(99L)).thenThrow(new ResourceNotFoundException("Payment not found with id: 99"));

        mockMvc.perform(get("/api/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .bookingId(1L).amount(new BigDecimal("60.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD).build();

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(1L).bookingId(1L).amount(new BigDecimal("60.00"))
                .status(Payment.PaymentStatus.PENDING).build();

        when(paymentService.create(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(1));
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        String invalidJson = "{\"amount\": -10}";

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
