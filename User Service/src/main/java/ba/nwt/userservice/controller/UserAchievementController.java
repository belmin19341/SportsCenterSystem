package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.UserAchievementRequestDTO;
import ba.nwt.userservice.dto.UserAchievementResponseDTO;
import ba.nwt.userservice.service.UserAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-achievements")
@RequiredArgsConstructor
@Tag(name = "UserAchievement", description = "User achievement assignment APIs")
public class UserAchievementController {

    private final UserAchievementService userAchievementService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all achievements for a user")
    public ResponseEntity<List<UserAchievementResponseDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userAchievementService.getByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user achievement by ID")
    public ResponseEntity<UserAchievementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userAchievementService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Assign an achievement to a user")
    public ResponseEntity<UserAchievementResponseDTO> create(@Valid @RequestBody UserAchievementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAchievementService.create(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user achievement")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAchievementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

