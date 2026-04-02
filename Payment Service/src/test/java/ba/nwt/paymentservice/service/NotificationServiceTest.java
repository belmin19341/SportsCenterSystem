package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.NotificationRequestDTO;
import ba.nwt.paymentservice.dto.NotificationResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private NotificationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L).userId(3L).type(Notification.NotificationType.BOOKING_CONFIRMATION)
                .subject("Test").message("Test message").isRead(false).build();

        responseDTO = NotificationResponseDTO.builder()
                .id(1L).userId(3L).type(Notification.NotificationType.BOOKING_CONFIRMATION)
                .subject("Test").message("Test message").isRead(false).build();
    }

    @Test
    void getByUserId_shouldReturnList() {
        when(notificationRepository.findByUserId(3L)).thenReturn(List.of(notification));
        when(modelMapper.map(any(Notification.class), eq(NotificationResponseDTO.class))).thenReturn(responseDTO);

        List<NotificationResponseDTO> result = notificationService.getByUserId(3L);

        assertThat(result).hasSize(1);
    }

    @Test
    void markAsRead_shouldUpdateNotification() {
        NotificationResponseDTO readDTO = NotificationResponseDTO.builder()
                .id(1L).userId(3L).isRead(true).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(modelMapper.map(any(Notification.class), eq(NotificationResponseDTO.class))).thenReturn(readDTO);

        NotificationResponseDTO result = notificationService.markAsRead(1L);

        assertThat(result.getIsRead()).isTrue();
    }

    @Test
    void markAsRead_shouldThrowNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

