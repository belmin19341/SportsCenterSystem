package ba.nwt.paymentservice.repository;

import ba.nwt.paymentservice.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByReporterId(Long reporterId);
    List<Dispute> findByStatus(Dispute.DisputeStatus status);
}

