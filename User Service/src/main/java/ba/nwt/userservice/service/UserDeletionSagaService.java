package ba.nwt.userservice.service;

import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import ba.nwt.userservice.saga.UserDeletionSagaPublisher;
import ba.nwt.userservice.saga.event.UserBookingsCancelledEvent;
import ba.nwt.userservice.saga.event.UserBookingsCancellationFailedEvent;
import ba.nwt.userservice.saga.event.UserDeletionRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Deletion Saga — User Service side.
 *
 * Steps:
 *  1. initiate()      — User ACTIVE → DELETION_PENDING  [local TX 1]
 *                       → publish UserDeletionRequestedEvent
 *  2. finalizeDelete()— User DELETION_PENDING → DELETED  [final state]
 *  3. restoreUser()   — User DELETION_PENDING → ACTIVE   [compensating / inverse of TX1]
 */
@Service
@RequiredArgsConstructor
public class UserDeletionSagaService {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionSagaService.class);

    private final UserRepository userRepository;
    private final UserDeletionSagaPublisher publisher;

    /** Local TX 1: mark user as DELETION_PENDING and trigger cascade booking cancellation. */
    @Transactional
    public void initiate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found id=" + userId));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE users can be deleted via saga (current: " + user.getStatus() + ")");
        }

        String sagaId = UUID.randomUUID().toString();
        user.setStatus(User.UserStatus.DELETION_PENDING);
        userRepository.save(user);
        log.info("[SAGA-USER][{}] Local TX 1 — User id={} → DELETION_PENDING", sagaId, userId);

        publisher.publishUserDeletionRequested(UserDeletionRequestedEvent.builder()
                .sagaId(sagaId)
                .userId(userId)
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /** Triggered by UserBookingsCancelledEvent — user is permanently DELETED (final state). */
    @Transactional
    public void finalizeDelete(UserBookingsCancelledEvent event) {
        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found id=" + event.getUserId()));

        if (user.getStatus() != User.UserStatus.DELETION_PENDING) {
            log.warn("[SAGA-USER][{}] finalizeDelete skipped — user {} already in {}", event.getSagaId(), event.getUserId(), user.getStatus());
            return;
        }
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        log.info("[SAGA-USER][{}] User id={} DELETED — saga COMPLETE ({} bookings cancelled)", event.getSagaId(), event.getUserId(), event.getCancelledCount());
    }

    /**
     * Triggered by UserBookingsCancellationFailedEvent — compensating action.
     * Restores user from DELETION_PENDING back to ACTIVE.
     */
    @Transactional
    public void restoreUser(UserBookingsCancellationFailedEvent event) {
        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found id=" + event.getUserId()));

        if (user.getStatus() != User.UserStatus.DELETION_PENDING) {
            log.warn("[SAGA-USER][{}] restoreUser skipped — user {} already in {}", event.getSagaId(), event.getUserId(), user.getStatus());
            return;
        }
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.warn("[SAGA-USER][{}] User id={} RESTORED to ACTIVE (compensating TX) — reason: {}", event.getSagaId(), event.getUserId(), event.getReason());
    }
}
