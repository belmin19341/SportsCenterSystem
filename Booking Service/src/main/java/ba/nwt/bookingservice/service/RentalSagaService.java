package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.EquipmentRentalRequestDTO;
import ba.nwt.bookingservice.dto.EquipmentRentalResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.EquipmentRental;
import ba.nwt.bookingservice.repository.EquipmentRentalRepository;
import ba.nwt.bookingservice.saga.RentalSagaPublisher;
import ba.nwt.bookingservice.saga.event.RentalCreatedEvent;
import ba.nwt.bookingservice.saga.event.RentalPaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.RentalPaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Equipment Rental Saga — Booking Service side.
 *
 * Steps:
 *  1. initiate()      — EquipmentRental saved as RESERVED  [local TX 1]
 *                       → publish RentalCreatedEvent
 *  2. confirmRental() — rental stays RESERVED (payment confirmed) [final state]
 *  3. cancelRental()  — rental → CANCELLED  [compensating / inverse of TX1]
 */
@Service
@RequiredArgsConstructor
public class RentalSagaService {

    private static final Logger log = LoggerFactory.getLogger(RentalSagaService.class);

    private final EquipmentRentalRepository rentalRepository;
    private final RentalSagaPublisher publisher;
    private final ModelMapper modelMapper;

    @Transactional
    public EquipmentRentalResponseDTO initiate(EquipmentRentalRequestDTO dto, String paymentMethod, boolean simulateFailure) {
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        String sagaId = UUID.randomUUID().toString();

        EquipmentRental rental = rentalRepository.save(EquipmentRental.builder()
                .userId(dto.getUserId())
                .equipmentId(dto.getEquipmentId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1)
                .totalPrice(dto.getTotalPrice())
                .depositPaid(dto.getDepositPaid() != null ? dto.getDepositPaid() : java.math.BigDecimal.ZERO)
                .status(EquipmentRental.RentalStatus.RESERVED)
                .build());

        log.info("[SAGA-RENTAL][{}] Local TX 1 — Rental id={} saved as RESERVED", sagaId, rental.getId());

        publisher.publishRentalCreated(RentalCreatedEvent.builder()
                .sagaId(sagaId)
                .rentalId(rental.getId())
                .userId(rental.getUserId())
                .equipmentId(rental.getEquipmentId())
                .startDate(rental.getStartDate())
                .endDate(rental.getEndDate())
                .quantity(rental.getQuantity())
                .totalPrice(rental.getTotalPrice())
                .depositAmount(rental.getDepositPaid())
                .paymentMethod(paymentMethod == null ? "CREDIT_CARD" : paymentMethod)
                .simulateFailure(simulateFailure)
                .timestamp(LocalDateTime.now())
                .build());

        return modelMapper.map(rental, EquipmentRentalResponseDTO.class);
    }

    /** Payment succeeded — rental is confirmed (stays RESERVED, payment done). */
    @Transactional
    public void confirmRental(RentalPaymentCompletedEvent event) {
        EquipmentRental rental = rentalRepository.findById(event.getRentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found id=" + event.getRentalId()));

        if (rental.getStatus() != EquipmentRental.RentalStatus.RESERVED) {
            log.warn("[SAGA-RENTAL][{}] confirmRental skipped — rental {} already in {}", event.getSagaId(), event.getRentalId(), rental.getStatus());
            return;
        }
        // Rental stays RESERVED — payment confirmed, ready for pickup. This is the final state.
        rentalRepository.save(rental);
        log.info("[SAGA-RENTAL][{}] Rental id={} CONFIRMED — saga COMPLETE (paymentId={} txn={})", event.getSagaId(), event.getRentalId(), event.getPaymentId(), event.getTransactionId());
    }

    /** Payment failed — compensating action: cancel the rental. */
    @Transactional
    public void cancelRental(RentalPaymentFailedEvent event) {
        EquipmentRental rental = rentalRepository.findById(event.getRentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found id=" + event.getRentalId()));

        if (rental.getStatus() != EquipmentRental.RentalStatus.RESERVED) {
            log.warn("[SAGA-RENTAL][{}] cancelRental skipped — rental {} already in {}", event.getSagaId(), event.getRentalId(), rental.getStatus());
            return;
        }
        rental.setStatus(EquipmentRental.RentalStatus.CANCELLED);
        rentalRepository.save(rental);
        log.warn("[SAGA-RENTAL][{}] Rental id={} CANCELLED (compensating TX) — reason: {}", event.getSagaId(), event.getRentalId(), event.getReason());
    }
}
