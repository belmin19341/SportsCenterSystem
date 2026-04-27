package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.config.JsonPatchUtil;
import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import com.github.fge.jsonpatch.JsonPatch;
import lombok.Builder;
import lombok.Data;
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
        Payment payment = Payment.builder()
                .bookingId(dto.getBookingId())
                .rentalId(dto.getRentalId())
                .amount(dto.getAmount())
                .depositAmount(dto.getDepositAmount())
                .paymentMethod(dto.getPaymentMethod())
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(dto.getStatus() != null ? dto.getStatus() : Payment.PaymentStatus.PENDING)
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

    public RevenueReport getRevenueBetween(LocalDateTime from, LocalDateTime to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'to' must be after 'from'");
        }
        BigDecimal total = paymentRepository.sumRevenueBetween(from, to);
        List<RevenueByMethod> byMethod = new ArrayList<>();
        for (Object[] row : paymentRepository.revenueByMethodBetween(from, to)) {
            byMethod.add(RevenueByMethod.builder()
                    .method((Payment.PaymentMethod) row[0])
                    .total((BigDecimal) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }
        return RevenueReport.builder()
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

    @Data @Builder
    public static class RevenueReport {
        private LocalDateTime from;
        private LocalDateTime to;
        private BigDecimal totalRevenue;
        private List<RevenueByMethod> byMethod;
    }

    @Data @Builder
    public static class RevenueByMethod {
        private Payment.PaymentMethod method;
        private BigDecimal total;
        private long count;
    }
}


