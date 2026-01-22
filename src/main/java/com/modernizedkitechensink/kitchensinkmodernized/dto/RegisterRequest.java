package com.modernizedkitechensink.kitchensinkmodernized.dto;

import com.modernizedkitechensink.kitchensinkmodernized.util.PasswordValidator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration requests.
 *
 * Example JSON:
 * {
 *   "username": "john_doe",
 *   "email": "john@example.com",
 *   "password": "SecurePass123!",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "phoneNumber": "9876543210"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = PasswordValidator.MIN_LENGTH, 
        message = "Password must be at least 8 characters")
  @Pattern(
    regexp = PasswordValidator.REGEX_PATTERN,
    message = PasswordValidator.ERROR_MESSAGE
  )
  private String password;

  @NotBlank(message = "First name is required")
  @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
  @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
  @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
  private String lastName;

  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits starting with 6, 7, 8, or 9 (Indian mobile numbers only)")
  private String phoneNumber;
}
