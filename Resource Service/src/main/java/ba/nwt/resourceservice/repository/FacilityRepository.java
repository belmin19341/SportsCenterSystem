package ba.nwt.resourceservice.repository;

import ba.nwt.resourceservice.model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByType(Facility.FacilityType type);
    List<Facility> findByStatus(Facility.FacilityStatus status);
    List<Facility> findByOwnerId(Long ownerId);
}

