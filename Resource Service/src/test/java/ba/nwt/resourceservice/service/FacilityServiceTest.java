package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private FacilityService facilityService;

    private Facility facility;
    private FacilityResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        facility = Facility.builder()
                .id(1L).ownerId(2L).name("Mali teren A")
                .type(Facility.FacilityType.FOOTBALL_5V5).capacity(10)
                .basePricePerHour(new BigDecimal("60.00"))
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(23, 0))
                .status(Facility.FacilityStatus.ACTIVE).build();

        responseDTO = FacilityResponseDTO.builder()
                .id(1L).ownerId(2L).name("Mali teren A")
                .type(Facility.FacilityType.FOOTBALL_5V5)
                .basePricePerHour(new BigDecimal("60.00"))
                .status(Facility.FacilityStatus.ACTIVE).build();
    }

    @Test
    void getAll_shouldReturnList() {
        when(facilityRepository.findAll()).thenReturn(List.of(facility));
        when(modelMapper.map(any(Facility.class), eq(FacilityResponseDTO.class))).thenReturn(responseDTO);

        List<FacilityResponseDTO> result = facilityService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mali teren A");
    }

    @Test
    void getById_shouldReturnFacility() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(modelMapper.map(facility, FacilityResponseDTO.class)).thenReturn(responseDTO);

        FacilityResponseDTO result = facilityService.getById(1L);

        assertThat(result.getName()).isEqualTo("Mali teren A");
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldCreateFacility() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(2L).name("Novi teren")
                .type(Facility.FacilityType.PADEL).capacity(4)
                .basePricePerHour(new BigDecimal("40.00"))
                .workingHoursStart(LocalTime.of(7, 0))
                .workingHoursEnd(LocalTime.of(22, 0)).build();

        when(facilityRepository.save(any(Facility.class))).thenReturn(facility);
        when(modelMapper.map(any(Facility.class), eq(FacilityResponseDTO.class))).thenReturn(responseDTO);

        FacilityResponseDTO result = facilityService.create(request);

        assertThat(result).isNotNull();
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void create_shouldThrowWhenInvalidHours() {
        FacilityRequestDTO request = FacilityRequestDTO.builder()
                .ownerId(2L).name("Novi teren")
                .type(Facility.FacilityType.PADEL)
                .basePricePerHour(new BigDecimal("40.00"))
                .workingHoursStart(LocalTime.of(22, 0))
                .workingHoursEnd(LocalTime.of(7, 0)).build();

        assertThatThrownBy(() -> facilityService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Working hours end must be after working hours start");
    }

    @Test
    void delete_shouldDeleteExisting() {
        when(facilityRepository.existsById(1L)).thenReturn(true);

        facilityService.delete(1L);

        verify(facilityRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound() {
        when(facilityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> facilityService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

