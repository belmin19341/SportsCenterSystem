package ba.nwt.userservice.repository;

import ba.nwt.userservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%')))
           """)
    Page<User> searchByRoleAndKeyword(@Param("role") User.Role role,
                                      @Param("q") String keyword,
                                      Pageable pageable);
}

