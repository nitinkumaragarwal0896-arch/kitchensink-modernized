package com.modernizedkitechensink.kitchensinkmodernized.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailService.
 * 
 * Since EmailService is a MOCK service that logs emails to console,
 * we verify that:
 * 1. Methods execute without errors
 * 2. Correct log messages are generated
 * 3. HTML content is properly formatted
 * 4. All email configurations work correctly
 * 
 * NOTE: These tests verify the MOCK email service behavior.
 * For real email service tests, mock JavaMailSender instead.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        // Inject @Value properties using ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@memberhubpro.com");
        ReflectionTestUtils.setField(emailService, "fromName", "MemberHub Pro");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");

        // Set up log appender to capture log messages
        Logger logger = (Logger) LoggerFactory.getLogger(EmailService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmailSuccessfully() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "abc123token456";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Wait a bit for async method to complete (not needed since we're not actually async in test)
        // In real async tests, use @EnableAsync and wait for completion

        // Then - verify log messages contain expected content
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("📧 MOCK EMAIL")),
            "Should log mock email header");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(toEmail)),
            "Should log recipient email");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Password Reset Request")),
            "Should log email subject");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Password reset email sent to: " + toEmail)),
            "Should log success message");
    }

    @Test
    @DisplayName("Should include reset link in password reset email")
    void shouldIncludeResetLinkInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "resetToken123";
        String username = "testuser";
        String expectedResetLink = "http://localhost:5173/reset-password?token=" + resetToken;

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify reset link is in the logged HTML content
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(expectedResetLink)),
            "Should include reset link in email HTML");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(username)),
            "Should include username in email HTML");
    }

    @Test
    @DisplayName("Should handle password reset email with special characters in username")
    void shouldHandlePasswordResetEmailWithSpecialCharactersInUsername() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "test<user>";  // Special characters

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail(toEmail, resetToken, username),
            "Should handle special characters in username without errors"
        );

        // Verify email was sent
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Password reset email sent to: " + toEmail)),
            "Should log success message");
    }

    @Test
    @DisplayName("Should handle password reset with special characters in token")
    void shouldHandlePasswordResetWithSpecialCharactersInToken() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token+with/special=chars";
        String username = "testuser";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail(toEmail, resetToken, username),
            "Should handle special characters in token without errors"
        );

        // Verify email was sent
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Password reset email sent to: " + toEmail)),
            "Should log success message");
    }

    @Test
    @DisplayName("Should use configured fromEmail and fromName in emails")
    void shouldUseConfiguredFromEmailAndFromName() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";
        String expectedFromEmail = "noreply@memberhubpro.com";
        String expectedFromName = "MemberHub Pro";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify fromEmail and fromName are in the logged content
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(expectedFromEmail)),
            "Should use configured fromEmail");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(expectedFromName)),
            "Should use configured fromName");
    }

    @Test
    @DisplayName("Should use configured frontendUrl in reset link")
    void shouldUseConfiguredFrontendUrlInResetLink() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";
        String expectedFrontendUrl = "http://localhost:5173";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify frontendUrl is in the reset link
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(expectedFrontendUrl + "/reset-password?token=" + resetToken)),
            "Should use configured frontendUrl in reset link");
    }

    @Test
    @DisplayName("Should include HTML structure in password reset email")
    void shouldIncludeHtmlStructureInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify HTML structure elements
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("<!DOCTYPE html>")),
            "Should include DOCTYPE declaration");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("<html>")),
            "Should include HTML tags");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("<style>")),
            "Should include inline CSS styles");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Security Notice:")),
            "Should include security notice");
    }

    @Test
    @DisplayName("Should include security warnings in password reset email")
    void shouldIncludeSecurityWarningsInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify security warnings
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("This link will expire in 1 hour")),
            "Should warn about link expiration");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("If you didn't request this reset")),
            "Should include advice for non-requested resets");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Never share this link")),
            "Should warn against sharing link");
    }

    @Test
    @DisplayName("Should handle empty strings gracefully")
    void shouldHandleEmptyStringsGracefully() {
        // Given
        String toEmail = "";
        String resetToken = "";
        String username = "";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail(toEmail, resetToken, username),
            "Should handle empty strings without errors"
        );
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Given
        String toEmail = null;
        String resetToken = null;
        String username = null;

        // When & Then - should not throw exception (String.format handles nulls)
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail(toEmail, resetToken, username),
            "Should handle null values without errors"
        );
    }

    @Test
    @DisplayName("Should include reset button in HTML email")
    void shouldIncludeResetButtonInHtmlEmail() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify button is present
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Reset Password")),
            "Should include 'Reset Password' button text");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("class=\"button\"")),
            "Should include button CSS class");
    }

    @Test
    @DisplayName("Should include footer with copyright and automated message notice")
    void shouldIncludeFooterWithCopyrightAndAutomatedMessageNotice() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify footer content
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("&copy; 2026")),
            "Should include copyright year");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("This is an automated message")),
            "Should include automated message notice");
    }

    @Test
    @DisplayName("Should format username with strong tag")
    void shouldFormatUsernameWithStrongTag() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify username is bolded
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("<strong>" + username + "</strong>")),
            "Should format username with <strong> tag");
    }

    @Test
    @DisplayName("Should display reset link in both button and plain text")
    void shouldDisplayResetLinkInBothButtonAndPlainText() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";
        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify link appears twice (in button href and as plain text)
        long linkCount = logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .mapToLong(message -> {
                int count = 0;
                int index = 0;
                while ((index = message.indexOf(resetLink, index)) != -1) {
                    count++;
                    index += resetLink.length();
                }
                return count;
            })
            .sum();

        assertTrue(linkCount >= 2, 
            "Reset link should appear at least twice (in button and as plain text)");
    }

    @Test
    @DisplayName("Should use gradient header background")
    void shouldUseGradientHeaderBackground() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify gradient CSS
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("linear-gradient")),
            "Should use linear-gradient for header background");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("#667eea")),
            "Should include gradient color #667eea");
    }

    @Test
    @DisplayName("Should send multiple emails without interference")
    void shouldSendMultipleEmailsWithoutInterference() {
        // When - send multiple emails
        emailService.sendPasswordResetEmail("user1@example.com", "token1", "user1");
        emailService.sendPasswordResetEmail("user2@example.com", "token2", "user2");
        emailService.sendPasswordResetEmail("user3@example.com", "token3", "user3");

        // Then - verify all three were logged
        long mockEmailCount = logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains("📧 MOCK EMAIL"))
            .count();

        assertEquals(3, mockEmailCount, "Should log 3 mock emails");

        // Verify each user's email was sent
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("user1@example.com")),
            "Should send email to user1");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("user2@example.com")),
            "Should send email to user2");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("user3@example.com")),
            "Should send email to user3");
    }

    @Test
    @DisplayName("Should use responsive email styling")
    void shouldUseResponsiveEmailStyling() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify responsive styling elements
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("max-width: 600px")),
            "Should use max-width for responsive design");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("border-radius")),
            "Should use border-radius for modern look");
    }

    @Test
    @DisplayName("Should include warning section with yellow background")
    void shouldIncludeWarningSectionWithYellowBackground() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, username);

        // Then - verify warning section styling
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("#fff3cd")),
            "Should use yellow background for warning section");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("#ffc107")),
            "Should use yellow border for warning section");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("class=\"warning\"")),
            "Should include warning CSS class");
    }

    @Test
    @DisplayName("Should send email verification with all HTML elements")
    void shouldSendEmailVerificationWithAllHtmlElements() {
        // Given
        String toEmail = "newuser@example.com";
        String verificationToken = "verify123token";
        String username = "newuser";

        // When
        emailService.sendEmailVerificationEmail(toEmail, verificationToken, username);

        // Then - verify all HTML elements
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Verify Your Email")),
            "Should include email verification subject");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(username)),
            "Should include username in email body");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("http://localhost:5173/verify-email?token=" + verificationToken)),
            "Should include verification link");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Welcome to")),
            "Should include welcome message");
    }

    @Test
    @DisplayName("Should send account notification with 'created' action")
    void shouldSendAccountNotificationWithCreatedAction() {
        // Given
        String toEmail = "admin@example.com";
        String username = "newadmin";
        String action = "created";
        String adminUsername = "superadmin";

        // When
        emailService.sendAccountNotificationEmail(toEmail, username, action, adminUsername);

        // Then - verify "created" specific message
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("A new account has been created for you")),
            "Should include 'created' message");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(adminUsername)),
            "Should include admin username");
    }

    @Test
    @DisplayName("Should send account notification with 'updated' action")
    void shouldSendAccountNotificationWithUpdatedAction() {
        // Given
        String toEmail = "user@example.com";
        String username = "testuser";
        String action = "updated";
        String adminUsername = "admin";

        // When
        emailService.sendAccountNotificationEmail(toEmail, username, action, adminUsername);

        // Then - verify "updated" specific message
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Your account has been updated")),
            "Should include 'updated' message");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Account updated")),
            "Should include 'updated' in header");
    }

    @Test
    @DisplayName("Should send account notification with any other action (default to updated message)")
    void shouldSendAccountNotificationWithOtherAction() {
        // Given
        String toEmail = "user@example.com";
        String username = "testuser";
        String action = "modified"; // Neither "created" nor standard
        String adminUsername = "admin";

        // When
        emailService.sendAccountNotificationEmail(toEmail, username, action, adminUsername);

        // Then - verify default message (updated)
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Your account has been updated")),
            "Should default to 'updated' message for non-created actions");
    }

    @Test
    @DisplayName("Should handle null username in email methods")
    void shouldHandleNullUsernameInEmailMethods() {
        // Given
        String toEmail = "user@example.com";
        String token = "token123";

        // When - send with null username
        emailService.sendPasswordResetEmail(toEmail, token, null);

        // Then - should not throw exception and log email
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("MOCK EMAIL")),
            "Should still send email even with null username");
    }

    @Test
    @DisplayName("Should handle empty username in email methods")
    void shouldHandleEmptyUsernameInEmailMethods() {
        // Given
        String toEmail = "user@example.com";
        String token = "token123";

        // When - send with empty username
        emailService.sendPasswordResetEmail(toEmail, token, "");

        // Then - should not throw exception
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("MOCK EMAIL")),
            "Should still send email with empty username");
    }

    @Test
    @DisplayName("Should handle very long token in password reset email")
    void shouldHandleVeryLongTokenInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String veryLongToken = "a".repeat(500); // 500 character token
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, veryLongToken, username);

        // Then - should handle long token without truncation
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(veryLongToken)),
            "Should include full long token in email");
    }

    @Test
    @DisplayName("Should include all security warnings in password reset email")
    void shouldIncludeAllSecurityWarningsInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String token = "token123";
        String username = "testuser";

        // When
        emailService.sendPasswordResetEmail(toEmail, token, username);

        // Then - verify all security warnings
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("This link will expire in 1 hour")),
            "Should warn about link expiration");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("If you didn't request this reset")),
            "Should warn about unsolicited requests");

        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Never share this link")),
            "Should warn about sharing link");
    }

    @Test
    @DisplayName("Should include copyright footer in password reset email")
    void shouldIncludeCopyrightFooterInPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";

        // When
        emailService.sendPasswordResetEmail(toEmail, "token1", "user1");

        // Then
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Kitchensink Application")),
            "Password reset email should include copyright footer");
    }

    @Test
    @DisplayName("Should include copyright footer in email verification")
    void shouldIncludeCopyrightFooterInEmailVerification() {
        // Given
        String toEmail = "user@example.com";

        // When
        emailService.sendEmailVerificationEmail(toEmail, "token2", "user2");

        // Then
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Kitchensink Application")),
            "Email verification should include copyright footer");
    }

    @Test
    @DisplayName("Should include copyright footer in account notification")
    void shouldIncludeCopyrightFooterInAccountNotification() {
        // Given
        String toEmail = "user@example.com";

        // When
        emailService.sendAccountNotificationEmail(toEmail, "user3", "created", "admin");

        // Then
        assertTrue(logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("Kitchensink Application")),
            "Account notification should include copyright footer");
    }

    @Test
    @DisplayName("Should use consistent branding across all email types")
    void shouldUseConsistentBrandingAcrossAllEmailTypes() {
        // Given
        String toEmail = "user@example.com";

        // When - send different email types
        emailService.sendPasswordResetEmail(toEmail, "token1", "user1");
        emailService.sendEmailVerificationEmail(toEmail, "token2", "user2");
        emailService.sendAccountNotificationEmail(toEmail, "user3", "created", "admin");

        // Then - all should have consistent gradient color
        long gradientCount = logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains("linear-gradient(135deg, #667eea"))
            .count();
        
        assertTrue(gradientCount >= 3, 
            "All email types should use consistent gradient branding");
    }
}
