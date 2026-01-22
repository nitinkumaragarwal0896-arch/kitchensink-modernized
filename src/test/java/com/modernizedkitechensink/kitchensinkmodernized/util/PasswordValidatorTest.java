package com.modernizedkitechensink.kitchensinkmodernized.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordValidator.
 */
class PasswordValidatorTest {

    @Test
    void testValidPasswords() {
        // Valid passwords
        assertTrue(PasswordValidator.isValid("SecurePass123!"));
        assertTrue(PasswordValidator.isValid("MyP@ssw0rd"));
        assertTrue(PasswordValidator.isValid("Test123!@#"));
        assertTrue(PasswordValidator.isValid("Abcd1234!"));
    }

    @Test
    void testInvalidPasswords_TooShort() {
        assertFalse(PasswordValidator.isValid("Abc12!"));  // Only 6 chars
        assertFalse(PasswordValidator.isValid("Test1!"));  // Only 6 chars
    }

    @Test
    void testInvalidPasswords_MissingUppercase() {
        assertFalse(PasswordValidator.isValid("password123!"));  // No uppercase
        assertFalse(PasswordValidator.isValid("test1234!"));     // No uppercase
    }

    @Test
    void testInvalidPasswords_MissingLowercase() {
        assertFalse(PasswordValidator.isValid("PASSWORD123!"));  // No lowercase
        assertFalse(PasswordValidator.isValid("TEST1234!"));     // No lowercase
    }

    @Test
    void testInvalidPasswords_MissingDigit() {
        assertFalse(PasswordValidator.isValid("Password!"));     // No digit
        assertFalse(PasswordValidator.isValid("TestPass!"));     // No digit
    }

    @Test
    void testInvalidPasswords_MissingSpecialChar() {
        assertFalse(PasswordValidator.isValid("Password123"));   // No special char
        assertFalse(PasswordValidator.isValid("TestPass123"));   // No special char
    }

    @Test
    void testInvalidPasswords_WithWhitespace() {
        assertFalse(PasswordValidator.isValid("Pass word123!"));  // Has space
        assertFalse(PasswordValidator.isValid("Test 123!"));      // Has space
    }

    @Test
    void testNullPassword() {
        assertFalse(PasswordValidator.isValid(null));
    }

    @Test
    void testEmptyPassword() {
        assertFalse(PasswordValidator.isValid(""));
        assertFalse(PasswordValidator.isValid("   "));  // Only whitespace
    }

    @Test
    void testValidationResult_ValidPassword() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("SecurePass123!");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidationResult_InvalidPassword() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("weak");
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("must be at least 8 characters"));
    }

    @Test
    void testValidationResult_MultipleErrors() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("short");
        assertFalse(result.isValid());
        String errorMsg = result.getErrorMessage();
        
        // Should contain multiple error messages
        assertTrue(errorMsg.contains("must be at least"));
        assertTrue(errorMsg.contains("uppercase"));
        assertTrue(errorMsg.contains("number"));
        assertTrue(errorMsg.contains("special character"));
    }

    @Test
    void testRegexPattern_Consistency() {
        // Ensure the regex pattern constant matches the validate() logic
        String validPassword = "SecurePass123!";
        String invalidPassword = "weak";
        
        // Both methods should give same result
        assertEquals(PasswordValidator.isValid(validPassword), 
                    PasswordValidator.validate(validPassword).isValid());
        assertEquals(PasswordValidator.isValid(invalidPassword), 
                    PasswordValidator.validate(invalidPassword).isValid());
    }

    @Test
    void testEdgeCases_ExactlyMinLength() {
        // Exactly 8 characters
        assertTrue(PasswordValidator.isValid("Test123!"));
    }

    @Test
    void testEdgeCases_AllSpecialCharsWork() {
        // Test various special characters
        assertTrue(PasswordValidator.isValid("Test123!"));
        assertTrue(PasswordValidator.isValid("Test123@"));
        assertTrue(PasswordValidator.isValid("Test123#"));
        assertTrue(PasswordValidator.isValid("Test123$"));
        assertTrue(PasswordValidator.isValid("Test123%"));
        assertTrue(PasswordValidator.isValid("Test123^"));
        assertTrue(PasswordValidator.isValid("Test123&"));
        assertTrue(PasswordValidator.isValid("Test123*"));
    }
}

