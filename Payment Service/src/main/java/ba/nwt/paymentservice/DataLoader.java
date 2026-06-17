package ba.nwt.paymentservice;

import ba.nwt.paymentservice.model.*;
import ba.nwt.paymentservice.repository.*;
import ba.nwt.paymentservice.service.SavedCardService;
import ba.nwt.paymentservice.service.StripeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentRepository documentRepository;
    private final DisputeRepository disputeRepository;
    private final SavedCardService savedCardService;
    private final StripeGateway stripeGateway;

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info(">>> Data already exists, skipping DataLoader.");
            return;
        }

        log.info(">>> Inserting initial data into Payment Service database...");

        // ── Payments ──
        Payment payment1 = paymentRepository.save(Payment.builder()
                .bookingId(1L)
                .amount(new BigDecimal("60.00"))
                .depositAmount(BigDecimal.ZERO)
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusHours(2))
                .build());

        Payment payment2 = paymentRepository.save(Payment.builder()
                .bookingId(2L)
                .amount(new BigDecimal("60.00"))
                .paymentMethod(Payment.PaymentMethod.PAYPAL)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .build());

        Payment rentalPayment = paymentRepository.save(Payment.builder()
                .rentalId(2L)
                .amount(new BigDecimal("140.00"))
                .depositAmount(new BigDecimal("100.00"))
                .paymentMethod(Payment.PaymentMethod.DEBIT_CARD)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusDays(1))
                .build());

        Payment completedPayment = paymentRepository.save(Payment.builder()
                .bookingId(4L)
                .amount(new BigDecimal("35.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PAID)
                .paidAt(LocalDateTime.now().minusDays(5))
                .build());

        // ── Notifications ──
        notificationRepository.save(Notification.builder()
                .userId(3L)
                .type(Notification.NotificationType.BOOKING_CONFIRMATION)
                .subject("Booking confirmed")
                .message("Your booking for Small Court A is confirmed for " +
                        LocalDateTime.now().plusDays(2).toLocalDate() + " at 18:00.")
                .sentAt(LocalDateTime.now().minusHours(2))
                .isRead(true)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(4L)
                .type(Notification.NotificationType.GROUP_INVITE)
                .subject("Group Invitation")
                .message("Belmin invited you to join a booking on Small Court A.")
                .sentAt(LocalDateTime.now().minusHours(1))
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(3L)
                .type(Notification.NotificationType.ACHIEVEMENT_UNLOCKED)
                .subject("New badge unlocked!")
                .message("Congratulations! You've unlocked the 'Regular Player' badge 🏅")
                .sentAt(LocalDateTime.now().minusDays(1))
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(5L)
                .type(Notification.NotificationType.PAYMENT_RECEIPT)
                .subject("Payment Receipt")
                .message("Payment of 35.00 BAM for the tennis court was successfully processed.")
                .sentAt(LocalDateTime.now().minusDays(5))
                .isRead(true)
                .build());

        // ── Documents ──
        documentRepository.save(Document.builder()
                .userId(3L)
                .relatedEntityId(1L)
                .relatedEntityType(Document.RelatedEntityType.BOOKING)
                .documentType(Document.DocumentType.BOOKING_CONFIRMATION)
                .filePath("/documents/booking_confirmation_1.pdf")
                .build());

        documentRepository.save(Document.builder()
                .userId(5L)
                .relatedEntityId(completedPayment.getId())
                .relatedEntityType(Document.RelatedEntityType.PAYMENT)
                .documentType(Document.DocumentType.INVOICE)
                .filePath("/documents/invoice_4.pdf")
                .build());

        // ── Disputes ──
        disputeRepository.save(Dispute.builder()
                .bookingId(4L)
                .reporterId(5L)
                .description("The court was wet and slippery; we weren't told it had been watered before the session.")
                .status(Dispute.DisputeStatus.OPEN)
                .build());

        // ── Saved cards (seed for users 4=belmin_d and 5=harun_g) ──
        // tok_visa is a Stripe test token that always succeeds
        if (stripeGateway.isEnabled()) {
            try {
                String cus1 = stripeGateway.createCustomer("tok_visa");
                savedCardService.save(4L, "4242", "visa", cus1);
                String cus2 = stripeGateway.createCustomer("tok_visa");
                savedCardService.save(5L, "4242", "visa", cus2);
                log.info(">>> Seeded 2 Stripe saved cards (users 4, 5).");
            } catch (Exception e) {
                log.warn(">>> Stripe saved card seeding failed (Stripe may be unreachable): {}", e.getMessage());
            }
        } else {
            // Stripe not configured — seed mock records so UI shows saved cards
            savedCardService.save(4L, "4242", "visa", "cus_mock_belmin");
            savedCardService.save(5L, "4242", "visa", "cus_mock_harun");
            log.info(">>> Seeded 2 mock saved cards (Stripe key not configured).");
        }

        log.info(">>> Payment Service DataLoader finished — {} payments, {} notifications, {} documents, {} disputes.",
                paymentRepository.count(), notificationRepository.count(),
                documentRepository.count(), disputeRepository.count());
    }
}
