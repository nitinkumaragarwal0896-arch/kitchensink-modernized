package com.mongodb.kitchensink.contract;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract Tests for Legacy Kitchensink API.
 *
 * These tests define the expected behavior.
 * Run against legacy first, then against new app.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LegacyApiContractTest {

  // Legacy API base URL
  private static final String BASE_URL = "http://localhost:8080/kitchensink/rest";

  private static RestTemplate restTemplate;
  private static String testEmail;

  @BeforeAll
  static void setup() {
    restTemplate = new RestTemplate();
    testEmail = "test" + System.currentTimeMillis() + "@example.com";
  }

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
    System.out.println("✅ GET /members returned " + response.getBody().size() + " members");
  }

  @Test
  @Order(2)
  @DisplayName("GET /members/0 - should return John Smith")
  void getMemberById_shouldReturnMember() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
      BASE_URL + "/members/0",
      Map.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("John Smith", response.getBody().get("name"));
    System.out.println("✅ GET /members/0 returned: " + response.getBody().get("name"));
  }

  @Test
  @Order(3)
  @DisplayName("POST /members - should create new member")
  void createMember_shouldSucceed() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String body = String.format("""
            {
                "name": "Test User",
                "email": "%s",
                "phoneNumber": "1234567890"
            }
            """, testEmail);

    HttpEntity<String> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(
      BASE_URL + "/members",
      request,
      String.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    System.out.println("✅ POST /members created member: " + testEmail);
  }

  @Test
  @Order(4)
  @DisplayName("POST /members - duplicate email should return 409")
  void createMember_duplicateEmail_shouldFail() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String body = String.format("""
            {
                "name": "Another User",
                "email": "%s",
                "phoneNumber": "0987654321"
            }
            """, testEmail);

    HttpEntity<String> request = new HttpEntity<>(body, headers);

    HttpClientErrorException exception = assertThrows(
      HttpClientErrorException.class,
      () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    System.out.println("✅ Duplicate email rejected with 409 Conflict");
  }

  @Test
  @Order(5)
  @DisplayName("POST /members - invalid name should return 400")
  void createMember_invalidName_shouldFail() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String body = """
            {
                "name": "Test123",
                "email": "invalid@test.com",
                "phoneNumber": "1234567890"
            }
            """;

    HttpEntity<String> request = new HttpEntity<>(body, headers);

    HttpClientErrorException exception = assertThrows(
      HttpClientErrorException.class,
      () -> restTemplate.postForEntity(BASE_URL + "/members", request, String.class)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    System.out.println("✅ Invalid name rejected with 400 Bad Request");
  }

  @Test
  @Order(6)
  @DisplayName("GET /members/99999 - non-existing should return 404")
  void getMemberById_notFound_shouldReturn404() {
    HttpClientErrorException exception = assertThrows(
      HttpClientErrorException.class,
      () -> restTemplate.getForEntity(BASE_URL + "/members/99999", Map.class)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    System.out.println("✅ Non-existing member returned 404");
  }
}