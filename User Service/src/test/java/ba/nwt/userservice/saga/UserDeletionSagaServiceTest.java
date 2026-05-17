package ba.nwt.userservice.saga;

import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import ba.nwt.userservice.saga.event.UserBookingsCancelledEvent;
import ba.nwt.userservice.saga.event.UserBookingsCancellationFailedEvent;
import ba.nwt.userservice.service.UserDeletionSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeletionSagaServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserDeletionSagaPublisher publisher;
    @InjectMocks UserDeletionSagaService service;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L).username("test_user").email("test@example.com")
                .passwordHash("hash").role(User.Role.USER)
                .status(User.UserStatus.ACTIVE).build();
    }

    // ── initiate() ────────────────────────────────────────────────────────────

    @Test
    void initiate_setsUserToDeletionPending_andPublishesEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        service.initiate(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(User.UserStatus.DELETION_PENDING);

        verify(publisher).publishUserDeletionRequested(argThat(e ->
                e.getUserId().equals(1L) && "test_user".equals(e.getUsername())));
    }

    @Test
    void initiate_throwsIllegalState_whenUserNotActive() {
        activeUser.setStatus(User.UserStatus.DELETION_PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> service.initiate(1L)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void initiate_throwsResourceNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.initiate(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── finalizeDelete() ──────────────────────────────────────────────────────

    @Test
    void finalizeDelete_setsUserToDeleted_finalState() {
        activeUser.setStatus(User.UserStatus.DELETION_PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        service.finalizeDelete(UserBookingsCancelledEvent.builder()
                .sagaId("s").userId(1L).cancelledCount(3).timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(User.UserStatus.DELETED);
    }

    @Test
    void finalizeDelete_isIdempotent_whenAlreadyDeleted() {
        activeUser.setStatus(User.UserStatus.DELETED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        service.finalizeDelete(UserBookingsCancelledEvent.builder().sagaId("s").userId(1L).cancelledCount(0).build());
        verify(userRepository, never()).save(any());
    }

    // ── restoreUser() (compensating) ──────────────────────────────────────────

    @Test
    void restoreUser_compensatingAction_setsUserBackToActive() {
        activeUser.setStatus(User.UserStatus.DELETION_PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        service.restoreUser(UserBookingsCancellationFailedEvent.builder()
                .sagaId("s").userId(1L).reason("DB error").timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    void restoreUser_isIdempotent_whenAlreadyActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        service.restoreUser(UserBookingsCancellationFailedEvent.builder().sagaId("s").userId(1L).reason("dup").build());
        verify(userRepository, never()).save(any());
    }

    @Test
    void restoreUser_throwsResourceNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.restoreUser(
                UserBookingsCancellationFailedEvent.builder().sagaId("s").userId(99L).reason("x").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
