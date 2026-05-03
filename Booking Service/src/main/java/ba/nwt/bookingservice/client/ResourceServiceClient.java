package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.client.dto.FacilityView;
import ba.nwt.bookingservice.client.dto.PriceQuoteView;
import ba.nwt.bookingservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(
        name = "resource-service",
        configuration = FeignConfig.class,
        fallbackFactory = ResourceServiceClientFallback.Factory.class
)
public interface ResourceServiceClient {

    @GetMapping("/api/facilities/{id}")
    FacilityView getFacility(@PathVariable("id") Long id);

    @GetMapping("/api/pricing-rules/calculate")
    PriceQuoteView calculatePrice(
            @RequestParam("facilityId") Long facilityId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end);
}
