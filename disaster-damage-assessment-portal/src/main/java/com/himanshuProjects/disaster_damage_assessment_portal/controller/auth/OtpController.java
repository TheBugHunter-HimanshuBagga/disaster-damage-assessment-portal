package com.himanshuProjects.disaster_damage_assessment_portal.controller.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.OtpVerifyRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.OtpService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "OTP Verification", description = "Email OTP verification for account activation")
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
    @Operation(summary = "Resend OTP to email", description = "Resends the verification OTP to the user's email address. Cooldown period applies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
            @ApiResponse(responseCode = "400", description = "Cooldown period active or user not found")
    })
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        log.info("Resend OTP request for email: {}", request.getEmail());

        String fullName = userRepository.findByEmail(request.getEmail())
                .map(User::getFullName)
                .orElse("User");

        otpService.resendOtp(request.getEmail(), fullName);
        return ResponseEntity.ok(OtpResponse.builder()
                .success(true)
                .message("OTP resent successfully. Please check your email.")
                .email(request.getEmail())
                .build());
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and activate account", description = "Verifies the OTP code and activates the user's account if valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account verified and activated"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("Verify OTP request for email: {}", request.getEmail());

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (isValid) {
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
