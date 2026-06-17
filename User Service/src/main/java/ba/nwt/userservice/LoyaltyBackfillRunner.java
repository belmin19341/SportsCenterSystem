package ba.nwt.userservice;

import ba.nwt.userservice.model.UserLoyalty;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import ba.nwt.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class LoyaltyBackfillRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLoyaltyRepository userLoyaltyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long created = userRepository.findAll().stream()
            .filter(user -> userLoyaltyRepository.findByUserId(user.getId()).isEmpty())
            .peek(user -> userLoyaltyRepository.save(
                UserLoyalty.builder()
                    .user(user)
                    .totalPoints(0)
                    .tier(UserLoyalty.LoyaltyTier.BRONZE)
                    .build()
            ))
            .count();

        if (created > 0) {
            log.info(">>> LoyaltyBackfillRunner: created {} missing loyalty records.", created);
        }
    }
}
