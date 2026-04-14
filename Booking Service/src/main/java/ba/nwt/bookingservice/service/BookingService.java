package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    public List<BookingResponseDTO> getAll() {
        return bookingRepository.findAll().stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public BookingResponseDTO getById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return modelMapper.map(booking, BookingResponseDTO.class);
    }

    public List<BookingResponseDTO> getByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getByFacilityId(Long facilityId) {
        return bookingRepository.findByFacilityId(facilityId).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status).stream()
                .map(b -> modelMapper.map(b, BookingResponseDTO.class))
                .collect(Collectors.toList());
    }

    public BookingResponseDTO create(BookingRequestDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Booking booking = Booking.builder()
                .userId(dto.getUserId())
                .facilityId(dto.getFacilityId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalPrice(dto.getTotalPrice())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurringPattern(dto.getRecurringPattern())
                .status(dto.getStatus() != null ? dto.getStatus() : Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        return modelMapper.map(saved, BookingResponseDTO.class);
    }

    public BookingResponseDTO update(Long id, BookingRequestDTO dto) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        booking.setUserId(dto.getUserId());
        booking.setFacilityId(dto.getFacilityId());
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setTotalPrice(dto.getTotalPrice());
        booking.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);
        booking.setRecurringPattern(dto.getRecurringPattern());
        if (dto.getStatus() != null) {
            booking.setStatus(dto.getStatus());
        }

        Booking saved = bookingRepository.save(booking);
        return modelMapper.map(saved, BookingResponseDTO.class);
    }

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }
}

