package com.himanshuProjects.disaster_damage_assessment_portal.controller.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpVerifyRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.OtpService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.auth.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class OtpController {

    private static final Logger log = LoggerFactory.getLogger(OtpController.class);

    private final OtpService otpService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public OtpController(OtpService otpService,
                         AuthService authService,
                         UserRepository userRepository) {
        this.otpService = otpService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        log.info("Resend OTP request for email: {}", request.getEmail());

        try {
            // Fetch user's full name for the email greeting
            String fullName = userRepository.findByEmail(request.getEmail())
                    .map(User::getFullName)
                    .orElse("User");

            otpService.resendOtp(request.getEmail(), fullName);
            return ResponseEntity.ok(OtpResponse.builder()
                    .success(true)
                    .message("OTP resent successfully. Please check your email.")
                    .email(request.getEmail())
                    .build());
        } catch (IllegalStateException e) {
            log.warn("Resend OTP failed for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .email(request.getEmail())
                    .build());
        } catch (Exception e) {
            log.error("Error resending OTP for email: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError().body(OtpResponse.builder()
                    .success(false)
                    .message("Failed to resend OTP. Please try again later.")
                    .email(request.getEmail())
                    .build());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("Verify OTP request for email: {}", request.getEmail());

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (isValid) {
            // Activate the user's account
            authService.activateAccount(request.getEmail());
            return ResponseEntity.ok(OtpResponse.builder()
                    .success(true)
                    .message("Email verified successfully! Your account is now active.")
                    .email(request.getEmail())
                    .build());
        } else {
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP. Please try again or resend OTP.")
                    .email(request.getEmail())
                    .build());
        }
    }
}