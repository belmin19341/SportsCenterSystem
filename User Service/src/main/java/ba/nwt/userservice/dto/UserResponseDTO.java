package ba.nwt.userservice.dto;

import ba.nwt.userservice.model.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private User.Role role;
    private String phone;
    private LocalDateTime createdAt;
}

