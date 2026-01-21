package com.modernizedkitechensink.kitchensinkmodernized.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Custom Exception Classes.
 * 
 * Tests exception construction and message handling.
 */
@DisplayName("Custom Exception Tests")
class ExceptionTest {

    @Test
    @DisplayName("MemberNotFoundException should create exception with message")
    void shouldCreateMemberNotFoundException() {
        // Given
        String id = "123";

        // When
        MemberNotFoundException exception = new MemberNotFoundException(id);

        // Then
        assertNotNull(exception);
        assertEquals("Member not found with ID: 123", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("MemberNotFoundException should create exception with null ID")
    void shouldCreateMemberNotFoundExceptionWithNullMessage() {
        // When
        MemberNotFoundException exception = new MemberNotFoundException(null);

        // Then
        assertNotNull(exception);
        assertEquals("Member not found with ID: null", exception.getMessage());
    }

    @Test
    @DisplayName("DuplicateEmailException should create exception with message")
    void shouldCreateDuplicateEmailException() {
        // Given
        String email = "test@example.com";

        // When
        DuplicateEmailException exception = new DuplicateEmailException(email);

        // Then
        assertNotNull(exception);
        assertEquals("Email already exists: test@example.com", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("DuplicateEmailException should create exception with null email")
    void shouldCreateDuplicateEmailExceptionWithNullMessage() {
        // When
        DuplicateEmailException exception = new DuplicateEmailException(null);

        // Then
        assertNotNull(exception);
        assertEquals("Email already exists: null", exception.getMessage());
    }

    @Test
    @DisplayName("Exceptions should be throwable")
    void exceptionsShouldBeThrowable() {
        // MemberNotFoundException
        assertThrows(MemberNotFoundException.class, () -> {
            throw new MemberNotFoundException("Test exception");
        });

        // DuplicateEmailException
        assertThrows(DuplicateEmailException.class, () -> {
            throw new DuplicateEmailException("Test exception");
        });
    }

    @Test
    @DisplayName("Exceptions should support message formatting")
    void exceptionsShouldSupportMessageFormatting() {
        // Given
        String id = "123";
        String email = "test@example.com";

        // When
        MemberNotFoundException notFoundEx = new MemberNotFoundException(
            String.format("Member not found with ID: %s", id)
        );
        DuplicateEmailException duplicateEx = new DuplicateEmailException(
            String.format("Email already exists: %s", email)
        );

        // Then
        assertTrue(notFoundEx.getMessage().contains("123"));
        assertTrue(duplicateEx.getMessage().contains("test@example.com"));
    }

    @Test
    @DisplayName("Exceptions should support stack traces")
    void exceptionsShouldSupportStackTraces() {
        // When
        MemberNotFoundException exception = new MemberNotFoundException("Test");

        // Then
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    @DisplayName("Exceptions should be catchable as RuntimeException")
    void exceptionsShouldBeCatchableAsRuntimeException() {
        // When/Then
        try {
            throw new MemberNotFoundException("Test");
        } catch (RuntimeException e) {
            assertTrue(e instanceof MemberNotFoundException);
        }

        try {
            throw new DuplicateEmailException("Test");
        } catch (RuntimeException e) {
            assertTrue(e instanceof DuplicateEmailException);
        }
    }
}

