package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.client.UserServiceClient;
import ba.nwt.paymentservice.config.JsonPatchUtil;
import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.dto.RevenueReportDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import com.github.fge.jsonpatch.JsonPatch;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final JsonPatchUtil jsonPatchUtil;
    private final UserServiceClient userServiceClient;
    private final StripeGateway stripeGateway;
    private final SavedCardService savedCardService;

    public List<PaymentResponseDTO> getAll() {
        return paymentRepository.findAll().stream()
                .map(p -> modelMapper.map(p, PaymentResponseDTO.class))
                .collect(Collectors.toList());
    }

    public Page<PaymentResponseDTO> search(Payment.PaymentStatus status,
                                           Payment.PaymentMethod method,
                                           Long bookingId,
                                           BigDecimal minAmount, BigDecimal maxAmount,
                                           LocalDateTime from, LocalDateTime to,
                                           Pageable pageable) {
        return paymentRepository.search(status, method, bookingId, minAmount, maxAmount, from, to, pageable)
                .map(p -> modelMapper.map(p, PaymentResponseDTO.class));
    }

    public PaymentResponseDTO getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return modelMapper.map(payment, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(p -> modelMapper.map(p, PaymentResponseDTO.class))
                .collect(Collectors.toList());
    }

    public PaymentResponseDTO getByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transactionId: " + transactionId));
        return modelMapper.map(payment, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(p -> modelMapper.map(p, PaymentResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponseDTO create(PaymentRequestDTO dto) {
        // Synchronous validation: confirm the paying user exists before persisting anything.
        // User not found (404) → DownstreamBadRequestException → 400
        // User Service down   → DownstreamUnavailableException → 503
        if (dto.getUserId() != null) {
            userServiceClient.getUser(dto.getUserId());
        }

        Payment.PaymentStatus status;
        String transactionId;

        Payment.PaymentMethod method = dto.getPaymentMethod();

        if (method == Payment.PaymentMethod.CASH) {
            transactionId = "CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            status = Payment.PaymentStatus.PAID;

        } else if (method == Payment.PaymentMethod.PAYPAL || !stripeGateway.isEnabled()) {
            // PAYPAL has no integration; also fallback when Stripe key not configured
            transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            status = Payment.PaymentStatus.PAID;

        } else if (dto.getSavedCardId() != null) {
            // Use a previously saved card
            try {
                var card = savedCardService.getById(dto.getSavedCardId());
                var result = stripeGateway.chargeCustomer(dto.getAmount(), card.getStripeCustomerId());
                transactionId = result.chargeId();
                status = Payment.PaymentStatus.PAID;
            } catch (StripeGateway.StripeChargeException e) {
                transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                status = Payment.PaymentStatus.FAILED;
            }

        } else if (dto.getStripeToken() != null) {
            // New card provided by frontend via Stripe.js
            try {
                if (Boolean.TRUE.equals(dto.getSaveCard()) && dto.getUserId() != null) {
                    var result = stripeGateway.createCustomerAndCharge(dto.getAmount(), dto.getStripeToken());
                    transactionId = result.chargeId();
                    status = Payment.PaymentStatus.PAID;
                    savedCardService.save(dto.getUserId(), dto.getCardLast4(), dto.getCardBrand(), result.customerId());
                } else {
                    var result = stripeGateway.chargeWithToken(dto.getAmount(), dto.getStripeToken());
                    transactionId = result.chargeId();
                    status = Payment.PaymentStatus.PAID;
                }
            } catch (StripeGateway.StripeChargeException e) {
                transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                status = Payment.PaymentStatus.FAILED;
            }

        } else {
            // No token, no saved card — backward-compat auto-approve
            transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            status = Payment.PaymentStatus.PAID;
        }

        Payment payment = Payment.builder()
                .bookingId(dto.getBookingId())
                .rentalId(dto.getRentalId())
                .amount(dto.getAmount())
                .depositAmount(dto.getDepositAmount())
                .paymentMethod(dto.getPaymentMethod())
                .transactionId(transactionId)
                .status(status)
                .build();

        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment saved = paymentRepository.save(payment);
        return modelMapper.map(saved, PaymentResponseDTO.class);
    }

    @Transactional
    public PaymentResponseDTO update(Long id, PaymentRequestDTO dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        payment.setBookingId(dto.getBookingId());
        payment.setRentalId(dto.getRentalId());
        payment.setAmount(dto.getAmount());
        payment.setDepositAmount(dto.getDepositAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getStatus() != null) {
            payment.setStatus(dto.getStatus());
            if (dto.getStatus() == Payment.PaymentStatus.PAID && payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
        }

        Payment saved = paymentRepository.save(payment);
        return modelMapper.map(saved, PaymentResponseDTO.class);
    }

    @Transactional
    public PaymentResponseDTO patch(Long id, JsonPatch patch) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        PaymentRequestDTO current = modelMapper.map(payment, PaymentRequestDTO.class);
        PaymentRequestDTO patched = jsonPatchUtil.apply(patch, current, PaymentRequestDTO.class);

        payment.setBookingId(patched.getBookingId());
        payment.setRentalId(patched.getRentalId());
        payment.setAmount(patched.getAmount());
        payment.setDepositAmount(patched.getDepositAmount());
        payment.setPaymentMethod(patched.getPaymentMethod());
        if (patched.getStatus() != null) {
            payment.setStatus(patched.getStatus());
            if (patched.getStatus() == Payment.PaymentStatus.PAID && payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
        }
        return modelMapper.map(paymentRepository.save(payment), PaymentResponseDTO.class);
    }

    /**
     * Atomically refunds a PAID payment and notifies the user (if linked to a booking we
     * can derive a recipient). The whole operation is rolled back if either step fails.
     */
    @Transactional
    public PaymentResponseDTO refund(Long id, Long recipientUserId, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Only PAID payments can be refunded (current: " + payment.getStatus() + ")");
        }
        if (recipientUserId == null) {
            throw new IllegalArgumentException("recipientUserId is required to notify the payer");
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);

        notificationRepository.save(Notification.builder()
                .userId(recipientUserId)
                .type(Notification.NotificationType.PAYMENT_RECEIPT)
                .subject("Refund processed for payment " + savedPayment.getTransactionId())
                .message("Your payment of " + savedPayment.getAmount() +
                         " has been refunded. Reason: " + (reason == null ? "n/a" : reason))
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build());

        return modelMapper.map(savedPayment, PaymentResponseDTO.class);
    }

    public RevenueReportDTO getRevenueBetween(LocalDateTime from, LocalDateTime to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'to' must be after 'from'");
        }
        BigDecimal total = paymentRepository.sumRevenueBetween(from, to);
        List<RevenueReportDTO.RevenueByMethodDTO> byMethod = new ArrayList<>();
        for (Object[] row : paymentRepository.revenueByMethodBetween(from, to)) {
            byMethod.add(RevenueReportDTO.RevenueByMethodDTO.builder()
                    .method((Payment.PaymentMethod) row[0])
                    .total((BigDecimal) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }
        return RevenueReportDTO.builder()
                .from(from).to(to)
                .totalRevenue(total == null ? BigDecimal.ZERO : total)
                .byMethod(byMethod)
                .build();
    }

    @Transactional
    public void delete(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
    }

}


