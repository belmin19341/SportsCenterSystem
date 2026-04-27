package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.client.dto.PaymentCreateView;
import ba.nwt.bookingservice.client.dto.PaymentView;
import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

public class PaymentServiceClientFallback implements PaymentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceClientFallback.class);
    private final Throwable cause;

    public PaymentServiceClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public PaymentView createPayment(PaymentCreateView request) {
        log.warn("payment-service unavailable while creating payment for booking {}: {}",
                request == null ? null : request.getBookingId(),
                cause == null ? "n/a" : cause.toString());
        throw new DownstreamUnavailableException("payment-service",
                "Payment service is unavailable; booking cannot be charged at this time.", cause);
    }

    @Component
    public static class Factory implements FallbackFactory<PaymentServiceClient> {
        @Override
        public PaymentServiceClient create(Throwable cause) {
            return new PaymentServiceClientFallback(cause);
        }
    }
}
