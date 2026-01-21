package com.modernizedkitechensink.kitchensinkmodernized.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PhoneValidationService.
 * 
 * Tests Indian mobile number validation (10 digits, starts with 6/7/8/9).
 */
@DisplayName("PhoneValidationService Unit Tests")
class PhoneValidationServiceTest {

    private PhoneValidationService phoneValidationService;

    @BeforeEach
    void setUp() {
        phoneValidationService = new PhoneValidationService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "9876543210",
        "8765432109",
        "7654321098",
        "6543210987"
    })
    @DisplayName("Should accept valid Indian mobile numbers")
    void shouldAcceptValidIndianMobileNumbers(String phone) {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(phone);
        
        // Then
        assertTrue(result.isValid(), "Phone should be valid: " + phone);
        assertNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "5876543210",  // Starts with 5
        "4876543210",  // Starts with 4
        "1234567890",  // Starts with 1
        "0987654321"   // Starts with 0
    })
    @DisplayName("Should reject numbers not starting with 6/7/8/9")
    void shouldRejectNumbersNotStartingWithValidDigits(String phone) {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(phone);
        
        // Then
        assertFalse(result.isValid(), "Phone should be invalid: " + phone);
        assertTrue(result.getErrorMessage().contains("6, 7, 8, or 9"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "987654321",    // 9 digits
        "98765432",     // 8 digits
        "98765432101",  // 11 digits
        "987654321012"  // 12 digits
    })
    @DisplayName("Should reject numbers not exactly 10 digits")
    void shouldRejectNumbersNotExactly10Digits(String phone) {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(phone);
        
        // Then
        assertFalse(result.isValid(), "Phone should be invalid: " + phone);
        assertTrue(result.getErrorMessage().contains("10 digits"));
    }

    @Test
    @DisplayName("Should reject null phone number")
    void shouldRejectNullPhoneNumber() {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(null);
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Phone number is required", result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject empty phone number")
    void shouldRejectEmptyPhoneNumber() {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate("");
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Phone number is required", result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject phone number with whitespace only")
    void shouldRejectPhoneNumberWithWhitespaceOnly() {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate("   ");
        
        // Then
        assertFalse(result.isValid());
        assertEquals("Phone number is required", result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "987-654-3210",
        "9876 543 210",
        "+919876543210",
        "(987) 654-3210",
        "9876-543-210"
    })
    @DisplayName("Should reject numbers with special characters or formatting")
    void shouldRejectNumbersWithSpecialCharacters(String phone) {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(phone);
        
        // Then
        assertFalse(result.isValid(), "Phone should be invalid: " + phone);
        assertNotNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abcdefghij",
        "98765abc10",
        "nine8seven6"
    })
    @DisplayName("Should reject numbers with letters")
    void shouldRejectNumbersWithLetters(String phone) {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(phone);
        
        // Then
        assertFalse(result.isValid(), "Phone should be invalid: " + phone);
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should normalize phone number by trimming whitespace")
    void shouldNormalizePhoneNumberByTrimmingWhitespace() {
        // When
        String normalized = phoneValidationService.normalize("  9876543210  ");
        
        // Then
        assertEquals("9876543210", normalized);
    }

    @Test
    @DisplayName("Should handle null during normalization")
    void shouldHandleNullDuringNormalization() {
        // When
        String normalized = phoneValidationService.normalize(null);
        
        // Then
        assertNull(normalized);
    }

    @Test
    @DisplayName("Should validate and normalize in sequence")
    void shouldValidateAndNormalizeInSequence() {
        // Given
        String phoneWithWhitespace = "  9876543210  ";
        
        // When
        String normalized = phoneValidationService.normalize(phoneWithWhitespace);
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate(normalized);
        
        // Then
        assertEquals("9876543210", normalized);
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should provide specific error message for invalid format")
    void shouldProvideSpecificErrorMessageForInvalidFormat() {
        // When
        PhoneValidationService.ValidationResult result = 
            phoneValidationService.validate("1234567890");
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Indian mobile"));
        assertTrue(result.getErrorMessage().contains("6, 7, 8, or 9"));
    }
}

