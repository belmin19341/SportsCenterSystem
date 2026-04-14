package ba.nwt.resourceservice.controller;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.exception.GlobalExceptionHandler;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.service.FacilityService;
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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FacilityControllerTest {

    @Mock
    private FacilityService facilityService;

    @InjectMocks
    private FacilityController facilityController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(facilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        FacilityResponseDTO dto = FacilityResponseDTO.builder()
                .id(1L).name("Mali teren A").type(Facility.FacilityType.FOOTBALL_5V5)
                .basePricePerHour(new BigDecimal("60.00")).status(Facility.FacilityStatus.ACTIVE).build();

        when(facilityService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mali teren A"));
    }

    @Test
    void getById_shouldReturn404() throws Exception {
        when(facilityService.getById(99L)).thenThrow(new ResourceNotFoundException("Facility not found with id: 99"));

        mockMvc.perform(get("/api/facilities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Facility not found with id: 99"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(2L).name("Novi teren").type(Facility.FacilityType.PADEL)
                .capacity(4).basePricePerHour(new BigDecimal("40.00"))
                .workingHoursStart(LocalTime.of(7, 0))
                .workingHoursEnd(LocalTime.of(22, 0)).build();

        FacilityResponseDTO response = FacilityResponseDTO.builder()
                .id(5L).name("Novi teren").type(Facility.FacilityType.PADEL)
                .status(Facility.FacilityStatus.ACTIVE).build();

        when(facilityService.create(any(FacilityRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novi teren"));
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        String invalidJson = "{\"name\": \"\"}";

        mockMvc.perform(post("/api/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/facilities/1"))
                .andExpect(status().isNoContent());
    }
}
