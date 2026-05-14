package ba.nwt.resourceservice.integration;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.EquipmentRepository;
import ba.nwt.resourceservice.repository.FacilityRepository;
import ba.nwt.resourceservice.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Facility CRUD operations.
 * Full Spring context with H2 in-memory database — no mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FacilityCrudIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @BeforeEach
    void cleanUp() {
        pricingRuleRepository.deleteAll();
        equipmentRepository.deleteAll();
        facilityRepository.deleteAll();
    }

    private FacilityRequestDTO buildRequest(String name, Facility.FacilityType type) {
        return FacilityRequestDTO.builder()
                .ownerId(1L)
                .name(name)
                .type(type)
                .capacity(10)
                .basePricePerHour(new BigDecimal("50.00"))
                .description("Test facility")
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build();
    }

    @Test
    void createFacility_shouldReturn201AndPersistToDatabase() {
        FacilityRequestDTO request = buildRequest("Tennis Court A", Facility.FacilityType.TENNIS);

        ResponseEntity<FacilityResponseDTO> response =
                restTemplate.postForEntity("/api/facilities", request, FacilityResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getName()).isEqualTo("Tennis Court A");
        assertThat(response.getBody().getType()).isEqualTo(Facility.FacilityType.TENNIS);
        assertThat(response.getBody().getStatus()).isEqualTo(Facility.FacilityStatus.ACTIVE);
        assertThat(response.getBody().getBasePricePerHour()).isEqualByComparingTo("50.00");
        assertThat(facilityRepository.count()).isEqualTo(1);
    }

    @Test
    void getFacilityById_shouldReturn200WithPersistedData() {
        FacilityResponseDTO created = restTemplate.postForObject(
                "/api/facilities", buildRequest("Padel Court 1", Facility.FacilityType.PADEL), FacilityResponseDTO.class);

        ResponseEntity<FacilityResponseDTO> response =
                restTemplate.getForEntity("/api/facilities/" + created.getId(), FacilityResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Padel Court 1");
        assertThat(response.getBody().getType()).isEqualTo(Facility.FacilityType.PADEL);
        assertThat(response.getBody().getOwnerId()).isEqualTo(1L);
    }

    @Test
    void updateFacility_shouldReturn200WithModifiedData() {
        FacilityResponseDTO created = restTemplate.postForObject(
                "/api/facilities", buildRequest("Old Name", Facility.FacilityType.FOOTBALL_5V5), FacilityResponseDTO.class);

        FacilityRequestDTO updateRequest = FacilityRequestDTO.builder()
                .ownerId(2L)
                .name("New Name")
                .type(Facility.FacilityType.FOOTBALL_7V7)
                .capacity(14)
                .basePricePerHour(new BigDecimal("80.00"))
                .workingHoursStart(LocalTime.of(9, 0))
                .workingHoursEnd(LocalTime.of(23, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build();

        ResponseEntity<FacilityResponseDTO> response = restTemplate.exchange(
                "/api/facilities/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                FacilityResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("New Name");
        assertThat(response.getBody().getType()).isEqualTo(Facility.FacilityType.FOOTBALL_7V7);
        assertThat(response.getBody().getCapacity()).isEqualTo(14);
        assertThat(response.getBody().getBasePricePerHour()).isEqualByComparingTo("80.00");
    }

    @Test
    void deleteFacility_shouldReturn204AndSubsequentGetReturns404() {
        FacilityResponseDTO created = restTemplate.postForObject(
                "/api/facilities", buildRequest("To Delete", Facility.FacilityType.TENNIS), FacilityResponseDTO.class);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/facilities/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse =
                restTemplate.getForEntity("/api/facilities/" + created.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(facilityRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void getFacility_shouldReturn404_whenIdDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/facilities/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getFacilitiesByOwner_shouldReturnOnlyOwnersFacilities() {
        restTemplate.postForObject("/api/facilities",
                FacilityRequestDTO.builder().ownerId(10L).name("Owner10 Court").type(Facility.FacilityType.PADEL)
                        .capacity(4).basePricePerHour(new BigDecimal("40.00"))
                        .workingHoursStart(LocalTime.of(8, 0)).workingHoursEnd(LocalTime.of(22, 0)).build(),
                FacilityResponseDTO.class);
        restTemplate.postForObject("/api/facilities",
                FacilityRequestDTO.builder().ownerId(99L).name("Other Court").type(Facility.FacilityType.TENNIS)
                        .capacity(4).basePricePerHour(new BigDecimal("35.00"))
                        .workingHoursStart(LocalTime.of(8, 0)).workingHoursEnd(LocalTime.of(22, 0)).build(),
                FacilityResponseDTO.class);

        ResponseEntity<FacilityResponseDTO[]> response =
                restTemplate.getForEntity("/api/facilities/owner/10", FacilityResponseDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getName()).isEqualTo("Owner10 Court");
    }
}
