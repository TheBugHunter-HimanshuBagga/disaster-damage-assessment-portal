package com.himanshuProjects.disaster_damage_assessment_portal.service;

public interface OtpService {

    void generateAndSendOtp(String email, String fullName);

    boolean verifyOtp(String email, String otp);

    void resendOtp(String email, String fullName);

    void clearOtp(String email);
}