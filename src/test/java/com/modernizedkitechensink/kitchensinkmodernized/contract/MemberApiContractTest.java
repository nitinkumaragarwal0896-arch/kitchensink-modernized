package com.modernizedkitechensink.kitchensinkmodernized.contract;

import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONTRACT TESTS - API Compatibility Tests
 * 
 * These tests define the expected behavior of the Members API.
 * 
 * STEP 1: Run against LEGACY app to verify tests are correct
 *         → Set BASE_URL = "http://localhost:8080/kitchensink/rest"
 * 
 * STEP 2: Run against NEW app to verify migration is correct
 *         → Set BASE_URL = "http://localhost:8080/api"
 * 
 * If tests pass on both, the migration preserves behavior! ✅
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemberApiContractTest {

    // ⚠️ CHANGE THIS TO TEST DIFFERENT APPS:
    // Legacy: "http://localhost:8080/kitchensink/rest"
    // New:    "http://localhost:8080/api"
    private static final String BASE_URL = "http://localhost:8080/kitchensink/rest";
    
    private static RestTemplate restTemplate;
    private static String createdMemberEmail;
    
    @BeforeAll
    static void setup() {
        restTemplate = new RestTemplate();
        createdMemberEmail = "test" + System.currentTimeMillis() + "@example.com";
    }

    // ==================== GET ALL MEMBERS ====================
    
    @Test
    @Order(1)
    @DisplayName("GET /members - should return list of members")
    void getAllMembers_shouldReturnList() {
        ResponseEntity<List> response = restTemplate.getForEntity(
            BASE_URL + "/members", 
            List.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        
        System.out.println("✅ GET /members returned " + response.getBody().size() + " members");
    }

    // ==================== CREATE MEMBER ====================
    
    @Test
    @Order(2)
    @DisplayName("POST /members - should create new member")
    void createMember_shouldSucceed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = """
            {
                "name": "Test User",
                "email": "%s",
                "phoneNumber": "1234567890"
            }
            """.formatted(createdMemberEmail);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            BASE_URL + "/members",
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ POST /members created member with email: " + createdMemberEmail);
    }

    @Test
    @Order(3)
    @DisplayName("POST /members - duplicate email should fail")
    void createMember_duplicateEmail_shouldFail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Try to create with same email again
        String requestBody = """
            {
                "name": "Duplicate User",
                "email": "%s",
                "phoneNumber": "0987654321"
            }
            """.formatted(createdMemberEmail);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        System.out.println("✅ Duplicate email correctly rejected with 409 Conflict");
    }

    @Test
    @Order(4)
    @DisplayName("POST /members - invalid name (with numbers) should fail")
    void createMember_invalidName_shouldFail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = """
            {
                "name": "Test123",
                "email": "another@example.com",
                "phoneNumber": "1234567890"
            }
            """;
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        System.out.println("✅ Name with numbers correctly rejected with 400 Bad Request");
    }

    @Test
    @Order(5)
    @DisplayName("POST /members - empty name should fail")
    void createMember_emptyName_shouldFail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = """
            {
                "name": "",
                "email": "empty@example.com",
                "phoneNumber": "1234567890"
            }
            """;
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        System.out.println("✅ Empty name correctly rejected with 400 Bad Request");
    }

    @Test
    @Order(6)
    @DisplayName("POST /members - invalid email should fail")
    void createMember_invalidEmail_shouldFail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = """
            {
                "name": "Valid Name",
                "email": "not-an-email",
                "phoneNumber": "1234567890"
            }
            """;
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        System.out.println("✅ Invalid email correctly rejected with 400 Bad Request");
    }

    @Test
    @Order(7)
    @DisplayName("POST /members - invalid phone (non-digits) should fail")
    void createMember_invalidPhone_shouldFail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = """
            {
                "name": "Valid Name",
                "email": "phone@example.com",
                "phoneNumber": "123-456-7890"
            }
            """;
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        System.out.println("✅ Phone with non-digits correctly rejected with 400 Bad Request");
    }

    // ==================== GET MEMBER BY ID ====================
    
    @Test
    @Order(8)
    @DisplayName("GET /members/{id} - existing member should return 200")
    void getMemberById_existing_shouldSucceed() {
        // ID 0 exists by default in legacy app (from import.sql)
        ResponseEntity<Map> response = restTemplate.getForEntity(
            BASE_URL + "/members/0",
            Map.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Smith", response.getBody().get("name"));
        
        System.out.println("✅ GET /members/0 returned: " + response.getBody().get("name"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /members/{id} - non-existing member should return 404")
    void getMemberById_nonExisting_shouldReturn404() {
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> restTemplate.getForEntity(BASE_URL + "/members/99999", Map.class)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        System.out.println("✅ Non-existing member correctly returned 404");
    }
}

