package ba.nwt.resourceservice;

import ba.nwt.resourceservice.model.*;
import ba.nwt.resourceservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final FacilityRepository facilityRepository;
    private final EquipmentRepository equipmentRepository;
    private final PricingRuleRepository pricingRuleRepository;

    @Override
    public void run(String... args) {
        if (facilityRepository.count() > 0) {
            log.info(">>> Podaci već postoje, preskačem DataLoader.");
            return;
        }

        log.info(">>> Unosim početne podatke u Resource Service bazu...");

        // ── Tereni ──
        Facility football5 = facilityRepository.save(Facility.builder()
                .ownerId(2L)
                .name("Mali teren A")
                .type(Facility.FacilityType.FOOTBALL_5V5)
                .capacity(10)
                .basePricePerHour(new BigDecimal("60.00"))
                .description("Mali fudbalski teren sa umjetnom travom, osvjetljenje")
                .workingHoursStart(LocalTime.of(8, 0))
                .workingHoursEnd(LocalTime.of(23, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build());

        Facility football7 = facilityRepository.save(Facility.builder()
                .ownerId(2L)
                .name("Veliki teren B")
                .type(Facility.FacilityType.FOOTBALL_7V7)
                .capacity(14)
                .basePricePerHour(new BigDecimal("100.00"))
                .description("Veliki fudbalski teren 7v7 sa tribinama")
                .workingHoursStart(LocalTime.of(9, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build());

        Facility padel = facilityRepository.save(Facility.builder()
                .ownerId(2L)
                .name("Padel Court 1")
                .type(Facility.FacilityType.PADEL)
                .capacity(4)
                .basePricePerHour(new BigDecimal("40.00"))
                .description("Indoor padel teren sa LED osvjetljenjem")
                .workingHoursStart(LocalTime.of(7, 0))
                .workingHoursEnd(LocalTime.of(22, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build());

        Facility tennis = facilityRepository.save(Facility.builder()
                .ownerId(2L)
                .name("Teniski teren 1")
                .type(Facility.FacilityType.TENNIS)
                .capacity(4)
                .basePricePerHour(new BigDecimal("35.00"))
                .description("Outdoor teniski teren — zemlja")
                .workingHoursStart(LocalTime.of(7, 0))
                .workingHoursEnd(LocalTime.of(21, 0))
                .status(Facility.FacilityStatus.ACTIVE)
                .build());

        // ── Oprema ──
        equipmentRepository.save(Equipment.builder()
                .facility(football5)
                .name("Nike Fudbalska lopta")
                .type(Equipment.EquipmentType.BALL)
                .category("Fudbal")
                .quantityTotal(10)
                .quantityAvailable(8)
                .pricePerDay(new BigDecimal("5.00"))
                .equipmentCondition(Equipment.EquipmentCondition.GOOD)
                .depositRequired(new BigDecimal("10.00"))
                .rentalCount(25)
                .build());

        equipmentRepository.save(Equipment.builder()
                .facility(padel)
                .name("Bullpadel Vertex reket")
                .type(Equipment.EquipmentType.RACKET)
                .category("Padel")
                .quantityTotal(6)
                .quantityAvailable(4)
                .pricePerDay(new BigDecimal("15.00"))
                .equipmentCondition(Equipment.EquipmentCondition.NEW)
                .depositRequired(new BigDecimal("50.00"))
                .rentalCount(12)
                .lastMaintenance(LocalDate.now().minusDays(15))
                .build());

        equipmentRepository.save(Equipment.builder()
                .facility(tennis)
                .name("Wilson teniski reket")
                .type(Equipment.EquipmentType.RACKET)
                .category("Tenis")
                .quantityTotal(8)
                .quantityAvailable(6)
                .pricePerDay(new BigDecimal("10.00"))
                .equipmentCondition(Equipment.EquipmentCondition.GOOD)
                .depositRequired(new BigDecimal("30.00"))
                .rentalCount(40)
                .build());

        equipmentRepository.save(Equipment.builder()
                .name("Sobni bicikl")
                .type(Equipment.EquipmentType.BICYCLE)
                .category("Fitness")
                .quantityTotal(5)
                .quantityAvailable(5)
                .pricePerDay(new BigDecimal("20.00"))
                .equipmentCondition(Equipment.EquipmentCondition.NEW)
                .depositRequired(new BigDecimal("100.00"))
                .rentalCount(0)
                .build());

        // ── Pricing Rules ──
        pricingRuleRepository.save(PricingRule.builder()
                .facility(football5)
                .timeSlotStart(LocalTime.of(18, 0))
                .timeSlotEnd(LocalTime.of(22, 0))
                .dayOfWeek(PricingRule.DayOfWeekEnum.SATURDAY)
                .priceMultiplier(new BigDecimal("1.50"))
                .description("Vikend večernji termin — 50% premija")
                .build());

        pricingRuleRepository.save(PricingRule.builder()
                .facility(football5)
                .timeSlotStart(LocalTime.of(8, 0))
                .timeSlotEnd(LocalTime.of(14, 0))
                .priceMultiplier(new BigDecimal("0.80"))
                .description("Jutarnji popust — 20% manje")
                .build());

        pricingRuleRepository.save(PricingRule.builder()
                .facility(padel)
                .timeSlotStart(LocalTime.of(17, 0))
                .timeSlotEnd(LocalTime.of(21, 0))
                .dayOfWeek(PricingRule.DayOfWeekEnum.SUNDAY)
                .priceMultiplier(new BigDecimal("1.30"))
                .description("Nedjeljni popodnevni slot — 30% premija")
                .build());

        log.info(">>> Resource Service DataLoader završen — {} terena, {} oprema, {} pricing pravila.",
                facilityRepository.count(), equipmentRepository.count(), pricingRuleRepository.count());
    }
}

