package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.dto.EquipmentRentalRequestDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.EquipmentRental;
import ba.nwt.bookingservice.repository.EquipmentRentalRepository;
import ba.nwt.bookingservice.saga.event.RentalPaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.RentalPaymentFailedEvent;
import ba.nwt.bookingservice.service.RentalSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalSagaServiceTest {

    @Mock EquipmentRentalRepository rentalRepository;
    @Mock RentalSagaPublisher publisher;
    @Mock ModelMapper modelMapper;
    @InjectMocks RentalSagaService service;

    private EquipmentRentalRequestDTO validRequest;
    private EquipmentRental savedRental;

    @BeforeEach
    void setUp() {
        validRequest = EquipmentRentalRequestDTO.builder()
                .userId(1L).equipmentId(5L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .quantity(1)
                .totalPrice(new BigDecimal("50.00"))
                .build();

        savedRental = EquipmentRental.builder()
                .id(20L).userId(1L).equipmentId(5L)
                .startDate(validRequest.getStartDate())
                .endDate(validRequest.getEndDate())
                .quantity(1)
                .totalPrice(new BigDecimal("50.00"))
                .status(EquipmentRental.RentalStatus.RESERVED)
                .build();
    }

    // ── initiate() ────────────────────────────────────────────────────────────

    @Test
    void initiate_savesRentalAsReserved_andPublishesEvent() {
        when(rentalRepository.save(any())).thenReturn(savedRental);
        when(modelMapper.map(any(), any())).thenReturn(null);

        service.initiate(validRequest, "CREDIT_CARD", false);

        ArgumentCaptor<EquipmentRental> captor = ArgumentCaptor.forClass(EquipmentRental.class);
        verify(rentalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EquipmentRental.RentalStatus.RESERVED);

        verify(publisher).publishRentalCreated(argThat(e ->
                e.getRentalId().equals(20L) && !e.isSimulateFailure()));
    }

    @Test
    void initiate_withSimulateFailure_passesFlag() {
        when(rentalRepository.save(any())).thenReturn(savedRental);
        when(modelMapper.map(any(), any())).thenReturn(null);

        service.initiate(validRequest, "DEBIT_CARD", true);

        verify(publisher).publishRentalCreated(argThat(e -> e.isSimulateFailure()));
    }

    @Test
    void initiate_throwsIllegalArgument_whenEndDateBeforeStartDate() {
        validRequest.setEndDate(validRequest.getStartDate().minusDays(1));
        assertThatThrownBy(() -> service.initiate(validRequest, "CREDIT_CARD", false))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(publisher);
    }

    // ── confirmRental() ───────────────────────────────────────────────────────

    @Test
    void confirmRental_finalState_rentalStaysReserved() {
        when(rentalRepository.findById(20L)).thenReturn(Optional.of(savedRental));
        when(rentalRepository.save(any())).thenReturn(savedRental);

        service.confirmRental(RentalPaymentCompletedEvent.builder()
                .sagaId("s").rentalId(20L).paymentId(1L)
                .transactionId("TXN-RENTAL-ABC").amount(BigDecimal.TEN).timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<EquipmentRental> captor = ArgumentCaptor.forClass(EquipmentRental.class);
        verify(rentalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EquipmentRental.RentalStatus.RESERVED);
    }

    @Test
    void confirmRental_isIdempotent_whenAlreadyActive() {
        savedRental.setStatus(EquipmentRental.RentalStatus.ACTIVE);
        when(rentalRepository.findById(20L)).thenReturn(Optional.of(savedRental));

        service.confirmRental(RentalPaymentCompletedEvent.builder().sagaId("s").rentalId(20L).build());
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void confirmRental_throwsResourceNotFound_whenRentalMissing() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmRental(
                RentalPaymentCompletedEvent.builder().sagaId("s").rentalId(99L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancelRental() (compensating) ─────────────────────────────────────────

    @Test
    void cancelRental_compensatingAction_setsRentalToCancelled() {
        when(rentalRepository.findById(20L)).thenReturn(Optional.of(savedRental));
        when(rentalRepository.save(any())).thenReturn(savedRental);

        service.cancelRental(RentalPaymentFailedEvent.builder()
                .sagaId("s").rentalId(20L).reason("card declined").timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<EquipmentRental> captor = ArgumentCaptor.forClass(EquipmentRental.class);
        verify(rentalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EquipmentRental.RentalStatus.CANCELLED);
    }

    @Test
    void cancelRental_isIdempotent_whenAlreadyCancelled() {
        savedRental.setStatus(EquipmentRental.RentalStatus.CANCELLED);
        when(rentalRepository.findById(20L)).thenReturn(Optional.of(savedRental));

        service.cancelRental(RentalPaymentFailedEvent.builder().sagaId("s").rentalId(20L).reason("dup").build());
        verify(rentalRepository, never()).save(any());
    }
}
