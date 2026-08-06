package com.himanshuProjects.disaster_damage_assessment_portal.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Disaster Portal - Email Verification OTP");

            String htmlContent = buildOtpEmailHtml(fullName, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Disaster Damage Assessment Portal");

            String htmlContent = buildWelcomeEmailHtml(fullName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildOtpEmailHtml(String fullName, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #f8f9fa; border-radius: 10px; padding: 30px; border: 1px solid #e9ecef;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #2c3e50; margin: 0;">Disaster Damage Assessment Portal</h1>
                        <p style="color: #6c757d; margin: 5px 0 0;">Government of India</p>
                    </div>
                    
                    <h2 style="color: #2c3e50;">Email Verification Required</h2>
                    
                    <p>Dear <strong>%s</strong>,</p>
                    
                    <p>Thank you for registering with the Disaster Damage Assessment Portal. To complete your registration, please verify your email address using the OTP below:</p>
                    
                    <div style="background-color: #ffffff; border: 2px dashed #007bff; border-radius: 8px; padding: 20px; text-align: center; margin: 25px 0;">
                        <p style="margin: 0 0 10px; color: #6c757d; font-size: 14px;">Your One-Time Password (OTP)</p>
                        <h1 style="color: #007bff; margin: 0; font-size: 36px; letter-spacing: 8px;">%s</h1>
                    </div>
                    
                    <p><strong>Important:</strong></p>
                    <ul>
                        <li>This OTP is valid for <strong>5 minutes</strong> only</li>
                        <li>Do not share this OTP with anyone</li>
                        <li>If you didn't request this, please ignore this email</li>
                    </ul>
                    
                    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 25px 0;">
                    
                    <p style="color: #6c757d; font-size: 12px; text-align: center;">
                        This is an automated email. Please do not reply to this message.<br>
                        Disaster Damage Assessment Portal &copy; 2026
                    </p>
                </div>
            </body>
            </html>
            """.formatted(fullName, otp);
    }

    private String buildWelcomeEmailHtml(String fullName) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #f8f9fa; border-radius: 10px; padding: 30px; border: 1px solid #e9ecef;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #2c3e50; margin: 0;">Disaster Damage Assessment Portal</h1>
                        <p style="color: #6c757d; margin: 5px 0 0;">Government of India</p>
                    </div>
                    
                    <h2 style="color: #28a745;">Welcome!</h2>
                    
                    <p>Dear <strong>%s</strong>,</p>
                    
                    <p>Your email has been successfully verified! You can now access all features of the Disaster Damage Assessment Portal.</p>
                    
                    <p>With your account, you can:</p>
                    <ul>
                        <li>Submit disaster damage reports</li>
                        <li>Track the status of your reports</li>
                        <li>Receive real-time notifications</li>
                        <li>Access compensation information</li>
                    </ul>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="http://localhost:8081" style="background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                            Access Portal
                        </a>
                    </div>
                    
                    <hr style="border: none; border-top: 1px solid #e9ecef; margin: 25px 0;">
                    
                    <p style="color: #6c757d; font-size: 12px; text-align: center;">
                        Disaster Damage Assessment Portal &copy; 2026<br>
                        For support, contact: support@disasterportal.gov.in
                    </p>
                </div>
            </body>
            </html>
            """.formatted(fullName);
    }
}