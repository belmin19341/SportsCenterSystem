package ba.nwt.resourceservice.integration;

import ba.nwt.resourceservice.dto.EquipmentRequestDTO;
import ba.nwt.resourceservice.dto.EquipmentResponseDTO;
import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.model.Equipment;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: kreiranje terena sa opremom.
 * Testira vezu između terena i opreme, batch kreiranje,
 * promjenu statusa terena i pretragu opreme po tipu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FacilityEquipmentFlowIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private FacilityRepository facilityRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;

    @BeforeEach
    void cleanUp() {
        pricingRuleRepository.deleteAll();
        equipmentRepository.deleteAll();
        facilityRepository.deleteAll();
    }

    private FacilityResponseDTO createFacility(String name, Facility.FacilityType type) {
        return restTemplate.postForObject("/api/facilities",
                FacilityRequestDTO.builder()
                        .ownerId(1L).name(name).type(type).capacity(10)
                        .basePricePerHour(new BigDecimal("50.00"))
                        .workingHoursStart(LocalTime.of(8, 0))
                        .workingHoursEnd(LocalTime.of(22, 0))
                        .status(Facility.FacilityStatus.ACTIVE).build(),
                FacilityResponseDTO.class);
    }

    private EquipmentResponseDTO addEquipment(Long facilityId, String name, Equipment.EquipmentType type) {
        return restTemplate.postForObject("/api/equipment",
                EquipmentRequestDTO.builder()
                        .facilityId(facilityId).name(name).type(type)
                        .quantityTotal(5).quantityAvailable(5)
                        .pricePerDay(new BigDecimal("10.00"))
                        .equipmentCondition(Equipment.EquipmentCondition.NEW)
                        .build(),
                EquipmentResponseDTO.class);
    }

    @Test
    void createFacilityWithEquipment_equipmentLinkedToFacility() {
        FacilityResponseDTO facility = createFacility("Tennis Club", Facility.FacilityType.TENNIS);

        EquipmentResponseDTO equipment = addEquipment(facility.getId(), "Wilson reket", Equipment.EquipmentType.RACKET);

        assertThat(equipment.getFacilityId()).isEqualTo(facility.getId());
        assertThat(equipment.getName()).isEqualTo("Wilson reket");

        // Provjeri link kroz GET /api/equipment/facility/{id}
        ResponseEntity<List<EquipmentResponseDTO>> resp = restTemplate.exchange(
                "/api/equipment/facility/" + facility.getId(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EquipmentResponseDTO>>() {});
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getFacilityId()).isEqualTo(facility.getId());
    }

    @Test
    void addMultipleEquipmentItemsToFacility_allRetrievableByFacilityId() {
        FacilityResponseDTO facility = createFacility("Padel Arena", Facility.FacilityType.PADEL);

        addEquipment(facility.getId(), "Bullpadel reket 1", Equipment.EquipmentType.RACKET);
        addEquipment(facility.getId(), "Bullpadel reket 2", Equipment.EquipmentType.RACKET);
        addEquipment(facility.getId(), "Padel lopta", Equipment.EquipmentType.BALL);

        ResponseEntity<List<EquipmentResponseDTO>> resp = restTemplate.exchange(
                "/api/equipment/facility/" + facility.getId(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EquipmentResponseDTO>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(3);
        assertThat(resp.getBody()).allMatch(e -> e.getFacilityId().equals(facility.getId()));
    }

    @Test
    void batchEquipmentCreation_allItemsCreatedAtomically() {
        FacilityResponseDTO facility = createFacility("Fitness Centar", Facility.FacilityType.TABLE_TENNIS);

        List<EquipmentRequestDTO> batch = List.of(
                EquipmentRequestDTO.builder().facilityId(facility.getId()).name("Bicikl 1")
                        .type(Equipment.EquipmentType.BICYCLE).quantityTotal(3).quantityAvailable(3)
                        .pricePerDay(new BigDecimal("15.00")).equipmentCondition(Equipment.EquipmentCondition.NEW).build(),
                EquipmentRequestDTO.builder().facilityId(facility.getId()).name("Bicikl 2")
                        .type(Equipment.EquipmentType.BICYCLE).quantityTotal(2).quantityAvailable(2)
                        .pricePerDay(new BigDecimal("15.00")).equipmentCondition(Equipment.EquipmentCondition.GOOD).build(),
                EquipmentRequestDTO.builder().facilityId(facility.getId()).name("Trenažer")
                        .type(Equipment.EquipmentType.FITNESS).quantityTotal(1).quantityAvailable(1)
                        .pricePerDay(new BigDecimal("20.00")).equipmentCondition(Equipment.EquipmentCondition.NEW).build()
        );

        ResponseEntity<List<EquipmentResponseDTO>> resp = restTemplate.exchange(
                "/api/equipment/batch", HttpMethod.POST,
                new HttpEntity<>(batch),
                new ParameterizedTypeReference<List<EquipmentResponseDTO>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).hasSize(3);
        assertThat(equipmentRepository.count()).isEqualTo(3);
        assertThat(resp.getBody()).allMatch(e -> e.getFacilityId().equals(facility.getId()));
    }

    @Test
    void updateFacilityStatusToMaintenance_shouldPersistAndBeReflectedInDatabase() {
        FacilityResponseDTO facility = createFacility("Teren na renoviranju", Facility.FacilityType.FOOTBALL_5V5);
        assertThat(facility.getStatus()).isEqualTo(Facility.FacilityStatus.ACTIVE);

        FacilityRequestDTO updateReq = FacilityRequestDTO.builder()
                .ownerId(1L).name(facility.getName()).type(facility.getType())
                .capacity(10).basePricePerHour(new BigDecimal("50.00"))
                .workingHoursStart(LocalTime.of(8, 0)).workingHoursEnd(LocalTime.of(22, 0))
                .status(Facility.FacilityStatus.MAINTENANCE).build();

        ResponseEntity<FacilityResponseDTO> updated = restTemplate.exchange(
                "/api/facilities/" + facility.getId(), HttpMethod.PUT,
                new HttpEntity<>(updateReq), FacilityResponseDTO.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().getStatus()).isEqualTo(Facility.FacilityStatus.MAINTENANCE);
        assertThat(facilityRepository.findById(facility.getId()).get().getStatus())
                .isEqualTo(Facility.FacilityStatus.MAINTENANCE);
    }

    @Test
    void getEquipmentByType_shouldReturnOnlyMatchingType() {
        FacilityResponseDTO facility = createFacility("Multi Sport", Facility.FacilityType.TENNIS);

        addEquipment(facility.getId(), "Reket A", Equipment.EquipmentType.RACKET);
        addEquipment(facility.getId(), "Reket B", Equipment.EquipmentType.RACKET);
        addEquipment(facility.getId(), "Lopta",   Equipment.EquipmentType.BALL);

        ResponseEntity<List<EquipmentResponseDTO>> resp = restTemplate.exchange(
                "/api/equipment/type/RACKET", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EquipmentResponseDTO>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody()).allMatch(e -> e.getType() == Equipment.EquipmentType.RACKET);
    }
}
