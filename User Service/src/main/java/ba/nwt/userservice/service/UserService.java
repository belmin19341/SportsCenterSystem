package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> modelMapper.map(u, UserResponseDTO.class))
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return modelMapper.map(user, UserResponseDTO.class);
    }

    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return modelMapper.map(user, UserResponseDTO.class);
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already in use");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(dto.getPassword()) // In production, hash the password
                .role(dto.getRole())
                .phone(dto.getPhone())
                .build();

        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserResponseDTO.class);
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check uniqueness for username and email if changed
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken");
        }
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already in use");
        }

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword());
        user.setRole(dto.getRole());
        user.setPhone(dto.getPhone());

        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserResponseDTO.class);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}

