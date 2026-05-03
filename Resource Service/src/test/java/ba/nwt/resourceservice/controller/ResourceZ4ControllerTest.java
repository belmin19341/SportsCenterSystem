package ba.nwt.resourceservice.controller;

import ba.nwt.resourceservice.dto.EquipmentRequestDTO;
import ba.nwt.resourceservice.dto.EquipmentResponseDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.exception.GlobalExceptionHandler;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Equipment;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.service.EquipmentService;
import ba.nwt.resourceservice.service.FacilityService;
import ba.nwt.resourceservice.service.PricingRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
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
class ResourceZ4ControllerTest {

    @Mock private FacilityService facilityService;
    @Mock private EquipmentService equipmentService;
    @Mock private PricingRuleService pricingRuleService;

    @InjectMocks private FacilityController facilityController;
    @InjectMocks private EquipmentController equipmentController;
    @InjectMocks private PricingRuleController pricingRuleController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        facilityController, equipmentController, pricingRuleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private MockMvc pageMockMvc() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.registerModule(new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule());
        return MockMvcBuilders.standaloneSetup(facilityController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(om))
                .build();
    }

    @Test
    void searchFacilities_shouldReturnPage() throws Exception {
        FacilityResponseDTO dto = FacilityResponseDTO.builder()
                .id(1L).name("Court A").type(Facility.FacilityType.PADEL)
                .basePricePerHour(new BigDecimal("60.00")).status(Facility.FacilityStatus.ACTIVE).build();
        Page<FacilityResponseDTO> page = new PageImpl<>(List.of(dto));
        when(facilityService.search(eq(Facility.FacilityType.PADEL), isNull(), eq("court"), any(Pageable.class)))
                .thenReturn(page);

        pageMockMvc().perform(get("/api/facilities")
                        .param("type", "PADEL").param("q", "court")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Court A"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void patchFacility_shouldReturn200() throws Exception {
        FacilityResponseDTO dto = FacilityResponseDTO.builder()
                .id(1L).name("Renamed").type(Facility.FacilityType.PADEL)
                .basePricePerHour(new BigDecimal("80.00")).status(Facility.FacilityStatus.MAINTENANCE).build();
        when(facilityService.patch(eq(1L), any())).thenReturn(dto);

        String patch = "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"MAINTENANCE\"}]";

        mockMvc.perform(patch("/api/facilities/1")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void patchFacility_shouldReturn404_whenMissing() throws Exception {
        when(facilityService.patch(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Facility not found with id: 99"));

        String patch = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"X\"}]";

        mockMvc.perform(patch("/api/facilities/99")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchFacility_shouldReturn400_whenMalformedBody() throws Exception {
        mockMvc.perform(patch("/api/facilities/1")
                        .contentType("application/json-patch+json")
                        .content("nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEquipmentBatch_shouldReturn201() throws Exception {
        List<EquipmentRequestDTO> reqs = List.of(
                EquipmentRequestDTO.builder()
                        .name("Padel racket").type(Equipment.EquipmentType.RACKET)
                        .quantityTotal(10).quantityAvailable(10)
                        .pricePerDay(new BigDecimal("5.00"))
                        .depositRequired(new BigDecimal("20.00"))
                        .build(),
                EquipmentRequestDTO.builder()
                        .name("Tennis ball pack").type(Equipment.EquipmentType.BALL)
                        .quantityTotal(50).quantityAvailable(50)
                        .pricePerDay(new BigDecimal("1.00"))
                        .depositRequired(new BigDecimal("0.00"))
                        .build());
        when(equipmentService.createBatch(anyList())).thenReturn(List.of(
                EquipmentResponseDTO.builder().id(1L).name("Padel racket").build(),
                EquipmentResponseDTO.builder().id(2L).name("Tennis ball pack").build()));

        mockMvc.perform(post("/api/equipment/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqs)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createEquipmentBatch_shouldReturn400_whenEmpty() throws Exception {
        when(equipmentService.createBatch(anyList()))
                .thenThrow(new IllegalArgumentException("Batch must contain at least one equipment item"));

        mockMvc.perform(post("/api/equipment/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculatePrice_shouldReturnQuote() throws Exception {
        PricingRuleService.PriceQuote quote = PricingRuleService.PriceQuote.builder()
                .facilityId(1L)
                .start(LocalDateTime.parse("2025-06-02T18:00:00"))
                .end(LocalDateTime.parse("2025-06-02T20:00:00"))
                .basePricePerHour(new BigDecimal("60.00"))
                .hours(new BigDecimal("2.0000"))
                .multiplier(new BigDecimal("1.20"))
                .totalPrice(new BigDecimal("144.00"))
                .build();
        when(pricingRuleService.calculatePrice(eq(1L), any(), any())).thenReturn(quote);

        mockMvc.perform(get("/api/pricing-rules/calculate")
                        .param("facilityId", "1")
                        .param("start", "2025-06-02T18:00:00")
                        .param("end", "2025-06-02T20:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(144.00))
                .andExpect(jsonPath("$.multiplier").value(1.20));
    }

    @Test
    void calculatePrice_shouldReturn400_whenEndBeforeStart() throws Exception {
        when(pricingRuleService.calculatePrice(eq(1L), any(), any()))
                .thenThrow(new IllegalArgumentException("End must be after start"));

        mockMvc.perform(get("/api/pricing-rules/calculate")
                        .param("facilityId", "1")
                        .param("start", "2025-06-02T20:00:00")
                        .param("end", "2025-06-02T18:00:00"))
                .andExpect(status().isBadRequest());
    }
}
