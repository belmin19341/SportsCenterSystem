package ba.nwt.resourceservice.repository;

import ba.nwt.resourceservice.model.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByType(Facility.FacilityType type);
    List<Facility> findByStatus(Facility.FacilityStatus status);
    List<Facility> findByOwnerId(Long ownerId);

    @Query("""
           SELECT f FROM Facility f
           WHERE (:type IS NULL OR f.type = :type)
             AND (:status IS NULL OR f.status = :status)
             AND (:q IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%')))
           """)
    Page<Facility> search(@Param("type") Facility.FacilityType type,
                          @Param("status") Facility.FacilityStatus status,
                          @Param("q") String q,
                          Pageable pageable);
}

