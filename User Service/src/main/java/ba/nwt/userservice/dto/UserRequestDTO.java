package ba.nwt.userservice.dto;

import ba.nwt.userservice.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Role is required")
    private User.Role role;

    @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Phone number is not valid")
    private String phone;
}

