package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.DownstreamBadRequestException;
import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import ba.nwt.bookingservice.exception.GlobalExceptionHandler;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Z5 — Controller-level tests for POST /api/bookings/orchestrated.
 * Verifies that the typed downstream exceptions are mapped to the right HTTP statuses
 * by the GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class BookingControllerZ5Test {

    @Mock private BookingService bookingService;
    @InjectMocks private BookingController bookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private BookingRequestDTO sampleRequest() {
        return BookingRequestDTO.builder()
                .userId(7L).facilityId(42L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("10.00"))
                .build();
    }

    @Test
    void orchestrated_happyPath_returns201() throws Exception {
        BookingResponseDTO response = BookingResponseDTO.builder()
                .id(1L).userId(7L).facilityId(42L)
                .totalPrice(new BigDecimal("80.00"))
                .status(Booking.BookingStatus.CONFIRMED).build();
        when(bookingService.createOrchestrated(any(BookingRequestDTO.class), eq("CREDIT_CARD")))
                .thenReturn(response);

        mockMvc.perform(post("/api/bookings/orchestrated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(80.00));
    }

    @Test
    void orchestrated_resourceServiceDown_returns503() throws Exception {
        when(bookingService.createOrchestrated(any(), any()))
                .thenThrow(new DownstreamUnavailableException("resource-service",
                        "Resource service is unavailable"));

        mockMvc.perform(post("/api/bookings/orchestrated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.details[0]").value("downstream: resource-service"));
    }

    @Test
    void orchestrated_paymentRejectedRequest_returns400() throws Exception {
        when(bookingService.createOrchestrated(any(), any()))
                .thenThrow(new DownstreamBadRequestException("payment-service", 400,
                        "payment-service rejected request (400): bad amount"));

        mockMvc.perform(post("/api/bookings/orchestrated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details[0]").value("downstream: payment-service"))
                .andExpect(jsonPath("$.details[1]").value("downstreamStatus: 400"));
    }

    @Test
    void orchestrated_facilityInactive_returns400() throws Exception {
        when(bookingService.createOrchestrated(any(), any()))
                .thenThrow(new IllegalArgumentException("Facility 42 is not bookable (status=MAINTENANCE)"));

        mockMvc.perform(post("/api/bookings/orchestrated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest());
    }
}
