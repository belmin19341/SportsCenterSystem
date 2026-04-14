package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.BookingUserRequestDTO;
import ba.nwt.bookingservice.dto.BookingUserResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.model.BookingUser;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingUserService {

    private final BookingUserRepository bookingUserRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    public List<BookingUserResponseDTO> getByBookingId(Long bookingId) {
        return bookingUserRepository.findByBookingId(bookingId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingUserResponseDTO> getByUserId(Long userId) {
        return bookingUserRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public BookingUserResponseDTO create(BookingUserRequestDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));

        BookingUser bookingUser = BookingUser.builder()
                .booking(booking)
                .userId(dto.getUserId())
                .amountDue(dto.getAmountDue())
                .paymentStatus(BookingUser.PaymentStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .build();

        BookingUser saved = bookingUserRepository.save(bookingUser);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!bookingUserRepository.existsById(id)) {
            throw new ResourceNotFoundException("BookingUser not found with id: " + id);
        }
        bookingUserRepository.deleteById(id);
    }

    private BookingUserResponseDTO toResponseDTO(BookingUser bu) {
        BookingUserResponseDTO dto = new BookingUserResponseDTO();
        dto.setId(bu.getId());
        dto.setBookingId(bu.getBooking().getId());
        dto.setUserId(bu.getUserId());
        dto.setAmountDue(bu.getAmountDue());
        dto.setPaymentStatus(bu.getPaymentStatus());
        dto.setInvitedAt(bu.getInvitedAt());
        dto.setPaidAt(bu.getPaidAt());
        return dto;
    }
}

