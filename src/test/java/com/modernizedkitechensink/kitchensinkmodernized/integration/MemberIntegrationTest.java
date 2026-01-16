package com.modernizedkitechensink.kitchensinkmodernized.integration;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests using Testcontainers.
 *
 * Spins up a real MongoDB container for each test class.
 * Tests the full request/response cycle through Spring MVC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Member API Integration Tests")
class MemberIntegrationTest {

  // Start a real MongoDB container
  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MemberRepository memberRepository;

  /**
   * Configure Spring to use the Testcontainers MongoDB.
   */
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
  }

  @BeforeEach
  void setUp() {
    memberRepository.deleteAll();
  }

  // ========== GET /api/v1/members Tests ==========

  @Test
  @Order(1)
  @DisplayName("GET /members - should return empty list initially")
  @WithMockUser(authorities = "member:read")
  void shouldReturnEmptyList() throws Exception {
    mockMvc.perform(get("/api/v1/members"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @Order(2)
  @DisplayName("GET /members - should return all members")
  @WithMockUser(authorities = "member:read")
  void shouldReturnAllMembers() throws Exception {
    // Arrange - create test data
    createTestMember("John Doe", "john@example.com", "1234567890");
    createTestMember("Jane Smith", "jane@example.com", "0987654321");

    // Act & Assert
    mockMvc.perform(get("/api/v1/members"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2));
  }

  // ========== GET /api/v1/members/{id} Tests ==========

  @Test
  @Order(3)
  @DisplayName("GET /members/{id} - should return member when exists")
  @WithMockUser(authorities = "member:read")
  void shouldReturnMemberById() throws Exception {
    Member member = createTestMember("John Doe", "john@example.com", "1234567890");

    mockMvc.perform(get("/api/v1/members/" + member.getId()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("John Doe"))
      .andExpect(jsonPath("$.email").value("john@example.com"));
  }

  @Test
  @Order(4)
  @DisplayName("GET /members/{id} - should return 404 when not found")
  @WithMockUser(authorities = "member:read")
  void shouldReturn404WhenNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/members/nonexistent"))
      .andExpect(status().isNotFound());
  }

  // ========== POST /api/v1/members Tests ==========

  @Test
  @Order(5)
  @DisplayName("POST /members - should create new member")
  @WithMockUser(authorities = "member:create")
  void shouldCreateMember() throws Exception {
    String memberJson = """
                {
                    "name": "New Member",
                    "email": "new@example.com",
                    "phoneNumber": "5555555555"
                }
                """;

    mockMvc.perform(post("/api/v1/members")
        .contentType(MediaType.APPLICATION_JSON)
        .content(memberJson))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("New Member"))
      .andExpect(jsonPath("$.id").exists());
  }

  @Test
  @Order(6)
  @DisplayName("POST /members - should return 400 for invalid data")
  @WithMockUser(authorities = "member:create")
  void shouldReturn400ForInvalidData() throws Exception {
    String invalidJson = """
                {
                    "name": "",
                    "email": "invalid-email",
                    "phoneNumber": "123"
                }
                """;

    mockMvc.perform(post("/api/v1/members")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
      .andExpect(status().isBadRequest());
  }

  @Test
  @Order(7)
  @DisplayName("POST /members - should return 409 for duplicate email")
  @WithMockUser(authorities = "member:create")
  void shouldReturn409ForDuplicateEmail() throws Exception {
    createTestMember("Existing", "existing@example.com", "1111111111");

    String duplicateJson = """
                {
                    "name": "Another",
                    "email": "existing@example.com",
                    "phoneNumber": "2222222222"
                }
                """;

    mockMvc.perform(post("/api/v1/members")
        .contentType(MediaType.APPLICATION_JSON)
        .content(duplicateJson))
      .andExpect(status().isConflict());
  }

  // ========== Security Tests ==========

  @Test
  @Order(8)
  @DisplayName("GET /members - should return 403 without proper authority")
  @WithMockUser(authorities = "other:permission")
  void shouldReturn403WithoutAuthority() throws Exception {
    mockMvc.perform(get("/api/v1/members"))
      .andExpect(status().isForbidden());
  }

  @Test
  @Order(9)
  @DisplayName("DELETE /members - should return 403 for user without delete permission")
  @WithMockUser(authorities = "member:read")
  void shouldReturn403ForDeleteWithoutPermission() throws Exception {
    Member member = createTestMember("ToDelete", "delete@example.com", "3333333333");

    mockMvc.perform(delete("/api/v1/members/" + member.getId()))
      .andExpect(status().isForbidden());
  }

  // ========== Helper Methods ==========

  private Member createTestMember(String name, String email, String phone) {
    Member member = new Member();
    member.setName(name);
    member.setEmail(email);
    member.setPhoneNumber(phone);
    return memberRepository.save(member);
  }
}