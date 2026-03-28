package ba.nwt.userservice;

import ba.nwt.userservice.model.*;
import ba.nwt.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLoyaltyRepository userLoyaltyRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info(">>> Podaci već postoje, preskačem DataLoader.");
            return;
        }

        log.info(">>> Unosim početne podatke u User Service bazu...");

        // ── Korisnici ──
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@sportcenter.ba")
                .passwordHash("$2a$10$hashed_admin_password")
                .role(User.Role.ADMIN)
                .phone("+38761000001")
                .build());

        User owner = userRepository.save(User.builder()
                .username("vlasnik_teren")
                .email("vlasnik@sportcenter.ba")
                .passwordHash("$2a$10$hashed_owner_password")
                .role(User.Role.OWNER)
                .phone("+38761000002")
                .build());

        User user1 = userRepository.save(User.builder()
                .username("belmin_d")
                .email("belmin@example.com")
                .passwordHash("$2a$10$hashed_user1_password")
                .role(User.Role.USER)
                .phone("+38762111111")
                .build());

        User user2 = userRepository.save(User.builder()
                .username("harun_g")
                .email("harun@example.com")
                .passwordHash("$2a$10$hashed_user2_password")
                .role(User.Role.USER)
                .phone("+38762222222")
                .build());

        User user3 = userRepository.save(User.builder()
                .username("amar_h")
                .email("amar@example.com")
                .passwordHash("$2a$10$hashed_user3_password")
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
                .name("Prva rezervacija")
                .description("Napravi svoju prvu rezervaciju terena")
                .badgeIcon("🏅")
                .category(Achievement.AchievementCategory.BOOKING)
                .unlockCriteriaType("BOOKING_COUNT")
                .unlockCriteriaValue(1)
                .build());

        Achievement tenBookings = achievementRepository.save(Achievement.builder()
                .name("Redovni igrač")
                .description("Napravi 10 rezervacija")
                .badgeIcon("⭐")
                .category(Achievement.AchievementCategory.MILESTONE)
                .unlockCriteriaType("BOOKING_COUNT")
                .unlockCriteriaValue(10)
                .build());

        Achievement firstRental = achievementRepository.save(Achievement.builder()
                .name("Oprema spremna")
                .description("Iznajmi opremu prvi put")
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

        log.info(">>> User Service DataLoader završen — uneseno {} korisnika, {} loyalty zapisa, {} achievementa.",
                userRepository.count(), userLoyaltyRepository.count(), achievementRepository.count());
    }
}

