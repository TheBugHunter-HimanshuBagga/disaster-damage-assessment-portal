package com.himanshuProjects.disaster_damage_assessment_portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private static final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();

    @Value("${otp.expiration.minutes:5}")
    private int otpExpirationMinutes;

    @Value("${otp.resend.cooldown.seconds:30}")
    private int resendCooldownSeconds;

    private final EmailService emailService;
    private final Random random = new Random();

    public OtpServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void generateAndSendOtp(String email, String fullName) {
        log.info("Generating OTP for email: {}", email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Store OTP with timestamp
        OtpData otpData = new OtpData(otp, LocalDateTime.now());
        otpStore.put(email.toLowerCase(), otpData);

        // Send OTP via email
        emailService.sendOtpEmail(email, otp, fullName);

        log.info("OTP generated and sent for email: {}", email);
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String normalizedEmail = email.toLowerCase();
        OtpData otpData = otpStore.get(normalizedEmail);

        if (otpData == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }

        // Check if OTP is expired
        if (isExpired(otpData)) {
            log.warn("OTP expired for email: {}", email);
            otpStore.remove(normalizedEmail);
            return false;
        }

        // Verify OTP
        if (!otpData.otp.equals(otp)) {
            log.warn("Invalid OTP provided for email: {}", email);
            return false;
        }

        // OTP verified successfully - remove it
        otpStore.remove(normalizedEmail);
        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    @Override
    public void resendOtp(String email, String fullName) {
        String normalizedEmail = email.toLowerCase();
        OtpData existingOtp = otpStore.get(normalizedEmail);

        // Check if resend is allowed (cooldown period)
        if (existingOtp != null && !isResendAllowed(existingOtp)) {
            long remainingSeconds = calculateRemainingCooldown(existingOtp);
            log.warn("Resend OTP attempted too soon for email: {}. Remaining cooldown: {} seconds", email, remainingSeconds);
            throw new IllegalStateException("Please wait " + remainingSeconds + " seconds before resending OTP");
        }

        // Remove old OTP and generate new one
        otpStore.remove(normalizedEmail);
        generateAndSendOtp(email, fullName);
        log.info("OTP resent for email: {}", email);
    }

    @Override
    public void clearOtp(String email) {
        otpStore.remove(email.toLowerCase());
        log.debug("OTP cleared for email: {}", email);
    }

    private boolean isExpired(OtpData otpData) {
        return otpData.createdAt.plusMinutes(otpExpirationMinutes).isBefore(LocalDateTime.now());
    }

    private boolean isResendAllowed(OtpData otpData) {
        return otpData.createdAt.plusSeconds(resendCooldownSeconds).isBefore(LocalDateTime.now());
    }

    private long calculateRemainingCooldown(OtpData otpData) {
        LocalDateTime allowedTime = otpData.createdAt.plusSeconds(resendCooldownSeconds);
        return TimeUnit.SECONDS.toSeconds(java.time.Duration.between(LocalDateTime.now(), allowedTime).toMillis());
    }

    private static class OtpData {
        final String otp;
        final LocalDateTime createdAt;

        OtpData(String otp, LocalDateTime createdAt) {
            this.otp = otp;
            this.createdAt = createdAt;
        }
    }
}