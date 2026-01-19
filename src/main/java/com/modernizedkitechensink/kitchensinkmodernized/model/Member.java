package com.modernizedkitechensink.kitchensinkmodernized.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Member entity representing a registered member.
 * 
 * Indexes:
 * - email: unique index for fast email lookups and duplicate prevention
 * - name: index for sorting and searching by name
 * - createdAt: index for sorting by creation date (descending)
 * - updatedAt: index for sorting by modification date (descending)
 * - compound(createdBy, createdAt): for querying members by creator with date
 */
@Document(collection = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
  @CompoundIndex(name = "createdBy_createdAt_idx", def = "{'createdBy': 1, 'createdAt': -1}"),
  @CompoundIndex(name = "name_createdAt_idx", def = "{'name': 1, 'createdAt': -1}")
})
public class Member {

  @Id
  private String id;

  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 25, message = "Name must be 1-25 characters")
  @Pattern(regexp = "[^0-9]*", message = "Name must not contain numbers")
  @Indexed
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Indexed(unique = true)
  private String email;

  @NotBlank(message = "Phone number is required")
  @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must start with 6, 7, 8, or 9 (Indian mobile numbers only)")
  private String phoneNumber;

  @CreatedDate
  @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
  private LocalDateTime updatedAt;

  @CreatedBy
  private String createdBy;

  @LastModifiedBy
  private String updatedBy;
}
