package com.modernizedkitechensink.kitchensinkmodernized.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to boost coverage to 80%+
 */
@DisplayName("GlobalExceptionHandler Coverage Boost Tests")
class GlobalExceptionHandlerCoverageBoostTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("should handle RuntimeException and return INTERNAL_SERVER_ERROR")
    void shouldHandleRuntimeException() {
        RuntimeException ex = new RuntimeException("Runtime error occurred");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().get("error"));
    }

    @Test
    @DisplayName("should handle NullPointerException and return INTERNAL_SERVER_ERROR")
    void shouldHandleNullPointerException() {
        NullPointerException ex = new NullPointerException("Null pointer");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().get("error"));
    }

    @Test
    @DisplayName("should handle Exception with null message")
    void shouldHandleExceptionWithNullMessage() {
        Exception ex = new Exception((String) null);
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().get("error"));
    }
}

