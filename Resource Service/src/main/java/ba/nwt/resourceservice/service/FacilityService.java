package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.FacilityRequestDTO;
import ba.nwt.resourceservice.dto.FacilityResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final ModelMapper modelMapper;

    public List<FacilityResponseDTO> getAll() {
        return facilityRepository.findAll().stream()
                .map(f -> modelMapper.map(f, FacilityResponseDTO.class))
                .collect(Collectors.toList());
    }

    public FacilityResponseDTO getById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + id));
        return modelMapper.map(facility, FacilityResponseDTO.class);
    }

    public List<FacilityResponseDTO> getByType(Facility.FacilityType type) {
        return facilityRepository.findByType(type).stream()
                .map(f -> modelMapper.map(f, FacilityResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<FacilityResponseDTO> getByStatus(Facility.FacilityStatus status) {
        return facilityRepository.findByStatus(status).stream()
                .map(f -> modelMapper.map(f, FacilityResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<FacilityResponseDTO> getByOwnerId(Long ownerId) {
        return facilityRepository.findByOwnerId(ownerId).stream()
                .map(f -> modelMapper.map(f, FacilityResponseDTO.class))
                .collect(Collectors.toList());
    }

    public FacilityResponseDTO create(FacilityRequestDTO dto) {
        if (dto.getWorkingHoursEnd().isBefore(dto.getWorkingHoursStart())) {
            throw new IllegalArgumentException("Working hours end must be after working hours start");
        }

        Facility facility = Facility.builder()
                .ownerId(dto.getOwnerId())
                .name(dto.getName())
                .type(dto.getType())
                .capacity(dto.getCapacity())
                .basePricePerHour(dto.getBasePricePerHour())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .workingHoursStart(dto.getWorkingHoursStart())
                .workingHoursEnd(dto.getWorkingHoursEnd())
                .status(dto.getStatus() != null ? dto.getStatus() : Facility.FacilityStatus.ACTIVE)
                .build();

        Facility saved = facilityRepository.save(facility);
        return modelMapper.map(saved, FacilityResponseDTO.class);
    }

    public FacilityResponseDTO update(Long id, FacilityRequestDTO dto) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + id));

        if (dto.getWorkingHoursEnd().isBefore(dto.getWorkingHoursStart())) {
            throw new IllegalArgumentException("Working hours end must be after working hours start");
        }

        facility.setOwnerId(dto.getOwnerId());
        facility.setName(dto.getName());
        facility.setType(dto.getType());
        facility.setCapacity(dto.getCapacity());
        facility.setBasePricePerHour(dto.getBasePricePerHour());
        facility.setDescription(dto.getDescription());
        facility.setImageUrl(dto.getImageUrl());
        facility.setWorkingHoursStart(dto.getWorkingHoursStart());
        facility.setWorkingHoursEnd(dto.getWorkingHoursEnd());
        if (dto.getStatus() != null) {
            facility.setStatus(dto.getStatus());
        }

        Facility saved = facilityRepository.save(facility);
        return modelMapper.map(saved, FacilityResponseDTO.class);
    }

    public void delete(Long id) {
        if (!facilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Facility not found with id: " + id);
        }
        facilityRepository.deleteById(id);
    }
}

