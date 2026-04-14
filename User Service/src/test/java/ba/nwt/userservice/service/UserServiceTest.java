package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpass")
                .role(User.Role.USER)
                .phone("+38762111111")
                .createdAt(LocalDateTime.now())
                .build();

        responseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(User.Role.USER)
                .phone("+38762111111")
                .build();
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(responseDTO);

        List<UserResponseDTO> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDTO.class)).thenReturn(responseDTO);

        UserResponseDTO result = userService.getUserById(1L);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_shouldThrowNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    void createUser_shouldCreateAndReturn() {
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class))).thenReturn(responseDTO);

        UserResponseDTO result = userService.createUser(requestDTO);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameExists() {
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .username("testuser")
                .email("new@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void deleteUser_shouldDeleteExisting() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

