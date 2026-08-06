package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.AuthResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.LoginRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.RegisterRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
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
import com.himanshuProjects.disaster_damage_assessment_portal.service.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           DistrictRepository districtRepository,
                           PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper,
                           JwtService jwtService,
                           CustomUserDetailsService customUserDetailsService,
                           OtpService otpService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ConflictException("Phone number is already registered: " + request.getPhoneNumber());
        }

        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District", "id", request.getDistrictId()));

        User user = modelMapper.map(request, User.class);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleType.CITIZEN);
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setDistrict(district);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Send OTP email for verification
        otpService.generateAndSendOtp(savedUser.getEmail(), savedUser.getFullName());

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(savedUser.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        AuthResponse response = modelMapper.map(savedUser, AuthResponse.class);
        response.setMessage("Registration successful. Please verify your email using the OTP sent to your inbox.");
        response.setToken(jwtToken);

        return response;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ConflictException("Invalid email or password");
        }

        // Block login if account is not yet verified
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            log.warn("Login blocked for unverified account: {}", user.getEmail());
            throw new BadRequestException(
                    "Account is not verified. Please verify your email using the OTP first."
            );
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        AuthResponse response = modelMapper.map(user, AuthResponse.class);
        response.setMessage("Login successful");
        response.setToken(jwtToken);

        log.info("User logged in successfully: {}", user.getEmail());
        return response;
    }

    @Override
    @Transactional
    public void activateAccount(String email) {
        log.info("Activating account for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            log.info("Account already active for email: {}", email);
            return;
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        log.info("Account activated successfully for email: {}", email);
    }
}