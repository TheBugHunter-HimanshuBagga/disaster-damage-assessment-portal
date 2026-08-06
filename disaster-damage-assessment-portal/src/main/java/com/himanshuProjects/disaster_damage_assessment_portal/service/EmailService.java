package com.himanshuProjects.disaster_damage_assessment_portal.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp, String fullName);

    void sendWelcomeEmail(String toEmail, String fullName);
}