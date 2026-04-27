package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.client.PaymentServiceClient;
import ba.nwt.bookingservice.client.ResourceServiceClient;
import ba.nwt.bookingservice.client.UserServiceClient;
import ba.nwt.bookingservice.client.dto.FacilityView;
import ba.nwt.bookingservice.client.dto.PaymentCreateView;
import ba.nwt.bookingservice.client.dto.PaymentView;
import ba.nwt.bookingservice.client.dto.PriceQuoteView;
import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import ba.nwt.bookingservice.config.JsonPatchUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Z5 — Unit tests for synchronous orchestration in {@link BookingService#createOrchestrated}.
 * All Feign clients are mocked; verifies the business contract for every failure mode.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceZ5OrchestrationTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingUserRepository bookingUserRepository;
    @Mock private JsonPatchUtil jsonPatchUtil;
    @Mock private ResourceServiceClient resourceServiceClient;
    @Mock private PaymentServiceClient paymentServiceClient;
    @Mock private UserServiceClient userServiceClient;

    private BookingService service;
    private BookingRequestDTO request;

    @BeforeEach
    void setUp() {
        ModelMapper realMapper = new ModelMapper();
        service = new BookingService(bookingRepository, bookingUserRepository,
                realMapper, jsonPatchUtil,
                resourceServiceClient, paymentServiceClient, userServiceClient);

        request = BookingRequestDTO.builder()
                .userId(7L)
                .facilityId(42L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("999.99")) // intentionally wrong; orchestration must override
                .build();

        // Default repo behavior: persist returns the entity with an id assigned.
        // lenient() — not every test path saves a Booking.
        org.mockito.Mockito.lenient()
                .when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                    Booking b = inv.getArgument(0);
                    if (b.getId() == null) b.setId(123L);
                    return b;
                });
    }

    @Test
    void happyPath_persistsConfirmed_usesAuthoritativePrice_creditsLoyalty() {
        when(resourceServiceClient.getFacility(42L))
                .thenReturn(FacilityView.builder().id(42L).status("ACTIVE").build());
        when(resourceServiceClient.calculatePrice(eq(42L), any(), any()))
                .thenReturn(PriceQuoteView.builder().totalPrice(new BigDecimal("80.00")).build());
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(PaymentView.builder().id(500L).status("PAID").amount(new BigDecimal("80.00")).build());

        BookingResponseDTO result = service.createOrchestrated(request, "CREDIT_CARD");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("80.00");

        ArgumentCaptor<PaymentCreateView> pay = ArgumentCaptor.forClass(PaymentCreateView.class);
        verify(paymentServiceClient).createPayment(pay.capture());
        assertThat(pay.getValue().getAmount()).isEqualByComparingTo("80.00");
        assertThat(pay.getValue().getBookingId()).isEqualTo(123L);
        assertThat(pay.getValue().getPaymentMethod()).isEqualTo("CREDIT_CARD");

        verify(userServiceClient).addLoyaltyPoints(7L, 80);
        // Booking saved twice: first PENDING, then CONFIRMED.
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    @Test
    void resourceServiceUnavailable_propagates503_andDoesNotPersist() {
        when(resourceServiceClient.getFacility(42L))
                .thenThrow(new DownstreamUnavailableException("resource-service", "down"));

        assertThatThrownBy(() -> service.createOrchestrated(request, "CREDIT_CARD"))
                .isInstanceOf(DownstreamUnavailableException.class);

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentServiceClient, never()).createPayment(any());
        verify(userServiceClient, never()).addLoyaltyPoints(anyLong(), anyInt());
    }

    @Test
    void facilityInactive_throwsBadRequest_andDoesNotPersist() {
        when(resourceServiceClient.getFacility(42L))
                .thenReturn(FacilityView.builder().id(42L).status("MAINTENANCE").build());

        assertThatThrownBy(() -> service.createOrchestrated(request, "CREDIT_CARD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not bookable");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(resourceServiceClient, never()).calculatePrice(any(), any(), any());
        verify(paymentServiceClient, never()).createPayment(any());
    }

    @Test
    void paymentServiceUnavailable_marksBookingCancelled_andRethrows() {
        when(resourceServiceClient.getFacility(42L))
                .thenReturn(FacilityView.builder().id(42L).status("ACTIVE").build());
        when(resourceServiceClient.calculatePrice(eq(42L), any(), any()))
                .thenReturn(PriceQuoteView.builder().totalPrice(new BigDecimal("50.00")).build());
        when(paymentServiceClient.createPayment(any()))
                .thenThrow(new DownstreamUnavailableException("payment-service", "down"));

        // Capture status at the moment of each save (the same Booking instance is mutated).
        java.util.List<Booking.BookingStatus> statusesAtSave = new java.util.ArrayList<>();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(123L);
            statusesAtSave.add(b.getStatus());
            return b;
        });

        assertThatThrownBy(() -> service.createOrchestrated(request, "CREDIT_CARD"))
                .isInstanceOf(DownstreamUnavailableException.class);

        assertThat(statusesAtSave)
                .containsExactly(Booking.BookingStatus.PENDING, Booking.BookingStatus.CANCELLED);
        verify(userServiceClient, never()).addLoyaltyPoints(anyLong(), anyInt());
    }

    @Test
    void paymentReturnedFailedStatus_marksBookingCancelled() {
        when(resourceServiceClient.getFacility(42L))
                .thenReturn(FacilityView.builder().id(42L).status("ACTIVE").build());
        when(resourceServiceClient.calculatePrice(eq(42L), any(), any()))
                .thenReturn(PriceQuoteView.builder().totalPrice(new BigDecimal("50.00")).build());
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(PaymentView.builder().id(501L).status("FAILED").build());

        java.util.List<Booking.BookingStatus> statusesAtSave = new java.util.ArrayList<>();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) b.setId(123L);
            statusesAtSave.add(b.getStatus());
            return b;
        });

        assertThatThrownBy(() -> service.createOrchestrated(request, "CREDIT_CARD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("did not succeed");

        assertThat(statusesAtSave)
                .containsExactly(Booking.BookingStatus.PENDING, Booking.BookingStatus.CANCELLED);
        verify(userServiceClient, never()).addLoyaltyPoints(anyLong(), anyInt());
    }

    @Test
    void userServiceUnavailable_bookingStillConfirmed_loyaltyFailureSwallowed() {
        when(resourceServiceClient.getFacility(42L))
                .thenReturn(FacilityView.builder().id(42L).status("ACTIVE").build());
        when(resourceServiceClient.calculatePrice(eq(42L), any(), any()))
                .thenReturn(PriceQuoteView.builder().totalPrice(new BigDecimal("80.00")).build());
        when(paymentServiceClient.createPayment(any()))
                .thenReturn(PaymentView.builder().id(500L).status("PAID").build());
        doThrow(new DownstreamUnavailableException("user-service", "down"))
                .when(userServiceClient).addLoyaltyPoints(anyLong(), anyInt());

        // Must NOT throw — loyalty is best-effort.
        BookingResponseDTO result = service.createOrchestrated(request, "CREDIT_CARD");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        verify(userServiceClient).addLoyaltyPoints(7L, 80);
    }

    @Test
    void endBeforeStart_throwsBadRequest_beforeAnyDownstreamCall() {
        BookingRequestDTO bad = BookingRequestDTO.builder()
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(1)) // end before start
                .totalPrice(new BigDecimal("10.00"))
                .build();

        assertThatThrownBy(() -> service.createOrchestrated(bad, "CREDIT_CARD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");

        verifyNoInteractions(resourceServiceClient, paymentServiceClient, userServiceClient);
    }
}
