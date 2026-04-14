package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.UserLoyaltyResponseDTO;
import ba.nwt.userservice.service.UserLoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
@Tag(name = "UserLoyalty", description = "User loyalty program APIs")
public class UserLoyaltyController {

    private final UserLoyaltyService userLoyaltyService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get loyalty info for a user")
    public ResponseEntity<UserLoyaltyResponseDTO> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userLoyaltyService.getByUserId(userId));
    }

    @PatchMapping("/user/{userId}/add-points")
    @Operation(summary = "Add loyalty points to a user")
    public ResponseEntity<UserLoyaltyResponseDTO> addPoints(@PathVariable Long userId, @RequestParam int points) {
        return ResponseEntity.ok(userLoyaltyService.addPoints(userId, points));
    }
}

