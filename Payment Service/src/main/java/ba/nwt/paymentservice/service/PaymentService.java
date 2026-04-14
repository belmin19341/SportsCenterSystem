package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    public List<PaymentResponseDTO> getAll() {
        return paymentRepository.findAll().stream()
                .map(p -> modelMapper.map(p, PaymentResponseDTO.class))
                .collect(Collectors.toList());
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

    public void delete(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
    }
}

