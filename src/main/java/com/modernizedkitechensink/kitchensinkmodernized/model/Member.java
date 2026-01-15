package com.modernizedkitechensink.kitchensinkmodernized.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

  @Id
  private String id;

  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 25, message = "Name must be 1-25 characters")
  @Pattern(regexp = "[^0-9]*", message = "Name must not contain numbers")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Indexed(unique = true)
  private String email;

  @NotBlank(message = "Phone number is required")
  @Size(min = 10, max = 12, message = "Phone must be 10-12 characters")
  @Pattern(regexp = "\\d+", message = "Phone must contain only digits")
  private String phoneNumber;
}
