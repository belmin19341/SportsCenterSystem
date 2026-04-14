package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.DisputeRequestDTO;
import ba.nwt.paymentservice.dto.DisputeResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Dispute;
import ba.nwt.paymentservice.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final ModelMapper modelMapper;

    public List<DisputeResponseDTO> getAll() {
        return disputeRepository.findAll().stream()
                .map(d -> modelMapper.map(d, DisputeResponseDTO.class))
                .collect(Collectors.toList());
    }

    public DisputeResponseDTO getById(Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + id));
        return modelMapper.map(dispute, DisputeResponseDTO.class);
    }

    public List<DisputeResponseDTO> getByReporterId(Long reporterId) {
        return disputeRepository.findByReporterId(reporterId).stream()
                .map(d -> modelMapper.map(d, DisputeResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<DisputeResponseDTO> getByStatus(Dispute.DisputeStatus status) {
        return disputeRepository.findByStatus(status).stream()
                .map(d -> modelMapper.map(d, DisputeResponseDTO.class))
                .collect(Collectors.toList());
    }

    public DisputeResponseDTO create(DisputeRequestDTO dto) {
        Dispute dispute = Dispute.builder()
                .bookingId(dto.getBookingId())
                .rentalId(dto.getRentalId())
                .reporterId(dto.getReporterId())
                .description(dto.getDescription())
                .evidenceUrl(dto.getEvidenceUrl())
                .status(Dispute.DisputeStatus.OPEN)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        return modelMapper.map(saved, DisputeResponseDTO.class);
    }

    public DisputeResponseDTO resolve(Long id, String resolutionNote) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + id));
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setResolutionNote(resolutionNote);
        dispute.setResolvedAt(LocalDateTime.now());
        Dispute saved = disputeRepository.save(dispute);
        return modelMapper.map(saved, DisputeResponseDTO.class);
    }

    public void delete(Long id) {
        if (!disputeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dispute not found with id: " + id);
        }
        disputeRepository.deleteById(id);
    }
}

