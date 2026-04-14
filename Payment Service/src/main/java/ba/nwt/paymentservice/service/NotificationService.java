package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.NotificationRequestDTO;
import ba.nwt.paymentservice.dto.NotificationResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    public List<NotificationResponseDTO> getAll() {
        return notificationRepository.findAll().stream()
                .map(n -> modelMapper.map(n, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    public NotificationResponseDTO getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return modelMapper.map(notification, NotificationResponseDTO.class);
    }

    public List<NotificationResponseDTO> getByUserId(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(n -> modelMapper.map(n, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<NotificationResponseDTO> getUnreadByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId).stream()
                .map(n -> modelMapper.map(n, NotificationResponseDTO.class))
                .collect(Collectors.toList());
    }

    public NotificationResponseDTO create(NotificationRequestDTO dto) {
        Notification notification = Notification.builder()
                .userId(dto.getUserId())
                .type(dto.getType())
                .subject(dto.getSubject())
                .message(dto.getMessage())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        return modelMapper.map(saved, NotificationResponseDTO.class);
    }

    public NotificationResponseDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return modelMapper.map(saved, NotificationResponseDTO.class);
    }

    public void delete(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }
}

