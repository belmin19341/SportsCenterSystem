package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.EquipmentRequestDTO;
import ba.nwt.resourceservice.dto.EquipmentResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Equipment;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.EquipmentRepository;
import ba.nwt.resourceservice.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    private Equipment equipment;
    private Facility facility;

    @BeforeEach
    void setUp() {
        facility = Facility.builder().id(1L).name("Mali teren A").build();
        equipment = Equipment.builder()
                .id(1L).facility(facility).name("Fudbalska lopta")
                .type(Equipment.EquipmentType.BALL).quantityTotal(10).quantityAvailable(8)
                .pricePerDay(new BigDecimal("5.00"))
                .equipmentCondition(Equipment.EquipmentCondition.GOOD).build();
    }

    @Test
    void getAll_shouldReturnList() {
        when(equipmentRepository.findAll()).thenReturn(List.of(equipment));

        List<EquipmentResponseDTO> result = equipmentService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Fudbalska lopta");
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldThrowWhenAvailableExceedsTotal() {
        EquipmentRequestDTO request = EquipmentRequestDTO.builder()
                .facilityId(1L).name("Test").type(Equipment.EquipmentType.BALL)
                .quantityTotal(5).quantityAvailable(10)
                .pricePerDay(new BigDecimal("5.00")).build();

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> equipmentService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available quantity cannot exceed total quantity");
    }

    @Test
    void create_shouldCreateEquipment() {
        EquipmentRequestDTO request = EquipmentRequestDTO.builder()
                .facilityId(1L).name("Fudbalska lopta").type(Equipment.EquipmentType.BALL)
                .quantityTotal(10).quantityAvailable(8)
                .pricePerDay(new BigDecimal("5.00")).build();

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(equipment);

        EquipmentResponseDTO result = equipmentService.create(request);

        assertThat(result).isNotNull();
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void delete_shouldThrowNotFound() {
        when(equipmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> equipmentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

