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
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Member entity representing a registered member.
 * 
 * INDEXES:
 * All indexes are managed centrally in MongoIndexInitializer.java for consistency.
 * 
 * Current indexes (4 total, optimized from original 7):
 *   1. _id (unique, mandatory)
 *   2. email_unique_idx (unique, for findByEmail and duplicate prevention)
 *   3. createdAt_desc_idx (for sorting recent members)
 *   4. name_createdAt_idx (compound, covers name sorting via prefix matching)
 * 
 * Query patterns covered:
 *   • findByEmail(email) → email_unique_idx ✅
 *   • findAllByOrderByNameAsc() → name_createdAt_idx (prefix) ✅
 *   • sort=name,asc → name_createdAt_idx (prefix) ✅
 *   • sort=createdAt,desc → createdAt_desc_idx ✅
 *   • sort=name,desc → IN-MEMORY SORT (acceptable for small dataset)
 *   • searchMembers($regex) → COLLSCAN (acceptable for small dataset)
 * 
 * Removed indexes (optimization):
 *   • name_idx: Redundant (covered by name_createdAt_idx prefix)
 *   • createdBy_createdAt_idx: UNUSED (no queries filter by createdBy)
 *   • updatedAt_desc_idx: Rarely/never used (UI doesn't sort by updatedAt)
 * 
 * @see com.modernizedkitechensink.kitchensinkmodernized.config.MongoIndexInitializer#createMemberIndexes()
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
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
  private String email;

  @NotBlank(message = "Phone number is required")
  @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must start with 6, 7, 8, or 9 (Indian mobile numbers only)")
  private String phoneNumber;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @CreatedBy
  private String createdBy;

  @LastModifiedBy
  private String updatedBy;
}
