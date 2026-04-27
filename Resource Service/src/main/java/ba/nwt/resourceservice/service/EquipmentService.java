package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.EquipmentRequestDTO;
import ba.nwt.resourceservice.dto.EquipmentResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Equipment;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.repository.EquipmentRepository;
import ba.nwt.resourceservice.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final FacilityRepository facilityRepository;

    public List<EquipmentResponseDTO> getAll() {
        return equipmentRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EquipmentResponseDTO getById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));
        return toResponseDTO(equipment);
    }

    public List<EquipmentResponseDTO> getByFacilityId(Long facilityId) {
        return equipmentRepository.findByFacilityId(facilityId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> getByType(Equipment.EquipmentType type) {
        return equipmentRepository.findByType(type).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EquipmentResponseDTO create(EquipmentRequestDTO dto) {
        Facility facility = null;
        if (dto.getFacilityId() != null) {
            facility = facilityRepository.findById(dto.getFacilityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + dto.getFacilityId()));
        }

        if (dto.getQuantityAvailable() > dto.getQuantityTotal()) {
            throw new IllegalArgumentException("Available quantity cannot exceed total quantity");
        }

        Equipment equipment = Equipment.builder()
                .facility(facility)
                .name(dto.getName())
                .type(dto.getType())
                .category(dto.getCategory())
                .quantityTotal(dto.getQuantityTotal())
                .quantityAvailable(dto.getQuantityAvailable())
                .pricePerDay(dto.getPricePerDay())
                .equipmentCondition(dto.getEquipmentCondition() != null ? dto.getEquipmentCondition() : Equipment.EquipmentCondition.NEW)
                .depositRequired(dto.getDepositRequired())
                .build();

        Equipment saved = equipmentRepository.save(equipment);
        return toResponseDTO(saved);
    }

    public EquipmentResponseDTO update(Long id, EquipmentRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));

        if (dto.getFacilityId() != null) {
            Facility facility = facilityRepository.findById(dto.getFacilityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + dto.getFacilityId()));
            equipment.setFacility(facility);
        }

        if (dto.getQuantityAvailable() > dto.getQuantityTotal()) {
            throw new IllegalArgumentException("Available quantity cannot exceed total quantity");
        }

        equipment.setName(dto.getName());
        equipment.setType(dto.getType());
        equipment.setCategory(dto.getCategory());
        equipment.setQuantityTotal(dto.getQuantityTotal());
        equipment.setQuantityAvailable(dto.getQuantityAvailable());
        equipment.setPricePerDay(dto.getPricePerDay());
        if (dto.getEquipmentCondition() != null) {
            equipment.setEquipmentCondition(dto.getEquipmentCondition());
        }
        equipment.setDepositRequired(dto.getDepositRequired());

        Equipment saved = equipmentRepository.save(equipment);
        return toResponseDTO(saved);
    }

    @Transactional
    public List<EquipmentResponseDTO> createBatch(List<EquipmentRequestDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Batch must contain at least one equipment item");
        }
        List<Equipment> entities = new ArrayList<>(dtos.size());
        for (EquipmentRequestDTO dto : dtos) {
            Facility facility = null;
            if (dto.getFacilityId() != null) {
                facility = facilityRepository.findById(dto.getFacilityId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Facility not found with id: " + dto.getFacilityId()));
            }
            if (dto.getQuantityAvailable() > dto.getQuantityTotal()) {
                throw new IllegalArgumentException("Available quantity cannot exceed total quantity");
            }
            entities.add(Equipment.builder()
                    .facility(facility)
                    .name(dto.getName())
                    .type(dto.getType())
                    .category(dto.getCategory())
                    .quantityTotal(dto.getQuantityTotal())
                    .quantityAvailable(dto.getQuantityAvailable())
                    .pricePerDay(dto.getPricePerDay())
                    .equipmentCondition(dto.getEquipmentCondition() != null
                            ? dto.getEquipmentCondition() : Equipment.EquipmentCondition.NEW)
                    .depositRequired(dto.getDepositRequired())
                    .build());
        }
        return equipmentRepository.saveAll(entities).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Equipment not found with id: " + id);
        }
        equipmentRepository.deleteById(id);
    }

    private EquipmentResponseDTO toResponseDTO(Equipment eq) {
        return EquipmentResponseDTO.builder()
                .id(eq.getId())
                .facilityId(eq.getFacility() != null ? eq.getFacility().getId() : null)
                .facilityName(eq.getFacility() != null ? eq.getFacility().getName() : null)
                .name(eq.getName())
                .type(eq.getType())
                .category(eq.getCategory())
                .quantityTotal(eq.getQuantityTotal())
                .quantityAvailable(eq.getQuantityAvailable())
                .pricePerDay(eq.getPricePerDay())
                .equipmentCondition(eq.getEquipmentCondition())
                .depositRequired(eq.getDepositRequired())
                .rentalCount(eq.getRentalCount())
                .lastMaintenance(eq.getLastMaintenance())
                .createdAt(eq.getCreatedAt())
                .build();
    }
}

