package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.client.dto.FacilityView;
import ba.nwt.bookingservice.client.dto.PriceQuoteView;
import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

public class ResourceServiceClientFallback implements ResourceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceClientFallback.class);
    private final Throwable cause;

    public ResourceServiceClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public FacilityView getFacility(Long id) {
        log.warn("resource-service unavailable while fetching facility {}: {}", id,
                cause == null ? "n/a" : cause.toString());
        throw new DownstreamUnavailableException("resource-service",
                "Resource service is unavailable while validating facility " + id, cause);
    }

    @Override
    public PriceQuoteView calculatePrice(Long facilityId, LocalDateTime start, LocalDateTime end) {
        log.warn("resource-service unavailable while calculating price for facility {}: {}",
                facilityId, cause == null ? "n/a" : cause.toString());
        throw new DownstreamUnavailableException("resource-service",
                "Resource service is unavailable while calculating price for facility " + facilityId, cause);
    }

    @Component
    public static class Factory implements FallbackFactory<ResourceServiceClient> {
        @Override
        public ResourceServiceClient create(Throwable cause) {
            return new ResourceServiceClientFallback(cause);
        }
    }
}
