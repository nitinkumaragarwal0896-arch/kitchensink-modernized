package com.modernizedkitechensink.kitchensinkmodernized.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Centralized password validation utility.
 * 
 * DESIGN DECISION: Single source of truth for password requirements
 * - Prevents inconsistent validation across different endpoints
 * - Easy to update password policy in one place
 * - Provides both programmatic validation and regex for annotations
 * 
 * PASSWORD POLICY:
 * - Minimum 8 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character (!@#$%^&*...)
 * 
 * USAGE:
 * 
 * // For programmatic validation:
 * ValidationResult result = PasswordValidator.validate("MyPass123!");
 * if (!result.isValid()) {
 *   throw new IllegalArgumentException(result.getErrorMessage());
 * }
 * 
 * // For JSR-303 Bean Validation (@Pattern annotation):
 * @Pattern(
 *   regexp = PasswordValidator.REGEX_PATTERN,
 *   message = PasswordValidator.ERROR_MESSAGE
 * )
 * private String password;
 */
public class PasswordValidator {

    /**
     * Minimum password length.
     */
    public static final int MIN_LENGTH = 8;

    /**
     * Regex pattern for password validation (for use in @Pattern annotations).
     * Uses positive lookaheads for efficient single-pass validation.
     * 
     * Breakdown:
     * - ^                  : Start of string
     * - (?=.*[a-z])        : Positive lookahead - contains at least one lowercase
     * - (?=.*[A-Z])        : Positive lookahead - contains at least one uppercase
     * - (?=.*\\d)          : Positive lookahead - contains at least one digit
     * - (?=.*[!@#$%...])   : Positive lookahead - contains at least one special char
     * - .{8,}              : At least 8 characters of any type
     * - $                  : End of string
     */
    public static final String REGEX_PATTERN = 
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?])(?=\\S+$).{8,}$";

    /**
     * User-friendly error message for invalid passwords.
     */
    public static final String ERROR_MESSAGE = 
        "Password must be at least 8 characters and contain at least one uppercase letter, " +
        "one lowercase letter, one number, and one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)";

    /**
     * Compiled regex pattern for better performance (compiled once, reused many times).
     */
    private static final Pattern COMPILED_PATTERN = Pattern.compile(REGEX_PATTERN);

    // Private constructor to prevent instantiation (utility class)
    private PasswordValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validate password strength (recommended method).
     * 
     * Returns detailed ValidationResult with specific error messages.
     * 
     * @param password The password to validate
     * @return ValidationResult with validity status and error message
     */
    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        // Check null/empty
        if (password == null || password.trim().isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            errors.add("must be at least " + MIN_LENGTH + " characters");
        }

        // Check for uppercase
        if (!password.matches(".*[A-Z].*")) {
            errors.add("must contain at least one uppercase letter");
        }

        // Check for lowercase
        if (!password.matches(".*[a-z].*")) {
            errors.add("must contain at least one lowercase letter");
        }

        // Check for digit
        if (!password.matches(".*\\d.*")) {
            errors.add("must contain at least one number");
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) {
            errors.add("must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        // Check for whitespace (passwords shouldn't have spaces)
        if (password.matches(".*\\s.*")) {
            errors.add("must not contain whitespace");
        }

        if (errors.isEmpty()) {
            return new ValidationResult(true, null);
        } else {
            String errorMessage = "Password " + String.join(", ", errors);
            return new ValidationResult(false, errorMessage);
        }
    }

    /**
     * Quick boolean validation (for simple true/false checks).
     * 
     * @param password The password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return COMPILED_PATTERN.matcher(password).matches();
    }

    /**
     * Result of password validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errorMessage;
        }
    }
}

