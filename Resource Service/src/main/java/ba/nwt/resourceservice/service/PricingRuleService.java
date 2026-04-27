package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.dto.PricingRuleRequestDTO;
import ba.nwt.resourceservice.dto.PricingRuleResponseDTO;
import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.model.PricingRule;
import ba.nwt.resourceservice.repository.FacilityRepository;
import ba.nwt.resourceservice.repository.PricingRuleRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /**
     * Calculates the rental price for a facility over a time window by
     * combining the facility's base price with any applicable pricing rules
     * (matching day-of-week and overlapping time slot). Spans two repos in
     * a single read-only transaction.
     */
    @Transactional(readOnly = true)
    public PriceQuote calculatePrice(Long facilityId, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        BigDecimal hours = BigDecimal.valueOf(Duration.between(start, end).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal basePrice = facility.getBasePricePerHour().multiply(hours);

        PricingRule.DayOfWeekEnum dow = PricingRule.DayOfWeekEnum.valueOf(start.getDayOfWeek().name());
        LocalTime startT = start.toLocalTime();
        LocalTime endT = end.toLocalTime();

        BigDecimal multiplier = pricingRuleRepository.findByFacilityId(facilityId).stream()
                .filter(r -> r.getDayOfWeek() == null || r.getDayOfWeek() == dow)
                .filter(r -> startT.isBefore(r.getTimeSlotEnd()) && endT.isAfter(r.getTimeSlotStart()))
                .map(PricingRule::getPriceMultiplier)
                .reduce(BigDecimal.ONE, BigDecimal::multiply);

        BigDecimal total = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return PriceQuote.builder()
                .facilityId(facilityId)
                .start(start).end(end)
                .basePricePerHour(facility.getBasePricePerHour())
                .hours(hours)
                .multiplier(multiplier)
                .totalPrice(total)
                .build();
    }

    @Data @Builder
    public static class PriceQuote {
        private Long facilityId;
        private LocalDateTime start;
        private LocalDateTime end;
        private BigDecimal basePricePerHour;
        private BigDecimal hours;
        private BigDecimal multiplier;
        private BigDecimal totalPrice;
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

