package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.exception.GlobalExceptionHandler;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllUsers_shouldReturn200() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .id(1L).username("testuser").email("test@example.com").role(User.Role.USER).build();

        when(userService.getAllUsers()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void getUserById_shouldReturn200() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .id(1L).username("testuser").email("test@example.com").role(User.Role.USER).build();

        when(userService.getUserById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserById_shouldReturn404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void createUser_shouldReturn201() throws Exception {
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .username("newuser").email("new@example.com").password("password123")
                .role(User.Role.USER).phone("+38762111111").build();

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .id(1L).username("newuser").email("new@example.com").role(User.Role.USER).build();

        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void createUser_shouldReturn400_whenValidationFails() throws Exception {
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .username("").email("invalid-email").password("12").build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}
