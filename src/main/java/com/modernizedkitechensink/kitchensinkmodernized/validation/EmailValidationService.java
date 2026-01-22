package com.modernizedkitechensink.kitchensinkmodernized.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Email Validation Service.
 * 
 * Provides production-grade email validation with detailed error messages.
 * 
 * Rules:
 * - Must follow standard email format (user@domain.tld)
 * - Domain must have at least one dot
 * - No spaces or special characters (except @, ., _, -, +)
 * - Optional: Block disposable email domains (configurable)
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@Slf4j
public class EmailValidationService {

    // Standard email regex (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validates an email address with optional disposable email blocking.
     * 
     * @param email The email address to validate
     * @return Validation result with error message (null if valid)
     */
    public ValidationResult validate(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.invalid("Email address is required");
        }

        String trimmed = email.trim();

        // Check for spaces
        if (trimmed.contains(" ")) {
            return ValidationResult.invalid("Email address cannot contain spaces");
        }

        // Check for @ symbol
        if (!trimmed.contains("@")) {
            return ValidationResult.invalid("Email address must contain @ symbol");
        }

        // Check for multiple @ symbols
        long atCount = trimmed.chars().filter(ch -> ch == '@').count();
        if (atCount > 1) {
            return ValidationResult.invalid("Email address can only contain one @ symbol");
        }

        // Split into local and domain parts
        String[] parts = trimmed.split("@");
        if (parts.length != 2) {
            return ValidationResult.invalid("Email format is invalid");
        }

        String localPart = parts[0];
        String domain = parts[1];

        // Validate local part
        if (localPart.isEmpty()) {
            return ValidationResult.invalid("Email address must have a username before @");
        }

        if (localPart.length() > 64) {
            return ValidationResult.invalid("Email username is too long (max 64 characters)");
        }

        // Validate domain
        if (domain.isEmpty()) {
            return ValidationResult.invalid("Email address must have a domain after @");
        }

        if (!domain.contains(".")) {
            return ValidationResult.invalid("Email domain must contain at least one dot (e.g., example.com)");
        }

        if (domain.startsWith(".") || domain.endsWith(".")) {
            return ValidationResult.invalid("Email domain cannot start or end with a dot");
        }

        // Check domain length
        if (domain.length() > 253) {
            return ValidationResult.invalid("Email domain is too long");
        }

    // Check for consecutive dots
    if (trimmed.contains("..")) {
      return ValidationResult.invalid("Email cannot contain consecutive dots");
    }

    // Check domain structure
    String[] domainParts = domain.split("\\.");
    
    // Domain must have at least 2 parts (e.g., example.com)
    if (domainParts.length < 2) {
      return ValidationResult.invalid("Email domain must have at least two parts (e.g., example.com)");
    }

    // Domain should have maximum 3 parts (e.g., mail.google.com)
    // Prevents: example.com.com or test.example.com.org
    if (domainParts.length > 3) {
      return ValidationResult.invalid("Email domain has too many parts (max: subdomain.domain.com)");
    }

    String tld = domainParts[domainParts.length - 1];
    String secondLevelDomain = domainParts[domainParts.length - 2];
    
    // Common TLDs that should only appear once at the end
    java.util.Set<String> commonTLDs = Set.of(
      "com", "org", "net", "edu", "gov", "mil", "co", "io", "ai", "app", "dev",
      "in", "uk", "us", "ca", "au", "de", "fr", "jp", "cn", "br", "ru"
    );
    
    // Check if second-level domain is a common TLD (e.g., .com.com)
    if (commonTLDs.contains(secondLevelDomain.toLowerCase())) {
      return ValidationResult.invalid("Email domain format is invalid (double extension detected like .com.com)");
    }
    
    // Ensure TLD is valid (2-7 letters, no numbers)
    if (!tld.matches("^[a-zA-Z]{2,7}$")) {
      return ValidationResult.invalid("Email domain extension must be 2-7 letters only");
    }

    // Ensure second-level domain is valid (letters, numbers, hyphens)
    if (!secondLevelDomain.matches("^[a-zA-Z0-9-]+$")) {
      return ValidationResult.invalid("Email domain contains invalid characters");
    }

    // Validate format with regex (after structure checks)
    if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
      return ValidationResult.invalid("Email format is invalid (must be like user@example.com)");
    }

    log.debug("Email validated successfully: {}", maskEmail(trimmed));
    return ValidationResult.valid();
    }

    /**
     * Checks if an email is valid (simple boolean check).
     */
    public boolean isValid(String email) {
        return validate(email).isValid();
    }

    /**
     * Normalizes an email address (lowercase, trim).
     */
    public String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Masks an email for logging (shows only first char and domain).
     * Example: john.doe@example.com -> j*******@example.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        String[] parts = email.split("@");
        if (parts[0].isEmpty()) {
            return "***@" + parts[1];
        }
        return parts[0].charAt(0) + "*".repeat(Math.max(0, parts[0].length() - 1)) + "@" + parts[1];
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

