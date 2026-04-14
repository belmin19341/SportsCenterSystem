package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.EquipmentRentalRequestDTO;
import ba.nwt.bookingservice.dto.EquipmentRentalResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.model.EquipmentRental;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.EquipmentRentalRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentRentalService {

    private final EquipmentRentalRepository equipmentRentalRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    public List<EquipmentRentalResponseDTO> getAll() {
        return equipmentRentalRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EquipmentRentalResponseDTO getById(Long id) {
        EquipmentRental rental = equipmentRentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment rental not found with id: " + id));
        return toResponseDTO(rental);
    }

    public List<EquipmentRentalResponseDTO> getByUserId(Long userId) {
        return equipmentRentalRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentRentalResponseDTO> getByBookingId(Long bookingId) {
        return equipmentRentalRepository.findByBookingId(bookingId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EquipmentRentalResponseDTO create(EquipmentRentalRequestDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        Booking booking = null;
        if (dto.getBookingId() != null) {
            booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));
        }

        EquipmentRental rental = EquipmentRental.builder()
                .userId(dto.getUserId())
                .equipmentId(dto.getEquipmentId())
                .booking(booking)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1)
                .totalPrice(dto.getTotalPrice())
                .depositPaid(dto.getDepositPaid())
                .status(dto.getStatus() != null ? dto.getStatus() : EquipmentRental.RentalStatus.RESERVED)
                .build();

        EquipmentRental saved = equipmentRentalRepository.save(rental);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!equipmentRentalRepository.existsById(id)) {
            throw new ResourceNotFoundException("Equipment rental not found with id: " + id);
        }
        equipmentRentalRepository.deleteById(id);
    }

    private EquipmentRentalResponseDTO toResponseDTO(EquipmentRental rental) {
        EquipmentRentalResponseDTO dto = modelMapper.map(rental, EquipmentRentalResponseDTO.class);
        dto.setBookingId(rental.getBooking() != null ? rental.getBooking().getId() : null);
        return dto;
    }
}

