package ba.nwt.userservice;

import ba.nwt.userservice.model.*;
import ba.nwt.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLoyaltyRepository userLoyaltyRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info(">>> Data already exists, skipping DataLoader.");
            return;
        }

        log.info(">>> Inserting initial data into User Service database...");

        // ── Seed user for JWT authentication testing ──
        User testUser = userRepository.save(User.builder()
                .username("john_doe")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .phone("+38761234567")
                .build());

        // ── Users ──
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@sportcenter.ba")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.ADMIN)
                .phone("+38761000001")
                .build());

        User owner = userRepository.save(User.builder()
                .username("court_owner")
                .email("owner@sportcenter.ba")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.OWNER)
                .phone("+38761000002")
                .build());

        User user1 = userRepository.save(User.builder()
                .username("belmin_d")
                .email("belmin@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .phone("+38762111111")
                .build());

        User user2 = userRepository.save(User.builder()
                .username("harun_g")
                .email("harun@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .phone("+38762222222")
                .build());

        User user3 = userRepository.save(User.builder()
                .username("amar_h")
                .email("amar@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .build());

        // ── Loyalty ──
        userLoyaltyRepository.save(UserLoyalty.builder()
                .user(user1).totalPoints(250).tier(UserLoyalty.LoyaltyTier.SILVER)
                .tierAchievedAt(LocalDateTime.now().minusDays(30)).build());

        userLoyaltyRepository.save(UserLoyalty.builder()
                .user(user2).totalPoints(50).tier(UserLoyalty.LoyaltyTier.BRONZE).build());

        userLoyaltyRepository.save(UserLoyalty.builder()
                .user(user3).totalPoints(0).tier(UserLoyalty.LoyaltyTier.BRONZE).build());

        // ── Achievements ──
        Achievement firstBooking = achievementRepository.save(Achievement.builder()
                .name("First Booking")
                .description("Make your first court booking")
                .badgeIcon("🏅")
                .category(Achievement.AchievementCategory.BOOKING)
                .unlockCriteriaType("BOOKING_COUNT")
                .unlockCriteriaValue(1)
                .build());

        Achievement tenBookings = achievementRepository.save(Achievement.builder()
                .name("Regular Player")
                .description("Make 10 bookings")
                .badgeIcon("⭐")
                .category(Achievement.AchievementCategory.MILESTONE)
                .unlockCriteriaType("BOOKING_COUNT")
                .unlockCriteriaValue(10)
                .build());

        Achievement firstRental = achievementRepository.save(Achievement.builder()
                .name("Gear Up")
                .description("Rent equipment for the first time")
                .badgeIcon("🎾")
                .category(Achievement.AchievementCategory.RENTAL)
                .unlockCriteriaType("RENTAL_COUNT")
                .unlockCriteriaValue(1)
                .build());

        // ── User Achievements ──
        userAchievementRepository.save(UserAchievement.builder()
                .user(user1).achievement(firstBooking).build());

        userAchievementRepository.save(UserAchievement.builder()
                .user(user1).achievement(tenBookings).build());

        userAchievementRepository.save(UserAchievement.builder()
                .user(user2).achievement(firstBooking).build());

        log.info(">>> User Service DataLoader finished — {} users, {} loyalty records, {} achievements.",
                userRepository.count(), userLoyaltyRepository.count(), achievementRepository.count());
    }
}
