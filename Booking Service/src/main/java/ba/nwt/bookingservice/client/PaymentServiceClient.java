package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.client.dto.PaymentCreateView;
import ba.nwt.bookingservice.client.dto.PaymentView;
import ba.nwt.bookingservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service",
        configuration = FeignConfig.class,
        fallbackFactory = PaymentServiceClientFallback.Factory.class
)
public interface PaymentServiceClient {

    @PostMapping("/api/payments")
    PaymentView createPayment(@RequestBody PaymentCreateView request);
}
