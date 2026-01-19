package com.modernizedkitechensink.kitchensinkmodernized.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item result in a bulk operation job.
 * 
 * Used to track:
 * - Which items were successfully processed
 * - Which items failed and why
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResultItem {

  private String itemId;

  private String itemDescription; // e.g., email address, member name

  private String errorMessage; // null if successful, error message if failed
}

