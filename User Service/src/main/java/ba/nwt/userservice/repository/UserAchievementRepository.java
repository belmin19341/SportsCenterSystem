package ba.nwt.userservice.repository;

import ba.nwt.userservice.model.UserAchievement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @EntityGraph(attributePaths = {"user", "achievement"})
    List<UserAchievement> findByUserId(Long userId);
}
