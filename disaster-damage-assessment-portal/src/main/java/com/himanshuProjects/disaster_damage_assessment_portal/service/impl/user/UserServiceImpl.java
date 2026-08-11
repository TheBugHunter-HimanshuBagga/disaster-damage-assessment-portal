package com.himanshuProjects.disaster_damage_assessment_portal.service.impl.user;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.CitizenProfileResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.ChangePasswordRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateAccountStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateCitizenProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.CitizenProfile;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.CitizenProfileRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "fullName", "email", "phoneNumber", "gender",
            "role", "accountStatus", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public UserServiceImpl(UserRepository userRepository,
                           DistrictRepository districtRepository,
                           CitizenProfileRepository citizenProfileRepository,
                           PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        log.info("Fetching profile for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        log.info("Updating profile for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email is already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new ConflictException("Phone number is already registered: " + request.getPhoneNumber());
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getDistrictId() != null) {
            District district = districtRepository.findById(request.getDistrictId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "District", "id", request.getDistrictId()));
            user.setDistrict(district);
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for email: {}", email);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Changing password for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for email: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse searchUsers(String search, RoleType role, AccountStatus status,
                                         Long districtId, int page, int size,
                                         String sortBy, String sortDirection) {
        log.info("Searching users - search: {}, role: {}, status: {}, districtId: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, role, status, districtId, page, size, sortBy, sortDirection);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: " + sortBy
                    + ". Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage = userRepository.searchUsers(
                search, role, status, districtId, pageable);

        java.util.List<UserResponse> users = userPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return UserPageResponse.builder()
                .users(users)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenProfileResponse getCitizenProfile(String email) {
        log.info("Fetching citizen profile for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        CitizenProfile profile = citizenProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CitizenProfile", "userId", user.getId()));

        return mapToCitizenProfileResponse(profile);
    }

    @Override
    @Transactional
    public CitizenProfileResponse updateCitizenProfile(String email, UpdateCitizenProfileRequest request) {
        log.info("Updating citizen profile for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        CitizenProfile profile = citizenProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("No citizen profile found for user: {}. Creating new profile.", email);
                    CitizenProfile newProfile = new CitizenProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setAadhaarNumber(request.getAadhaarNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAddress(request.getAddress());
        profile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        profile.setEmergencyContact(request.getEmergencyContact());

        CitizenProfile savedProfile = citizenProfileRepository.save(profile);
        log.info("Citizen profile updated successfully for email: {}", email);
        return mapToCitizenProfileResponse(savedProfile);
    }

    @Override
    @Transactional
    public UserResponse updateUserAccountStatus(Long userId, UpdateAccountStatusRequest request) {
        log.info("Updating account status for user ID: {} to {}", userId, request.getAccountStatus());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setAccountStatus(request.getAccountStatus());
        User updatedUser = userRepository.save(user);

        log.info("Account status updated successfully for user ID: {}", userId);
        return mapToResponse(updatedUser);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        if (user.getDistrict() != null) {
            response.setDistrictName(user.getDistrict().getName());
            if (user.getDistrict().getState() != null) {
                response.setStateName(user.getDistrict().getState().getName());
            }
        }
        return response;
    }

    private CitizenProfileResponse mapToCitizenProfileResponse(CitizenProfile profile) {
        CitizenProfileResponse response = CitizenProfileResponse.builder()
                .id(profile.getId())
                .aadhaarNumber(profile.getAadhaarNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .address(profile.getAddress())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .emergencyContact(profile.getEmergencyContact())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();

        if (profile.getUser() != null) {
            response.setUserFullName(profile.getUser().getFullName());
            response.setUserEmail(profile.getUser().getEmail());
        }

        return response;
    }
}
