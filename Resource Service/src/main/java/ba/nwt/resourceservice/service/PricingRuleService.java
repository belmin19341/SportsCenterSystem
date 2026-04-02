package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.PricingRuleRequestDTO;
import ba.nwt.resourceservice.dto.PricingRuleResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.model.PricingRule;
import ba.nwt.resourceservice.repository.FacilityRepository;
import ba.nwt.resourceservice.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final FacilityRepository facilityRepository;

    public List<PricingRuleResponseDTO> getAll() {
        return pricingRuleRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PricingRuleResponseDTO getById(Long id) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with id: " + id));
        return toResponseDTO(rule);
    }

    public List<PricingRuleResponseDTO> getByFacilityId(Long facilityId) {
        return pricingRuleRepository.findByFacilityId(facilityId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PricingRuleResponseDTO create(PricingRuleRequestDTO dto) {
        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + dto.getFacilityId()));

        if (dto.getTimeSlotEnd().isBefore(dto.getTimeSlotStart())) {
            throw new IllegalArgumentException("Time slot end must be after time slot start");
        }

        PricingRule rule = PricingRule.builder()
                .facility(facility)
                .timeSlotStart(dto.getTimeSlotStart())
                .timeSlotEnd(dto.getTimeSlotEnd())
                .dayOfWeek(dto.getDayOfWeek())
                .priceMultiplier(dto.getPriceMultiplier())
                .description(dto.getDescription())
                .build();

        PricingRule saved = pricingRuleRepository.save(rule);
        return toResponseDTO(saved);
    }

    public PricingRuleResponseDTO update(Long id, PricingRuleRequestDTO dto) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with id: " + id));

        Facility facility = facilityRepository.findById(dto.getFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + dto.getFacilityId()));

        if (dto.getTimeSlotEnd().isBefore(dto.getTimeSlotStart())) {
            throw new IllegalArgumentException("Time slot end must be after time slot start");
        }

        rule.setFacility(facility);
        rule.setTimeSlotStart(dto.getTimeSlotStart());
        rule.setTimeSlotEnd(dto.getTimeSlotEnd());
        rule.setDayOfWeek(dto.getDayOfWeek());
        rule.setPriceMultiplier(dto.getPriceMultiplier());
        rule.setDescription(dto.getDescription());

        PricingRule saved = pricingRuleRepository.save(rule);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!pricingRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pricing rule not found with id: " + id);
        }
        pricingRuleRepository.deleteById(id);
    }

    private PricingRuleResponseDTO toResponseDTO(PricingRule rule) {
        return PricingRuleResponseDTO.builder()
                .id(rule.getId())
                .facilityId(rule.getFacility().getId())
                .facilityName(rule.getFacility().getName())
                .timeSlotStart(rule.getTimeSlotStart())
                .timeSlotEnd(rule.getTimeSlotEnd())
                .dayOfWeek(rule.getDayOfWeek())
                .priceMultiplier(rule.getPriceMultiplier())
                .description(rule.getDescription())
                .build();
    }
}

