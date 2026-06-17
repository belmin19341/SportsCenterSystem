package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.client.UserServiceClient;
import ba.nwt.paymentservice.config.JsonPatchUtil;
import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private JsonPatchUtil jsonPatchUtil;
    @Mock private UserServiceClient userServiceClient;
    @Mock private StripeGateway stripeGateway;
    @Mock private SavedCardService savedCardService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private PaymentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(1L).bookingId(1L).amount(new BigDecimal("60.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-12345678")
                .status(Payment.PaymentStatus.PAID).build();

        responseDTO = PaymentResponseDTO.builder()
                .id(1L).bookingId(1L).amount(new BigDecimal("60.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-12345678")
                .status(Payment.PaymentStatus.PAID).build();
    }

    @Test
    void getAll_shouldReturnList() {
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(modelMapper.map(any(Payment.class), eq(PaymentResponseDTO.class))).thenReturn(responseDTO);

        List<PaymentResponseDTO> result = paymentService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void getById_shouldReturnPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, PaymentResponseDTO.class)).thenReturn(responseDTO);

        PaymentResponseDTO result = paymentService.getById(1L);

        assertThat(result.getTransactionId()).isEqualTo("TXN-12345678");
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldCreatePayment() {
        // userId is null → skip user validation
        // paymentMethod = CREDIT_CARD, no stripe token, no saved card
        // stripeGateway.isEnabled() = false → fallback auto-approve
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .bookingId(1L).amount(new BigDecimal("60.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD).build();

        when(stripeGateway.isEnabled()).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(modelMapper.map(any(Payment.class), eq(PaymentResponseDTO.class))).thenReturn(responseDTO);

        PaymentResponseDTO result = paymentService.create(request);

        assertThat(result).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void create_cash_shouldAutoApproveWithoutStripe() {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .bookingId(2L).amount(new BigDecimal("30.00"))
                .paymentMethod(Payment.PaymentMethod.CASH).build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(modelMapper.map(any(Payment.class), eq(PaymentResponseDTO.class))).thenReturn(responseDTO);

        PaymentResponseDTO result = paymentService.create(request);

        assertThat(result).isNotNull();
        // CASH path never touches Stripe
        verifyNoInteractions(stripeGateway);
    }

    @Test
    void delete_shouldThrowNotFound() {
        when(paymentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
