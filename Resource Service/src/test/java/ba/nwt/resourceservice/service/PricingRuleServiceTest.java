package ba.nwt.resourceservice.service;

import ba.nwt.resourceservice.exception.ResourceNotFoundException;
import ba.nwt.resourceservice.model.Facility;
import ba.nwt.resourceservice.model.PricingRule;
import ba.nwt.resourceservice.repository.FacilityRepository;
import ba.nwt.resourceservice.repository.PricingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceTest {

    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private FacilityRepository facilityRepository;

    @InjectMocks private PricingRuleService service;

    private Facility facility;

    @BeforeEach
    void setUp() {
        facility = Facility.builder()
                .id(1L).name("Court A").type(Facility.FacilityType.PADEL)
                .basePricePerHour(new BigDecimal("60.00")).build();
    }

    @Test
    void calculatePrice_noRules_returnsBasePrice() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(pricingRuleRepository.findByFacilityId(1L)).thenReturn(List.of());

        PricingRuleService.PriceQuote q = service.calculatePrice(
                1L,
                LocalDateTime.parse("2025-06-02T18:00:00"),
                LocalDateTime.parse("2025-06-02T20:00:00"));

        assertThat(q.getMultiplier()).isEqualByComparingTo("1.00");
        assertThat(q.getTotalPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void calculatePrice_appliesMatchingPeakRule() {
        PricingRule peak = PricingRule.builder()
                .facility(facility)
                .timeSlotStart(LocalTime.of(17, 0))
                .timeSlotEnd(LocalTime.of(22, 0))
                .dayOfWeek(PricingRule.DayOfWeekEnum.MONDAY)
                .priceMultiplier(new BigDecimal("1.20"))
                .build();
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(pricingRuleRepository.findByFacilityId(1L)).thenReturn(List.of(peak));

        // 2025-06-02 is a Monday
        PricingRuleService.PriceQuote q = service.calculatePrice(
                1L,
                LocalDateTime.parse("2025-06-02T18:00:00"),
                LocalDateTime.parse("2025-06-02T20:00:00"));

        assertThat(q.getMultiplier()).isEqualByComparingTo("1.20");
        assertThat(q.getTotalPrice()).isEqualByComparingTo("144.00");
    }

    @Test
    void calculatePrice_facilityMissing_throws() {
        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculatePrice(
                99L,
                LocalDateTime.parse("2025-06-02T18:00:00"),
                LocalDateTime.parse("2025-06-02T20:00:00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculatePrice_invalidWindow_throws() {
        assertThatThrownBy(() -> service.calculatePrice(
                1L,
                LocalDateTime.parse("2025-06-02T20:00:00"),
                LocalDateTime.parse("2025-06-02T18:00:00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
