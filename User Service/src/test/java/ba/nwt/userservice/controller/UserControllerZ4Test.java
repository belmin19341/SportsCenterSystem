package ba.nwt.userservice.controller;

import ba.nwt.userservice.config.JsonPatchUtil;
import ba.nwt.userservice.dto.AchievementRequestDTO;
import ba.nwt.userservice.dto.AchievementResponseDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.exception.GlobalExceptionHandler;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.service.AchievementService;
import ba.nwt.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for Z4 endpoints: PATCH (JSON Patch), paginated search, batch insert.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerZ4Test {

    @Mock private UserService userService;
    @Mock private AchievementService achievementService;

    @InjectMocks private UserController userController;
    @InjectMocks private AchievementController achievementController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController, achievementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
    }

    /** MockMvc with PageModule registered, used only for endpoints returning {@link Page}. */
    private MockMvc pageMockMvc() {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.registerModule(new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule());
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter conv =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(om);
        return MockMvcBuilders
                .standaloneSetup(userController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setMessageConverters(conv)
                .build();
    }

    @Test
    void searchUsers_shouldReturnPage() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .id(1L).username("alice").email("alice@example.com").role(User.Role.USER).build();
        Page<UserResponseDTO> page = new PageImpl<>(List.of(dto));
        when(userService.searchUsers(eq(User.Role.USER), eq("ali"), any(Pageable.class))).thenReturn(page);

        pageMockMvc().perform(get("/api/users/search")
                        .param("role", "USER").param("q", "ali")
                        .param("page", "0").param("size", "10").param("sort", "username,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void patchUser_shouldReturn200() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .id(1L).username("alice").email("alice@example.com").role(User.Role.USER).phone("+38762555555").build();
        when(userService.patchUser(eq(1L), any())).thenReturn(dto);

        String patch = "[{\"op\":\"replace\",\"path\":\"/phone\",\"value\":\"+38762555555\"}]";

        mockMvc.perform(patch("/api/users/1")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+38762555555"));
    }

    @Test
    void patchUser_shouldReturn404_whenMissing() throws Exception {
        when(userService.patchUser(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        String patch = "[{\"op\":\"replace\",\"path\":\"/phone\",\"value\":\"+38762555555\"}]";

        mockMvc.perform(patch("/api/users/99")
                        .contentType("application/json-patch+json")
                        .content(patch))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUser_shouldReturn400_whenPatchInvalid() throws Exception {
        // Malformed JSON Patch document → 400 from message converter
        mockMvc.perform(patch("/api/users/1")
                        .contentType("application/json-patch+json")
                        .content("not-a-patch"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAchievementBatch_shouldReturn201() throws Exception {
        List<AchievementRequestDTO> reqs = List.of(
                AchievementRequestDTO.builder().name("First Booking").category(Achievement.AchievementCategory.MILESTONE).build(),
                AchievementRequestDTO.builder().name("10 Reservations").category(Achievement.AchievementCategory.MILESTONE).build()
        );
        List<AchievementResponseDTO> resp = List.of(
                AchievementResponseDTO.builder().id(1L).name("First Booking").build(),
                AchievementResponseDTO.builder().id(2L).name("10 Reservations").build()
        );
        when(achievementService.createBatch(anyList())).thenReturn(resp);

        mockMvc.perform(post("/api/achievements/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqs)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("First Booking"));
    }

    @Test
    void createAchievementBatch_shouldReturn400_whenEmpty() throws Exception {
        when(achievementService.createBatch(anyList()))
                .thenThrow(new IllegalArgumentException("Batch must contain at least one achievement"));

        mockMvc.perform(post("/api/achievements/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }
}
