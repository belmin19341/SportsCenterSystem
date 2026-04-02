package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.AchievementRequestDTO;
import ba.nwt.userservice.dto.AchievementResponseDTO;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievement", description = "Achievement management APIs")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @Operation(summary = "Get all achievements")
    public ResponseEntity<List<AchievementResponseDTO>> getAll() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get achievement by ID")
    public ResponseEntity<AchievementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(achievementService.getAchievementById(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get achievements by category")
    public ResponseEntity<List<AchievementResponseDTO>> getByCategory(@PathVariable Achievement.AchievementCategory category) {
        return ResponseEntity.ok(achievementService.getByCategory(category));
    }

    @PostMapping
    @Operation(summary = "Create a new achievement")
    public ResponseEntity<AchievementResponseDTO> create(@Valid @RequestBody AchievementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achievementService.createAchievement(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an achievement")
    public ResponseEntity<AchievementResponseDTO> update(@PathVariable Long id, @Valid @RequestBody AchievementRequestDTO dto) {
        return ResponseEntity.ok(achievementService.updateAchievement(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an achievement")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }
}

