package com.modernizedkitechensink.kitchensinkmodernized.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email Service for sending emails asynchronously.
 * 
 * NOTE: For development, this is a MOCK service that logs emails to console.
 * For production, enable spring-boot-starter-mail and uncomment real email logic.
 * 
 * Features:
 * - Async email sending (non-blocking)
 * - HTML email support
 * - Template-based emails
 * - Error handling & logging
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${app.mail.from:noreply@kitchensink.com}")
    private String fromEmail;

    @Value("${app.mail.fromName:Kitchensink Application}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Send password reset email with reset link.
     * 
     * Runs on dedicated EMAIL thread pool (emailTaskExecutor).
     * This ensures email sending never blocks due to bulk operations.
     * 
     * @param toEmail Recipient email
     * @param resetToken Reset token (plain text, will be included in URL)
     * @param username User's username
     */
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String resetToken, String username) {
        try {
            String subject = "Password Reset Request - Kitchensink Application";
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            
            String htmlContent = buildPasswordResetEmailHtml(username, resetLink);
            
            sendHtmlEmail(toEmail, subject, htmlContent);
            
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    /**
     * Send email verification email with verification link.
     * 
     * Runs on dedicated EMAIL thread pool (emailTaskExecutor).
     * 
     * @param toEmail Recipient email
     * @param verificationToken Verification token
     * @param username User's username
     */
    @Async("emailTaskExecutor")
    public void sendEmailVerificationEmail(String toEmail, String verificationToken, String username) {
        try {
            String subject = "Email Verification - Kitchensink Application";
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
            
            String htmlContent = buildEmailVerificationHtml(username, verificationLink);
            
            sendHtmlEmail(toEmail, subject, htmlContent);
            
            log.info("Email verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email verification email to: {}", toEmail, e);
        }
    }

    /**
     * Send notification email when admin creates/updates user account.
     * 
     * Runs on dedicated EMAIL thread pool (emailTaskExecutor).
     * 
     * @param toEmail Recipient email
     * @param username User's username
     * @param action Action performed (created/updated)
     * @param adminUsername Admin who performed the action
     */
    @Async("emailTaskExecutor")
    public void sendAccountNotificationEmail(String toEmail, String username, String action, String adminUsername) {
        try {
            String subject = "Account " + action + " - Kitchensink Application";
            
            String htmlContent = buildAccountNotificationHtml(username, action, adminUsername);
            
            sendHtmlEmail(toEmail, subject, htmlContent);
            
            log.info("Account notification email sent to: {} (action: {})", toEmail, action);
        } catch (Exception e) {
            log.error("Failed to send account notification email to: {}", toEmail, e);
        }
    }

    /**
     * Send HTML email (MOCK - just logs to console for development).
     * For production: Enable spring-boot-starter-mail and implement real email sending.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("\n" +
            "====================================================\n" +
            "📧 MOCK EMAIL (Development Mode)\n" +
            "====================================================\n" +
            "From: {} <{}>\n" +
            "To: {}\n" +
            "Subject: {}\n" +
            "====================================================\n" +
            "{}\n" +
            "====================================================\n",
            fromName, fromEmail, to, subject, htmlContent);
    }

    /**
     * Build HTML content for password reset email.
     */
    private String buildPasswordResetEmailHtml(String username, String resetLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>We received a request to reset your password for your Kitchensink account.</p>
                        <p>Click the button below to reset your password:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background: #fff; padding: 10px; border-radius: 5px;">%s</p>
                        <div class="warning">
                            <strong>Security Notice:</strong>
                            <ul>
                                <li>This link will expire in 1 hour</li>
                                <li>If you didn't request this reset, please ignore this email</li>
                                <li>Never share this link with anyone</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Kitchensink Application. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, username, resetLink, resetLink);
    }

    /**
     * Build HTML content for email verification email.
     */
    private String buildEmailVerificationHtml(String username, String verificationLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Verify Your Email</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Welcome to Kitchensink! Please verify your email address to activate your account.</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify Email</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background: #fff; padding: 10px; border-radius: 5px;">%s</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Kitchensink Application. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, username, verificationLink, verificationLink);
    }

    /**
     * Build HTML content for account notification email.
     */
    private String buildAccountNotificationHtml(String username, String action, String adminUsername) {
        String message = action.equalsIgnoreCase("created") 
            ? "A new account has been created for you by an administrator."
            : "Your account has been updated by an administrator.";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Account %s</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>%s</p>
                        <p><strong>Action performed by:</strong> %s</p>
                        <p style="text-align: center;">
                            <a href="%s/login" class="button">Login to Your Account</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Kitchensink Application. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, action, username, message, adminUsername, frontendUrl);
    }
}

