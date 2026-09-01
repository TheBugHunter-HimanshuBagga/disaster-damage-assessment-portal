package com.himanshuProjects.disaster_damage_assessment_portal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private EmailService emailService;

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() throws Exception {
        otpService = new OtpServiceImpl(emailService);
        setField("otpExpirationMinutes", 5);
        setField("resendCooldownSeconds", 30);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = OtpServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(otpService, value);
    }

    @Test
    @DisplayName("shouldGenerateAndSendOtp")
    void shouldGenerateAndSendOtp() {
        otpService.generateAndSendOtp("user@example.com", "John");
        verify(emailService).sendOtpEmail(eq("user@example.com"), anyString(), eq("John"));
    }

    @Test
    @DisplayName("shouldVerifyCorrectOtp")
    void shouldVerifyCorrectOtp() {
        otpService.generateAndSendOtp("user@example.com", "John");

        // We cannot know the random OTP, so verify OTP store behavior differently:
        // generate fails-safe by sending... we spy the generated value via email capture.
        org.mockito.ArgumentCaptor<String> otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("user@example.com"), otpCaptor.capture(), eq("John"));
        String generatedOtp = otpCaptor.getValue();

        assertThat(otpService.verifyOtp("user@example.com", generatedOtp)).isTrue();
    }

    @Test
    @DisplayName("shouldRejectInvalidOtp")
    void shouldRejectInvalidOtp() {
        otpService.generateAndSendOtp("user@example.com", "John");
        assertThat(otpService.verifyOtp("user@example.com", "000000")).isFalse();
    }

    @Test
    @DisplayName("shouldRejectOtpWhenNoneGenerated")
    void shouldRejectOtpWhenNoneGenerated() {
        assertThat(otpService.verifyOtp("never@example.com", "123456")).isFalse();
    }

    @Test
    @DisplayName("shouldBeCaseInsensitiveForEmailLookup")
    void shouldBeCaseInsensitiveForEmailLookup() {
        otpService.generateAndSendOtp("user@example.com", "John");
        org.mockito.ArgumentCaptor<String> otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("user@example.com"), otpCaptor.capture(), eq("John"));
        String otp = otpCaptor.getValue();
        assertThat(otpService.verifyOtp("USER@example.com", otp)).isTrue();
    }

    @Test
    @DisplayName("shouldRemoveOtpAfterSuccessfulVerification")
    void shouldRemoveOtpAfterSuccessfulVerification() {
        otpService.generateAndSendOtp("user@example.com", "John");
        org.mockito.ArgumentCaptor<String> otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("user@example.com"), otpCaptor.capture(), eq("John"));
        String otp = otpCaptor.getValue();

        assertThat(otpService.verifyOtp("user@example.com", otp)).isTrue();
        // Reuse should now fail
        assertThat(otpService.verifyOtp("user@example.com", otp)).isFalse();
    }

    @Test
    @DisplayName("shouldResendOtpAfterCooldown")
    void shouldResendOtpAfterCooldown() throws Exception {
        // set cooldown to 0 sec for test to allow immediate resend
        setField("resendCooldownSeconds", 0);
        otpService.generateAndSendOtp("user@example.com", "John");

        // ensure the clock advances past the cooldown timestamp (strict isBefore comparison)
        Thread.sleep(20);
        otpService.resendOtp("user@example.com", "John");
        verify(emailService, org.mockito.Mockito.times(2))
                .sendOtpEmail(eq("user@example.com"), anyString(), eq("John"));
    }

    @Test
    @DisplayName("shouldThrowWhenResendTooSoon")
    void shouldThrowWhenResendTooSoon() {
        otpService.generateAndSendOtp("user@example.com", "John");

        assertThatThrownBy(() -> otpService.resendOtp("user@example.com", "John"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wait");
    }

    @Test
    @DisplayName("shouldResendWhenNoExistingOtp")
    void shouldResendWhenNoExistingOtp() {
        otpService.resendOtp("none@example.com", "John");
        verify(emailService).sendOtpEmail(eq("none@example.com"), anyString(), eq("John"));
    }

    @Test
    @DisplayName("shouldClearOtp")
    void shouldClearOtp() {
        otpService.generateAndSendOtp("user@example.com", "John");
        org.mockito.ArgumentCaptor<String> otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("user@example.com"), otpCaptor.capture(), eq("John"));
        String otp = otpCaptor.getValue();

        otpService.clearOtp("user@example.com");
        assertThat(otpService.verifyOtp("user@example.com", otp)).isFalse();
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}
