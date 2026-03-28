package ba.nwt.paymentservice;

import ba.nwt.paymentservice.model.*;
import ba.nwt.paymentservice.repository.*;
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

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info(">>> Podaci već postoje, preskačem DataLoader.");
            return;
        }

        log.info(">>> Unosim početne podatke u Payment Service bazu...");

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
                .subject("Rezervacija potvrđena")
                .message("Vaša rezervacija za Mali teren A je potvrđena za " +
                        LocalDateTime.now().plusDays(2).toLocalDate() + " u 18:00.")
                .sentAt(LocalDateTime.now().minusHours(2))
                .isRead(true)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(4L)
                .type(Notification.NotificationType.GROUP_INVITE)
                .subject("Poziv u grupu")
                .message("Belmin vas poziva da se pridružite rezervaciji na Malom terenu A.")
                .sentAt(LocalDateTime.now().minusHours(1))
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(3L)
                .type(Notification.NotificationType.ACHIEVEMENT_UNLOCKED)
                .subject("Novi bedž!")
                .message("Čestitamo! Otključali ste bedž 'Redovni igrač' 🏅")
                .sentAt(LocalDateTime.now().minusDays(1))
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(5L)
                .type(Notification.NotificationType.PAYMENT_RECEIPT)
                .subject("Potvrda plaćanja")
                .message("Plaćanje od 35.00 KM za teniski teren je uspješno procesirano.")
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
                .description("Teren je bio mokar i klizav, nisu nam rekli da je bio poliven prije termina.")
                .status(Dispute.DisputeStatus.OPEN)
                .build());

        log.info(">>> Payment Service DataLoader završen — {} plaćanja, {} notifikacija, {} dokumenta, {} sporova.",
                paymentRepository.count(), notificationRepository.count(),
                documentRepository.count(), disputeRepository.count());
    }
}

