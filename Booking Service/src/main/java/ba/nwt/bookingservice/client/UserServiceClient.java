package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class,
        fallbackFactory = UserServiceClientFallback.Factory.class
)
public interface UserServiceClient {

    @PatchMapping("/api/loyalty/user/{userId}/add-points")
    void addLoyaltyPoints(@PathVariable("userId") Long userId,
                          @RequestParam("points") int points);
}
