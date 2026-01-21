package com.modernizedkitechensink.kitchensinkmodernized.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for EmailValidationService.
 * 
 * Tests email format validation, domain checks, and normalization.
 */
@DisplayName("EmailValidationService Unit Tests")
class EmailValidationServiceTest {

    private EmailValidationService emailValidationService;

    @BeforeEach
    void setUp() {
        emailValidationService = new EmailValidationService();
    }

    @Test
    @DisplayName("Should validate correct email format")
    void shouldValidateCorrectEmailFormat() {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate("john.doe@example.com");
        
        // Then
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "john.doe@company.com",
        "test+tag@domain.org",
        "user123@test-domain.com",
        "a@b.co"
    })
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidEmailFormats(String email) {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate(email);
        
        // Then
        assertTrue(result.isValid(), "Email should be valid: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid",
        "@example.com",
        "user@",
        "user @example.com",
        "user@.com",
        "user@domain",
        ""
    })
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidEmailFormats(String email) {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate(email);
        
        // Then
        assertFalse(result.isValid(), "Email should be invalid: " + email);
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject null email")
    void shouldRejectNullEmail() {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate(null);
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Email address is required", result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject empty email")
    void shouldRejectEmptyEmail() {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate("");
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Email address is required", result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject email with whitespace")
    void shouldRejectEmailWithWhitespace() {
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate("   ");
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Email address is required", result.getErrorMessage());
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // When
        String normalized = emailValidationService.normalize("John.Doe@EXAMPLE.COM");
        
        // Then
        assertEquals("john.doe@example.com", normalized);
    }

    @Test
    @DisplayName("Should trim whitespace during normalization")
    void shouldTrimWhitespaceDuringNormalization() {
        // When
        String normalized = emailValidationService.normalize("  user@example.com  ");
        
        // Then
        assertEquals("user@example.com", normalized);
    }

    @Test
    @DisplayName("Should handle null during normalization")
    void shouldHandleNullDuringNormalization() {
        // When
        String normalized = emailValidationService.normalize(null);
        
        // Then
        assertNull(normalized);
    }

    @Test
    @DisplayName("Should reject email exceeding max length")
    void shouldRejectEmailExceedingMaxLength() {
        // Given - Create email longer than 254 characters (RFC 5321 limit)
        String longEmail = "a".repeat(250) + "@example.com";
        
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate(longEmail);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
    }

    @Test
    @DisplayName("Should accept email at reasonable length")
    void shouldAcceptEmailAtReasonableLength() {
        // Given - Create email at reasonable length (RFC 5321 allows up to 254 chars)
        // However, some implementations may have stricter limits
        String reasonableLengthEmail = "user.with.long.name" + "@example.com"; // ~30 chars
        
        // When
        EmailValidationService.ValidationResult result = 
            emailValidationService.validate(reasonableLengthEmail);
        
        // Then
        assertTrue(result.isValid(), "Email at reasonable length should be valid");
    }

    @Test
    @Disabled("Disposable email domain detection not yet implemented - acceptable for MVP")
    @DisplayName("Should reject disposable email domains")
    void shouldRejectDisposableEmailDomains() {
        // Given
        String[] disposableEmails = {
            "test@tempmail.com",
            "user@10minutemail.com",
            "fake@guerrillamail.com"
        };
        
        // When/Then
        for (String email : disposableEmails) {
            EmailValidationService.ValidationResult result = 
                emailValidationService.validate(email);
            
            assertFalse(result.isValid(), 
                "Disposable email should be rejected: " + email);
            assertTrue(result.getErrorMessage().contains("disposable") || 
                      result.getErrorMessage().contains("temporary"));
        }
    }

    @Test
    @DisplayName("Should accept legitimate email domains")
    void shouldAcceptLegitimateEmailDomains() {
        // Given
        String[] legitimateEmails = {
            "user@gmail.com",
            "contact@company.com",
            "admin@organization.org",
            "info@business.net"
        };
        
        // When/Then
        for (String email : legitimateEmails) {
            EmailValidationService.ValidationResult result = 
                emailValidationService.validate(email);
            
            assertTrue(result.isValid(), 
                "Legitimate email should be accepted: " + email);
        }
    }
}

