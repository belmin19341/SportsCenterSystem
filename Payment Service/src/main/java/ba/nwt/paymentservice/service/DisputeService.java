package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.config.JsonPatchUtil;
import ba.nwt.paymentservice.dto.DisputeRequestDTO;
import ba.nwt.paymentservice.dto.DisputeResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Dispute;
import ba.nwt.paymentservice.repository.DisputeRepository;
import com.github.fge.jsonpatch.JsonPatch;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final ModelMapper modelMapper;
    private final JsonPatchUtil jsonPatchUtil;

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

    @Transactional
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

    @Transactional
    public DisputeResponseDTO patch(Long id, JsonPatch patch) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + id));
        DisputeRequestDTO current = modelMapper.map(dispute, DisputeRequestDTO.class);
        DisputeRequestDTO patched = jsonPatchUtil.apply(patch, current, DisputeRequestDTO.class);
        dispute.setBookingId(patched.getBookingId());
        dispute.setRentalId(patched.getRentalId());
        dispute.setReporterId(patched.getReporterId());
        dispute.setDescription(patched.getDescription());
        dispute.setEvidenceUrl(patched.getEvidenceUrl());
        return modelMapper.map(disputeRepository.save(dispute), DisputeResponseDTO.class);
    }

    @Transactional
    public DisputeResponseDTO resolve(Long id, String resolutionNote) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id: " + id));
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setResolutionNote(resolutionNote);
        dispute.setResolvedAt(LocalDateTime.now());
        Dispute saved = disputeRepository.save(dispute);
        return modelMapper.map(saved, DisputeResponseDTO.class);
    }

    @Transactional
    public void delete(Long id) {
        if (!disputeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dispute not found with id: " + id);
        }
        disputeRepository.deleteById(id);
    }
}

