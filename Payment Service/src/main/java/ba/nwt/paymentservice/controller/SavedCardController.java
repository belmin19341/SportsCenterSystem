package ba.nwt.paymentservice.controller;

import ba.nwt.paymentservice.dto.SavedCardResponseDTO;
import ba.nwt.paymentservice.service.SavedCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/saved-cards")
@RequiredArgsConstructor
public class SavedCardController {

    private final SavedCardService savedCardService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavedCardResponseDTO>> getForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(savedCardService.getForUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        savedCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
