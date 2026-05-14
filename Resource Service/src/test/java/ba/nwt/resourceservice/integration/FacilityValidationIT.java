package ba.nwt.resourceservice.integration;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.EquipmentRepository;
import ba.nwt.resourceservice.repository.FacilityRepository;
import ba.nwt.resourceservice.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Facility input validation and business rules.
 * Verifies that invalid requests are rejected before reaching the database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FacilityValidationIT {

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

    @Test
    void createFacility_shouldReturn400_whenWorkingHoursEndIsBeforeStart() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(1L)
                .name("Bad Hours Facility")
                .type(Facility.FacilityType.TENNIS)
                .capacity(4)
                .basePricePerHour(new BigDecimal("30.00"))
                .workingHoursStart(LocalTime.of(22, 0))
                .workingHoursEnd(LocalTime.of(8, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }

    @Test
    void createFacility_shouldReturn400_whenNameIsMissing() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(1L)
                .type(Facility.FacilityType.TENNIS)
                .capacity(4)
                .basePricePerHour(new BigDecimal("30.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }

    @Test
    void createFacility_shouldReturn400_whenPriceIsBelowMinimum() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(1L)
                .name("Free Facility")
                .type(Facility.FacilityType.PADEL)
                .capacity(4)
                .basePricePerHour(new BigDecimal("0.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }

    @Test
    void createFacility_shouldReturn400_whenOwnerIdIsMissing() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .name("Orphan Facility")
                .type(Facility.FacilityType.TABLE_TENNIS)
                .capacity(2)
                .basePricePerHour(new BigDecimal("20.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }

    @Test
    void createFacility_shouldReturn400_whenTypeIsMissing() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(1L)
                .name("Typeless Facility")
                .capacity(4)
                .basePricePerHour(new BigDecimal("25.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }

    @Test
    void createFacility_shouldReturn400_whenCapacityIsZero() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(1L)
                .name("Zero Capacity Facility")
                .type(Facility.FacilityType.TENNIS)
                .capacity(0)
                .basePricePerHour(new BigDecimal("30.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/facilities", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(facilityRepository.count()).isZero();
    }
}
