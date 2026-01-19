package com.modernizedkitechensink.kitchensinkmodernized.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Phone Number Validation Service.
 * 
 * Provides production-grade phone number validation with detailed error messages.
 * 
 * Rules (Indian Mobile Numbers):
 * - Exactly 10 digits
 * - Must start with 6, 7, 8, or 9 (Indian mobile number prefixes)
 * - Only digits (no spaces, dashes, parentheses)
 * - No country code prefix (+91)
 * 
 * Examples:
 * - Valid: 9876543210, 8123456789, 7012345678, 6789012345
 * - Invalid: 1234567890 (starts with 1), 5123456789 (starts with 5), 12345 (too short)
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@Slf4j
public class PhoneValidationService {

    // Indian mobile number pattern: exactly 10 digits, starts with 6/7/8/9
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Validates a phone number and returns a detailed error message if invalid.
     * 
     * @param phoneNumber The phone number to validate
     * @return Validation result with error message (null if valid)
     */
    public ValidationResult validate(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ValidationResult.invalid("Phone number is required");
        }

        String trimmed = phoneNumber.trim();

        // Check for invalid characters (must be digits only)
        if (!DIGITS_ONLY_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Phone number must contain only digits (no spaces, dashes, or special characters)");
        }

        // Check exact length (Indian mobile numbers are exactly 10 digits)
        if (trimmed.length() < 10) {
            return ValidationResult.invalid("Phone number must be exactly 10 digits");
        }

        if (trimmed.length() > 10) {
            return ValidationResult.invalid("Phone number must be exactly 10 digits (do not include +91 country code)");
        }

        // Check if starts with valid prefix (6, 7, 8, or 9)
        char firstDigit = trimmed.charAt(0);
        if (firstDigit < '6' || firstDigit > '9') {
            return ValidationResult.invalid("Phone number must start with 6, 7, 8, or 9 (Indian mobile numbers only)");
        }

        // Final pattern check (10 digits starting with 6-9)
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Phone number format is invalid");
        }

        log.debug("Phone number validated successfully: {}", maskPhone(trimmed));
        return ValidationResult.valid();
    }

    /**
     * Checks if a phone number is valid (simple boolean check).
     */
    public boolean isValid(String phoneNumber) {
        return validate(phoneNumber).isValid();
    }

    /**
     * Normalizes a phone number (removes spaces, dashes, etc.).
     * For future use when we accept formatted input.
     */
    public String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    /**
     * Masks a phone number for logging (shows only last 4 digits).
     * Example: 1234567890 -> ******7890
     */
    private String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        int maskLength = phoneNumber.length() - 4;
        return "*".repeat(maskLength) + phoneNumber.substring(maskLength);
    }

    /**
     * Validation result wrapper.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

