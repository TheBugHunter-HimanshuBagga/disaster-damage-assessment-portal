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
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.BadRequestException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ConflictException;
import com.himanshuProjects.disaster_damage_assessment_portal.exception.ResourceNotFoundException;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.CitizenProfileRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private CitizenProfileRepository citizenProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ModelMapper modelMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);
        userService = new UserServiceImpl(
                userRepository, districtRepository, citizenProfileRepository,
                passwordEncoder, modelMapper);
    }

    private State defaultState() {
        return TestDataFactory.createState(1L, "TestState", "TS");
    }

    private District defaultDistrict() {
        return TestDataFactory.createDistrict(1L, "TestDistrict", defaultState());
    }

    private User defaultUser() {
        return TestDataFactory.createCitizen(1L, "citizen@example.com");
    }

    // ==================== getProfile ====================

    @Test
    @DisplayName("shouldReturnProfileForExistingUser")
    void shouldReturnProfileForExistingUser() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile("citizen@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("citizen@example.com");
        assertThat(response.getFullName()).isEqualTo("Test CITIZEN");
        verify(userRepository).findByEmail("citizen@example.com");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenEmailDoesNotExist")
    void shouldThrowResourceNotFoundExceptionWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository).findByEmail("missing@example.com");
    }

    // ==================== updateProfile ====================

    @Test
    @DisplayName("shouldUpdateFullNameSuccessfully")
    void shouldUpdateFullNameSuccessfully() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        UserResponse response = userService.updateProfile("citizen@example.com", request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldUpdateEmailSuccessfullyWhenNotDuplicate")
    void shouldUpdateEmailSuccessfullyWhenNotDuplicate() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("new@example.com");

        UserResponse response = userService.updateProfile("citizen@example.com", request);

        assertThat(response).isNotNull();
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenEmailAlreadyRegistered")
    void shouldThrowConflictExceptionWhenEmailAlreadyRegistered() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");

        assertThatThrownBy(() -> userService.updateProfile("citizen@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldNotCheckEmailConflictWhenEmailIsSameAsCurrent")
    void shouldNotCheckEmailConflictWhenEmailIsSameAsCurrent() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("citizen@example.com");

        userService.updateProfile("citizen@example.com", request);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldUpdatePhoneNumberSuccessfullyWhenNotDuplicate")
    void shouldUpdatePhoneNumberSuccessfullyWhenNotDuplicate() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("9999999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("9999999999");

        userService.updateProfile("citizen@example.com", request);

        verify(userRepository).existsByPhoneNumber("9999999999");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowConflictExceptionWhenPhoneNumberAlreadyRegistered")
    void shouldThrowConflictExceptionWhenPhoneNumberAlreadyRegistered() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("8888888888")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("8888888888");

        assertThatThrownBy(() -> userService.updateProfile("citizen@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Phone number is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldUpdateGenderSuccessfully")
    void shouldUpdateGenderSuccessfully() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setGender(Gender.FEMALE);

        userService.updateProfile("citizen@example.com", request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldUpdateDistrictSuccessfully")
    void shouldUpdateDistrictSuccessfully() {
        User user = defaultUser();
        District newDistrict = TestDataFactory.createDistrict(2L, "NewDistrict", defaultState());
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(districtRepository.findById(2L)).thenReturn(Optional.of(newDistrict));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDistrictId(2L);

        userService.updateProfile("citizen@example.com", request);

        verify(districtRepository).findById(2L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenDistrictDoesNotExistOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenDistrictDoesNotExistOnUpdate() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(districtRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDistrictId(999L);

        assertThatThrownBy(() -> userService.updateProfile("citizen@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("District");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserDoesNotExistOnUpdate")
    void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExistOnUpdate() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");

        assertThatThrownBy(() -> userService.updateProfile("missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldSkipNullFieldsOnUpdateProfile")
    void shouldSkipNullFieldsOnUpdateProfile() {
        User user = defaultUser();
        String originalEmail = user.getEmail();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Only Name Update");

        userService.updateProfile("citizen@example.com", request);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(districtRepository, never()).findById(anyLong());
        verify(userRepository).save(any(User.class));
    }

    // ==================== changePassword ====================

    @Test
    @DisplayName("shouldChangePasswordSuccessfully")
    void shouldChangePasswordSuccessfully() {
        User user = defaultUser();
        String originalPassword = user.getPassword();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", originalPassword)).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        userService.changePassword("citizen@example.com", request);

        verify(passwordEncoder).matches("oldPassword", originalPassword);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenCurrentPasswordIsIncorrect")
    void shouldThrowBadRequestExceptionWhenCurrentPasswordIsIncorrect() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        assertThatThrownBy(() -> userService.changePassword("citizen@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionWhenNewPasswordAndConfirmPasswordDoNotMatch")
    void shouldThrowBadRequestExceptionWhenNewPasswordAndConfirmPasswordDoNotMatch() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", user.getPassword())).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        assertThatThrownBy(() -> userService.changePassword("citizen@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password and confirmation password do not match");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenChangingPasswordForUnknownUser")
    void shouldThrowResourceNotFoundExceptionWhenChangingPasswordForUnknownUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        assertThatThrownBy(() -> userService.changePassword("missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== getUserById ====================

    @Test
    @DisplayName("shouldReturnUserById")
    void shouldReturnUserById() {
        User user = defaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFoundById")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundById() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    // ==================== searchUsers ====================

    @Test
    @DisplayName("shouldSearchUsersWithPaginationAscending")
    void shouldSearchUsersWithPaginationAscending() {
        User user = defaultUser();
        Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.searchUsers(null, null, null, null, PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("fullName").ascending())))
                .thenReturn(userPage);

        UserPageResponse response = userService.searchUsers(
                null, null, null, null, 0, 10, "fullName", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getUsers()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldSearchUsersWithPaginationDescending")
    void shouldSearchUsersWithPaginationDescending() {
        User user = defaultUser();
        Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.searchUsers(null, null, null, null, PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("fullName").descending())))
                .thenReturn(userPage);

        UserPageResponse response = userService.searchUsers(
                null, null, null, null, 0, 10, "fullName", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getUsers()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenNoUsersMatchSearch")
    void shouldReturnEmptyPageWhenNoUsersMatchSearch() {
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(userRepository.searchUsers("nomatch", RoleType.CITIZEN, AccountStatus.ACTIVE,
                1L, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending())))
                .thenReturn(emptyPage);

        UserPageResponse response = userService.searchUsers(
                "nomatch", RoleType.CITIZEN, AccountStatus.ACTIVE, 1L, 0, 10, "email", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getUsers()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("shouldThrowBadRequestExceptionForInvalidSortField")
    void shouldThrowBadRequestExceptionForInvalidSortField() {
        assertThatThrownBy(() -> userService.searchUsers(
                null, null, null, null, 0, 10, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // ==================== getCitizenProfile ====================

    @Test
    @DisplayName("shouldReturnCitizenProfileSuccessfully")
    void shouldReturnCitizenProfileSuccessfully() {
        User user = defaultUser();
        CitizenProfile profile = TestDataFactory.createCitizenProfile(1L, user);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(citizenProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        CitizenProfileResponse response = userService.getCitizenProfile("citizen@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAadhaarNumber()).isEqualTo("123456789012");
        assertThat(response.getAddress()).isEqualTo("123 Test Street");
        assertThat(response.getUserFullName()).isEqualTo("Test CITIZEN");
        assertThat(response.getUserEmail()).isEqualTo("citizen@example.com");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFoundForCitizenProfile")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundForCitizenProfile() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCitizenProfile("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenCitizenProfileNotFoundForUser")
    void shouldThrowResourceNotFoundExceptionWhenCitizenProfileNotFoundForUser() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(citizenProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCitizenProfile("citizen@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CitizenProfile");
    }

    // ==================== updateCitizenProfile ====================

    @Test
    @DisplayName("shouldUpdateExistingCitizenProfileSuccessfully")
    void shouldUpdateExistingCitizenProfileSuccessfully() {
        User user = defaultUser();
        CitizenProfile existingProfile = TestDataFactory.createCitizenProfile(1L, user);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(citizenProfileRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCitizenProfileRequest request = new UpdateCitizenProfileRequest();
        request.setAadhaarNumber("999988887777");
        request.setDateOfBirth(LocalDate.of(1995, 5, 15));
        request.setAddress("456 New Street");
        request.setProfilePhotoUrl("http://example.com/newphoto.jpg");
        request.setEmergencyContact("8765432109");

        CitizenProfileResponse response = userService.updateCitizenProfile("citizen@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getAadhaarNumber()).isEqualTo("999988887777");
        assertThat(response.getAddress()).isEqualTo("456 New Street");
        verify(citizenProfileRepository).save(any(CitizenProfile.class));
    }

    @Test
    @DisplayName("shouldCreateNewCitizenProfileWhenNoneExists")
    void shouldCreateNewCitizenProfileWhenNoneExists() {
        User user = defaultUser();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(user));
        when(citizenProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCitizenProfileRequest request = new UpdateCitizenProfileRequest();
        request.setAadhaarNumber("999988887777");
        request.setDateOfBirth(LocalDate.of(1995, 5, 15));
        request.setAddress("456 New Street");
        request.setProfilePhotoUrl("http://example.com/newphoto.jpg");
        request.setEmergencyContact("8765432109");

        CitizenProfileResponse response = userService.updateCitizenProfile("citizen@example.com", request);

        assertThat(response).isNotNull();
        verify(citizenProfileRepository).save(any(CitizenProfile.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnUpdateCitizenProfile")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnUpdateCitizenProfile() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UpdateCitizenProfileRequest request = new UpdateCitizenProfileRequest();
        request.setAadhaarNumber("999988887777");
        request.setDateOfBirth(LocalDate.of(1995, 5, 15));
        request.setAddress("456 New Street");
        request.setEmergencyContact("8765432109");

        assertThatThrownBy(() -> userService.updateCitizenProfile("missing@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(citizenProfileRepository, never()).save(any(CitizenProfile.class));
    }

    // ==================== updateUserAccountStatus ====================

    @Test
    @DisplayName("shouldUpdateUserAccountStatusSuccessfully")
    void shouldUpdateUserAccountStatusSuccessfully() {
        User user = defaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest();
        request.setAccountStatus(AccountStatus.SUSPENDED);

        UserResponse response = userService.updateUserAccountStatus(1L, request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnStatusUpdate")
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnStatusUpdate() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest();
        request.setAccountStatus(AccountStatus.SUSPENDED);

        assertThatThrownBy(() -> userService.updateUserAccountStatus(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository, never()).save(any(User.class));
    }
}
