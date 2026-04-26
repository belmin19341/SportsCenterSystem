package ba.nwt.bookingservice.controller;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.GlobalExceptionHandler;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.service.BookingService;
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
class BookingControllerZ4Test {

    @Mock private BookingService bookingService;
    @InjectMocks private BookingController bookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Separate MockMvc that can serialize Spring Data Page<T>. */
    private MockMvc pageMockMvc() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule())
                .registerModule(new SpringDataJacksonConfiguration.PageModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter conv = new MappingJackson2HttpMessageConverter(om);
        return MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(conv)
                .build();
    }

    private BookingResponseDTO sampleResponse() {
        BookingResponseDTO r = new BookingResponseDTO();
        r.setId(1L);
        r.setUserId(1L);
        r.setFacilityId(2L);
        r.setStartTime(LocalDateTime.now().plusDays(1));
        r.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        r.setTotalPrice(new BigDecimal("100.00"));
        r.setStatus(Booking.BookingStatus.PENDING);
        return r;
    }

    @Test
    void search_paged_shouldReturn200() throws Exception {
        when(bookingService.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));
        pageMockMvc().perform(get("/api/bookings?paged=true&page=0&size=20&sort=id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void patch_happyPath_shouldReturn200() throws Exception {
        when(bookingService.patch(eq(1L), any())).thenReturn(sampleResponse());
        String patch = "[{\"op\":\"replace\",\"path\":\"/totalPrice\",\"value\":150.00}]";
        mockMvc.perform(patch("/api/bookings/1")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void patch_notFound_shouldReturn404() throws Exception {
        when(bookingService.patch(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Booking not found with id: 99"));
        String patch = "[{\"op\":\"replace\",\"path\":\"/totalPrice\",\"value\":150.00}]";
        mockMvc.perform(patch("/api/bookings/99")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_malformedBody_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/bookings/1")
                        .contentType("application/json-patch+json")
                        .content("{not-a-patch}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findConflicting_shouldReturn200() throws Exception {
        when(bookingService.findConflicting(eq(2L), any(), any()))
                .thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/bookings/facility/2/conflicting")
                        .param("start", LocalDateTime.now().plusDays(1).toString())
                        .param("end", LocalDateTime.now().plusDays(1).plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createRecurring_shouldReturn201() throws Exception {
        BookingRequestDTO base = BookingRequestDTO.builder()
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        when(bookingService.createRecurring(any(), eq("WEEKLY"), eq(3)))
                .thenReturn(List.of(sampleResponse(), sampleResponse(), sampleResponse()));
        mockMvc.perform(post("/api/bookings/recurring?pattern=WEEKLY&occurrences=3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(base)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void createGroup_shouldReturn201() throws Exception {
        BookingRequestDTO base = BookingRequestDTO.builder()
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        when(bookingService.createGroup(any(), any())).thenReturn(sampleResponse());
        BookingController.GroupBookingRequest req =
                new BookingController.GroupBookingRequest(base, List.of(1L, 2L, 3L));
        mockMvc.perform(post("/api/bookings/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createGroup_emptyParticipants_shouldReturn400() throws Exception {
        BookingRequestDTO base = BookingRequestDTO.builder()
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        BookingController.GroupBookingRequest req =
                new BookingController.GroupBookingRequest(base, List.of());
        mockMvc.perform(post("/api/bookings/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
