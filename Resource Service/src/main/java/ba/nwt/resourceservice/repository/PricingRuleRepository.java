package ba.nwt.resourceservice.repository;

import ba.nwt.resourceservice.model.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByFacilityId(Long facilityId);
}

