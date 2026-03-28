package ba.nwt.resourceservice.repository;

import ba.nwt.resourceservice.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByFacilityId(Long facilityId);
    List<Equipment> findByCategory(String category);
    List<Equipment> findByType(Equipment.EquipmentType type);
}

