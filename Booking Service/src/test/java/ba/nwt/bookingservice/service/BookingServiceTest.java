package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.client.PaymentServiceClient;
import ba.nwt.bookingservice.client.ResourceServiceClient;
import ba.nwt.bookingservice.client.UserServiceClient;
import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ResourceServiceClient resourceServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private BookingService bookingService;

    private Booking booking;
    private BookingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        booking = Booking.builder()
                .id(1L)
                .userId(3L)
                .facilityId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .totalPrice(new BigDecimal("60.00"))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        responseDTO = BookingResponseDTO.builder()
                .id(1L)
                .userId(3L)
                .facilityId(1L)
                .totalPrice(new BigDecimal("60.00"))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
    }

    @Test
    void getAll_shouldReturnList() {
        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(modelMapper.map(any(Booking.class), eq(BookingResponseDTO.class))).thenReturn(responseDTO);

        List<BookingResponseDTO> result = bookingService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(3L);
    }

    @Test
    void getById_shouldReturnBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(modelMapper.map(booking, BookingResponseDTO.class)).thenReturn(responseDTO);

        BookingResponseDTO result = bookingService.getById(1L);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("60.00");
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldCreateBooking() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(3L)
                .facilityId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .totalPrice(new BigDecimal("60.00"))
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(modelMapper.map(any(Booking.class), eq(BookingResponseDTO.class))).thenReturn(responseDTO);

        BookingResponseDTO result = bookingService.create(request);

        assertThat(result).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_shouldThrowWhenEndBeforeStart() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(3L).facilityId(1L)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(2))
                .totalPrice(new BigDecimal("60.00"))
                .build();

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void delete_shouldDeleteExisting() {
        when(bookingRepository.existsById(1L)).thenReturn(true);

        bookingService.delete(1L);

        verify(bookingRepository).deleteById(1L);
    }
}

