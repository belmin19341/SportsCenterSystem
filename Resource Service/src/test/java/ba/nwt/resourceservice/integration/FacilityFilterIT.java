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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Facility filter, search, and listing endpoints.
 * Validates filtering by type, status, keyword, and pagination behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FacilityFilterIT {

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

    private void createFacility(String name, Facility.FacilityType type, Facility.FacilityStatus status) {
        restTemplate.postForObject("/api/facilities",
                FacilityRequestDTO.builder()
                        .ownerId(1L)
                        .name(name)
                        .type(type)
                        .capacity(10)
                        .basePricePerHour(new BigDecimal("50.00"))
                        .workingHoursStart(LocalTime.of(8, 0))
                        .workingHoursEnd(LocalTime.of(22, 0))
                        .status(status)
                        .build(),
                FacilityResponseDTO.class);
    }

    @Test
    void getByType_shouldReturnOnlyFacilitiesWithMatchingType() {
        createFacility("Tennis A", Facility.FacilityType.TENNIS, Facility.FacilityStatus.ACTIVE);
        createFacility("Tennis B", Facility.FacilityType.TENNIS, Facility.FacilityStatus.ACTIVE);
        createFacility("Football", Facility.FacilityType.FOOTBALL_5V5, Facility.FacilityStatus.ACTIVE);

        ResponseEntity<List<FacilityResponseDTO>> response = restTemplate.exchange(
                "/api/facilities/type/TENNIS",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<FacilityResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(f -> f.getType() == Facility.FacilityType.TENNIS);
    }

    @Test
    void getByStatus_shouldReturnOnlyFacilitiesWithMatchingStatus() {
        createFacility("Active A", Facility.FacilityType.PADEL, Facility.FacilityStatus.ACTIVE);
        createFacility("Active B", Facility.FacilityType.PADEL, Facility.FacilityStatus.ACTIVE);
        createFacility("Inactive", Facility.FacilityType.TENNIS, Facility.FacilityStatus.INACTIVE);

        ResponseEntity<List<FacilityResponseDTO>> response = restTemplate.exchange(
                "/api/facilities/status/ACTIVE",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<FacilityResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(f -> f.getStatus() == Facility.FacilityStatus.ACTIVE);
    }

    @Test
    void searchPagedByType_shouldReturnPagedResultsFilteredByType() {
        for (int i = 1; i <= 4; i++) {
            createFacility("Padel Court " + i, Facility.FacilityType.PADEL, Facility.FacilityStatus.ACTIVE);
        }
        createFacility("Football Field", Facility.FacilityType.FOOTBALL_5V5, Facility.FacilityStatus.ACTIVE);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/facilities?paged=true&type=PADEL&page=0&size=3",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(4);
        assertThat(response.getBody().get("totalPages")).isEqualTo(2);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(3);
    }

    @Test
    void searchByKeyword_shouldMatchFacilityNameCaseInsensitive() {
        createFacility("Zenith Padel Arena", Facility.FacilityType.PADEL, Facility.FacilityStatus.ACTIVE);
        createFacility("Zenith Tennis Club", Facility.FacilityType.TENNIS, Facility.FacilityStatus.ACTIVE);
        createFacility("Standard Football", Facility.FacilityType.FOOTBALL_5V5, Facility.FacilityStatus.ACTIVE);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/facilities?q=zenith&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void getAll_withoutPagingParam_shouldReturnAllFacilitiesAsList() {
        createFacility("Facility 1", Facility.FacilityType.TENNIS, Facility.FacilityStatus.ACTIVE);
        createFacility("Facility 2", Facility.FacilityType.PADEL, Facility.FacilityStatus.INACTIVE);
        createFacility("Facility 3", Facility.FacilityType.FOOTBALL_7V7, Facility.FacilityStatus.ACTIVE);

        ResponseEntity<List<FacilityResponseDTO>> response = restTemplate.exchange(
                "/api/facilities",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<FacilityResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void searchPagedByStatusAndKeyword_shouldCombineFilters() {
        createFacility("Sport Arena Active", Facility.FacilityType.TENNIS, Facility.FacilityStatus.ACTIVE);
        createFacility("Sport Zone Inactive", Facility.FacilityType.TENNIS, Facility.FacilityStatus.INACTIVE);
        createFacility("Other Active", Facility.FacilityType.PADEL, Facility.FacilityStatus.ACTIVE);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/facilities?status=ACTIVE&q=Sport&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(1);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content.get(0).get("name")).isEqualTo("Sport Arena Active");
    }
}
