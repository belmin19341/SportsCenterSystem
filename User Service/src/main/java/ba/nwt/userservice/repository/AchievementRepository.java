package ba.nwt.userservice.repository;

import ba.nwt.userservice.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByCategory(Achievement.AchievementCategory category);
}

