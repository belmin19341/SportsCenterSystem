package ba.nwt.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last4", length = 4)
    private String last4;

    @Column(name = "brand", length = 20)
    private String brand;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
