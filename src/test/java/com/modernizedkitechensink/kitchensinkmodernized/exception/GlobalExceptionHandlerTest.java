package com.modernizedkitechensink.kitchensinkmodernized.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for GlobalExceptionHandler.
 * 
 * Tests centralized exception handling for REST API error responses.
 */
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle MemberNotFoundException with 404 status")
    void shouldHandleMemberNotFoundException() {
        // Given
        MemberNotFoundException exception = new MemberNotFoundException("123");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMemberNotFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Member not found with ID: 123", body.get("error"));
    }

    @Test
    @DisplayName("Should handle DuplicateEmailException with 409 status")
    void shouldHandleDuplicateEmailException() {
        // Given
        DuplicateEmailException exception = new DuplicateEmailException("test@example.com");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleDuplicateEmail(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Email already exists: test@example.com", body.get("error"));
    }

    @Test
    @DisplayName("Should handle generic exceptions with 500 status")
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Something went wrong");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Internal server error", body.get("error"));
    }

    @Test
    @DisplayName("Should return response body for all exceptions")
    void shouldReturnResponseBodyForAllExceptions() {
        // Given
        MemberNotFoundException exception = new MemberNotFoundException("Test");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMemberNotFound(exception);

        // Then
        Map<String, String> body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertNotNull(body.get("error"), "Error field should be present");
    }

    @Test
    @DisplayName("Should sanitize error messages for security")
    void shouldSanitizeErrorMessagesForSecurity() {
        // Given - Generic exception that might expose internal details
        Exception exception = new RuntimeException("Database connection failed at jdbc:postgresql://localhost:5432/mydb");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(exception);

        // Then
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        
        String message = body.get("error");
        assertEquals("Internal server error", message, 
            "Generic message should hide internal details");
    }

    @Test
    @DisplayName("Should handle null exception message gracefully")
    void shouldHandleNullExceptionMessageGracefully() {
        // Given
        MemberNotFoundException exception = new MemberNotFoundException(null);

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMemberNotFound(exception);

        // Then
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        // Null message is acceptable - the response should still work
    }

    @Test
    @DisplayName("Should return proper HTTP status codes")
    void shouldReturnProperHttpStatusCodes() {
        // 404 for not found
        ResponseEntity<?> notFoundResponse = exceptionHandler.handleMemberNotFound(
            new MemberNotFoundException("test")
        );
        assertEquals(404, notFoundResponse.getStatusCode().value());

        // 409 for conflict
        ResponseEntity<?> conflictResponse = exceptionHandler.handleDuplicateEmail(
            new DuplicateEmailException("test")
        );
        assertEquals(409, conflictResponse.getStatusCode().value());

        // 500 for generic errors
        ResponseEntity<?> serverErrorResponse = exceptionHandler.handleGenericException(
            new Exception("test")
        );
        assertEquals(500, serverErrorResponse.getStatusCode().value());
    }

    @Test
    @DisplayName("Should handle empty error messages")
    void shouldHandleEmptyErrorMessages() {
        // Given
        MemberNotFoundException exception = new MemberNotFoundException("");

        // When
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMemberNotFound(exception);

        // Then
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.get("error"));
    }

    @Test
    @DisplayName("Should handle different exception types")
    void shouldHandleDifferentExceptionTypes() {
        // Test RuntimeException
        Exception runtimeEx = new RuntimeException("Runtime error");
        ResponseEntity<Map<String, String>> response1 = exceptionHandler.handleGenericException(runtimeEx);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response1.getStatusCode());

        // Test generic Exception
        Exception genericEx = new Exception("Generic error");
        ResponseEntity<Map<String, String>> response2 = exceptionHandler.handleGenericException(genericEx);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response2.getStatusCode());
    }
}
