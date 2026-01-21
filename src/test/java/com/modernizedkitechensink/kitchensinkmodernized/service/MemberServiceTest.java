package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.exception.DuplicateEmailException;
import com.modernizedkitechensink.kitchensinkmodernized.exception.MemberNotFoundException;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import com.modernizedkitechensink.kitchensinkmodernized.validation.EmailValidationService;
import com.modernizedkitechensink.kitchensinkmodernized.validation.PhoneValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit Tests for MemberServiceImpl.
 *
 * Uses Mockito to mock the repository layer.
 * Tests business logic in isolation from database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Tests")
class MemberServiceTest {

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private EmailValidationService emailValidationService;

  @Mock
  private PhoneValidationService phoneValidationService;

  @InjectMocks
  private MemberServiceImpl memberService;

  private Member testMember;

  @BeforeEach
  void setUp() {
    testMember = new Member();
    testMember.setId("1");
    testMember.setName("John Doe");
    testMember.setEmail("john@example.com");
    testMember.setPhoneNumber("9876543210");
    
    // Setup default mock behavior for validation services
    // Using lenient() to avoid UnnecessaryStubbing errors for tests that don't need validation
    lenient().when(emailValidationService.validate(anyString()))
        .thenReturn(EmailValidationService.ValidationResult.valid());
    lenient().when(emailValidationService.normalize(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class).toLowerCase().trim());
    
    lenient().when(phoneValidationService.validate(anyString()))
        .thenReturn(PhoneValidationService.ValidationResult.valid());
    lenient().when(phoneValidationService.normalize(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class).trim());
  }

  // ========== findAll Tests ==========

  @Nested
  @DisplayName("findAll()")
  class FindAllTests {

    @Test
    @DisplayName("should return all members ordered by name")
    void shouldReturnAllMembers() {
      // Arrange
      Member member2 = new Member();
      member2.setId("2");
      member2.setName("Alice Smith");

      when(memberRepository.findAllByOrderByNameAsc())
        .thenReturn(Arrays.asList(member2, testMember));

      // Act
      List<Member> result = memberService.findAll();

      // Assert
      assertEquals(2, result.size());
      assertEquals("Alice Smith", result.get(0).getName());
      verify(memberRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("should return empty list when no members exist")
    void shouldReturnEmptyList() {
      when(memberRepository.findAllByOrderByNameAsc())
        .thenReturn(List.of());

      List<Member> result = memberService.findAll();

      assertTrue(result.isEmpty());
    }
  }

  // ========== findById Tests ==========

  @Nested
  @DisplayName("findById()")
  class FindByIdTests {

    @Test
    @DisplayName("should return member when found")
    void shouldReturnMemberWhenFound() {
      when(memberRepository.findById("1"))
        .thenReturn(Optional.of(testMember));

      Member result = memberService.findById("1");

      assertNotNull(result);
      assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("should throw MemberNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(memberRepository.findById("999"))
        .thenReturn(Optional.empty());

      assertThrows(MemberNotFoundException.class,
        () -> memberService.findById("999"));
    }
  }

  // ========== register Tests ==========

  @Nested
  @DisplayName("register()")
  class RegisterTests {

    @Test
    @DisplayName("should register new member successfully")
    void shouldRegisterNewMember() {
      when(memberRepository.findByEmail(testMember.getEmail()))
        .thenReturn(Optional.empty());
      when(memberRepository.save(any(Member.class)))
        .thenReturn(testMember);

      Member result = memberService.register(testMember);

      assertNotNull(result);
      assertEquals("John Doe", result.getName());
      verify(memberRepository, times(1)).save(testMember);
    }

    @Test
    @DisplayName("should throw DuplicateEmailException when email exists")
    void shouldThrowWhenEmailExists() {
      when(memberRepository.findByEmail(testMember.getEmail()))
        .thenReturn(Optional.of(testMember));

      assertThrows(DuplicateEmailException.class,
        () -> memberService.register(testMember));

      verify(memberRepository, never()).save(any());
    }
  }

  // ========== update Tests ==========

  @Nested
  @DisplayName("update()")
  class UpdateTests {

    @Test
    @DisplayName("should update member successfully")
    void shouldUpdateMember() {
      Member updatedData = new Member();
      updatedData.setName("John Updated");
      updatedData.setEmail("john@example.com");
      updatedData.setPhoneNumber("9999999999");

      when(memberRepository.findById("1"))
        .thenReturn(Optional.of(testMember));
      when(memberRepository.save(any(Member.class)))
        .thenAnswer(inv -> inv.getArgument(0));

      Member result = memberService.update("1", updatedData);

      assertEquals("John Updated", result.getName());
      assertEquals("9999999999", result.getPhoneNumber());
    }

    @Test
    @DisplayName("should throw when member not found")
    void shouldThrowWhenMemberNotFound() {
      when(memberRepository.findById("999"))
        .thenReturn(Optional.empty());

      assertThrows(MemberNotFoundException.class,
        () -> memberService.update("999", testMember));
    }

    @Test
    @DisplayName("should throw when changing to existing email")
    void shouldThrowWhenEmailTaken() {
      Member existingMember = new Member();
      existingMember.setId("2");
      existingMember.setEmail("taken@example.com");

      Member updateData = new Member();
      updateData.setName("John Doe");
      updateData.setEmail("taken@example.com");
      updateData.setPhoneNumber("9876543210");

      when(memberRepository.findById("1"))
        .thenReturn(Optional.of(testMember));
      when(memberRepository.findByEmail("taken@example.com"))
        .thenReturn(Optional.of(existingMember));

      assertThrows(DuplicateEmailException.class,
        () -> memberService.update("1", updateData));
    }
  }

  // ========== delete Tests ==========

  @Nested
  @DisplayName("delete()")
  class DeleteTests {

    @Test
    @DisplayName("should delete member successfully")
    void shouldDeleteMember() {
      when(memberRepository.existsById("1")).thenReturn(true);
      doNothing().when(memberRepository).deleteById("1");

      assertDoesNotThrow(() -> memberService.delete("1"));
      verify(memberRepository, times(1)).deleteById("1");
    }

    @Test
    @DisplayName("should throw when member not found")
    void shouldThrowWhenNotFound() {
      when(memberRepository.existsById("999")).thenReturn(false);

      assertThrows(MemberNotFoundException.class,
        () -> memberService.delete("999"));

      verify(memberRepository, never()).deleteById(any());
    }
  }
}