package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.AuthResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.LoginRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.RegisterRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.security.CustomUserDetailsService;
import com.himanshuProjects.disaster_damage_assessment_portal.security.JwtService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.EmailService;
import com.himanshuProjects.disaster_damage_assessment_portal.service.OtpService;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private OtpService otpService;
    @Mock
    private EmailService emailService;

    private ModelMapper modelMapper;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);
        authService = new AuthServiceImpl(
                userRepository, districtRepository, passwordEncoder, modelMapper,
                jwtService, customUserDetailsService, otpService, emailService);
    }

    private District validDistrict() {
        State state = TestDataFactory.createState(1L, "TestState", "TS");
        return TestDataFactory.createDistrict(1L, "TestDistrict", state);
    }

    @Test
    @DisplayName("shouldRegisterUserSuccessfully")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = TestDataFactory.createRegisterRequest();
        District district = validDistrict();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = TestDataFactory.createCitizen(1L, request.getEmail());
        savedUser.setFullName(request.getFullName());
        savedUser.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(customUserDetailsService.loadUserByUsername(request.getEmail()))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        request.getEmail(), "encodedPassword", java.util.Collections.emptyList()));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo(request.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertThat(captured.getPassword()).isEqualTo("encodedPassword");
        assertThat(captured.getRole()).isEqualTo(RoleType.CITIZEN);
        assertThat(captured.getAccountStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(captured.getDistrict()).isEqualTo(district);

        verify(otpService).generateAndSendOtp(request.getEmail(), request.getFullName());
    }

    @Test
    @DisplayName("shouldRejectDuplicateEmail")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = TestDataFactory.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldRejectDuplicatePhoneNumber")
    void shouldRejectDuplicatePhoneNumber() {
        RegisterRequest request = TestDataFactory.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Phone number is already registered");
    }

    @Test
    @DisplayName("shouldThrowWhenDistrictDoesNotExistOnRegister")
    void shouldThrowWhenDistrictDoesNotExistOnRegister() {
        RegisterRequest request = TestDataFactory.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(districtRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldHashPasswordOnRegistration")
    void shouldHashPasswordOnRegistration() {
        RegisterRequest request = TestDataFactory.createRegisterRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(districtRepository.findById(1L)).thenReturn(Optional.of(validDistrict()));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class)))
                .thenReturn(TestDataFactory.createCitizen(1L, request.getEmail()));

        ArgumentCaptor<String> rawPasswordCaptor = ArgumentCaptor.forClass(String.class);
        authService.register(request);

        verify(passwordEncoder).encode(rawPasswordCaptor.capture());
        assertThat(rawPasswordCaptor.getValue()).isEqualTo("password123");
        assertThat(rawPasswordCaptor.getValue()).isNotEqualTo("hashed-password");
    }

    @Test
    @DisplayName("shouldLoginSuccessfullyForActiveAccount")
    void shouldLoginSuccessfullyForActiveAccount() {
        String email = "john.doe@example.com";
        User user = TestDataFactory.createCitizen(1L, email);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPassword("hashed");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername(email))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        email, "hashed", java.util.Collections.emptyList()));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("shouldRejectLoginForWrongPassword")
    void shouldRejectLoginForWrongPassword() {
        String email = "john.doe@example.com";
        User user = TestDataFactory.createCitizen(1L, email);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPassword("hashed");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("shouldRejectLoginForUnknownEmail")
    void shouldRejectLoginForUnknownEmail() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("shouldBlockLoginForUnverifiedAccount")
    void shouldBlockLoginForUnverifiedAccount() {
        String email = "pending@example.com";
        User user = TestDataFactory.createCitizen(1L, email);
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is not verified");

        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("shouldActivateAccount")
    void shouldActivateAccount() {
        String email = "john.doe@example.com";
        User user = TestDataFactory.createCitizen(1L, email);
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.activateAccount(email);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail(email, user.getFullName());
    }

    @Test
    @DisplayName("shouldNotResendWelcomeEmailWhenAlreadyActive")
    void shouldNotResendWelcomeEmailWhenAlreadyActive() {
        String email = "active@example.com";
        User user = TestDataFactory.createCitizen(1L, email);
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authService.activateAccount(email);

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("shouldThrowWhenActivatingUnknownUser")
    void shouldThrowWhenActivatingUnknownUser() {
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.activateAccount(email))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
