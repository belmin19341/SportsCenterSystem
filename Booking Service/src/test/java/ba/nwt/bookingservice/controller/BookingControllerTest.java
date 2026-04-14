package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.GlobalExceptionHandler;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

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

    @Test
    void getAll_shouldReturn200() throws Exception {
        BookingResponseDTO dto = BookingResponseDTO.builder()
                .id(1L).userId(3L).facilityId(1L)
                .totalPrice(new BigDecimal("60.00")).status(Booking.BookingStatus.CONFIRMED).build();

        when(bookingService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(3));
    }

    @Test
    void getById_shouldReturn404() throws Exception {
        when(bookingService.getById(99L)).thenThrow(new ResourceNotFoundException("Booking not found with id: 99"));

        mockMvc.perform(get("/api/bookings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(3L).facilityId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .totalPrice(new BigDecimal("60.00")).build();

        BookingResponseDTO response = BookingResponseDTO.builder()
                .id(1L).userId(3L).facilityId(1L)
                .totalPrice(new BigDecimal("60.00")).status(Booking.BookingStatus.PENDING).build();

        when(bookingService.create(any(BookingRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(3));
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        String invalidJson = "{\"totalPrice\": -5}";

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
