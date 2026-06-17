package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class StripeGateway {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    public boolean isEnabled() {
        return secretKey != null && secretKey.startsWith("sk_");
    }

    public record ChargeResult(String chargeId, String customerId) {}

    /** One-time charge using a Stripe token (card not saved). */
    public ChargeResult chargeWithToken(BigDecimal amount, String token) {
        Stripe.apiKey = secretKey;
        Map<String, Object> params = new HashMap<>();
        params.put("amount", toCents(amount));
        params.put("currency", "eur");
        params.put("source", token);
        params.put("description", "SportsCenterSystem payment");
        try {
            Charge charge = Charge.create(params);
            log.info("Stripe one-time charge: {} amount={}€", charge.getId(), amount);
            return new ChargeResult(charge.getId(), null);
        } catch (CardException e) {
            log.warn("Stripe card declined: code={} message={}", e.getCode(), e.getMessage());
            throw new StripeChargeException("Card declined: " + e.getMessage());
        } catch (StripeException e) {
            log.error("Stripe API error: {}", e.getMessage());
            throw new StripeChargeException("Payment processing error: " + e.getMessage());
        }
    }

    /** Create a Stripe Customer (saves card), then charge. Returns both chargeId and customerId. */
    public ChargeResult createCustomerAndCharge(BigDecimal amount, String token) {
        Stripe.apiKey = secretKey;
        try {
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("source", token);
            customerParams.put("description", "SportsCenterSystem saved card");
            Customer customer = Customer.create(customerParams);

            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", toCents(amount));
            chargeParams.put("currency", "eur");
            chargeParams.put("customer", customer.getId());
            chargeParams.put("description", "SportsCenterSystem payment");
            Charge charge = Charge.create(chargeParams);

            log.info("Stripe charge+save: {} customerId={} amount={}€", charge.getId(), customer.getId(), amount);
            return new ChargeResult(charge.getId(), customer.getId());
        } catch (CardException e) {
            log.warn("Stripe card declined: code={} message={}", e.getCode(), e.getMessage());
            throw new StripeChargeException("Card declined: " + e.getMessage());
        } catch (StripeException e) {
            log.error("Stripe API error: {}", e.getMessage());
            throw new StripeChargeException("Payment processing error: " + e.getMessage());
        }
    }

    /** Create a Stripe Customer without charging (used for seeding saved cards). */
    public String createCustomer(String token) {
        Stripe.apiKey = secretKey;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("source", token);
            params.put("description", "SportsCenterSystem seed card");
            Customer customer = Customer.create(params);
            log.info("Stripe customer created for seeding: {}", customer.getId());
            return customer.getId();
        } catch (StripeException e) {
            log.error("Stripe customer creation failed: {}", e.getMessage());
            throw new StripeChargeException("Customer creation error: " + e.getMessage());
        }
    }

    /** Charge an existing Stripe Customer (saved card). */
    public ChargeResult chargeCustomer(BigDecimal amount, String stripeCustomerId) {
        Stripe.apiKey = secretKey;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", toCents(amount));
            params.put("currency", "eur");
            params.put("customer", stripeCustomerId);
            params.put("description", "SportsCenterSystem payment (saved card)");
            Charge charge = Charge.create(params);
            log.info("Stripe saved-card charge: {} customerId={} amount={}€", charge.getId(), stripeCustomerId, amount);
            return new ChargeResult(charge.getId(), stripeCustomerId);
        } catch (CardException e) {
            log.warn("Stripe card declined: code={} message={}", e.getCode(), e.getMessage());
            throw new StripeChargeException("Card declined: " + e.getMessage());
        } catch (StripeException e) {
            log.error("Stripe API error: {}", e.getMessage());
            throw new StripeChargeException("Payment processing error: " + e.getMessage());
        }
    }

    private long toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public static class StripeChargeException extends RuntimeException {
        public StripeChargeException(String message) {
            super(message);
        }
    }
}
