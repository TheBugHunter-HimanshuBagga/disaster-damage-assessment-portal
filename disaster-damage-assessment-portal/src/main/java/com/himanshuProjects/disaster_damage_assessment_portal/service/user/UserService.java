package com.himanshuProjects.disaster_damage_assessment_portal.service.user;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.CitizenProfileResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.ChangePasswordRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateAccountStatusRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateCitizenProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UpdateProfileRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.user.UserResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;

public interface UserService {

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    UserResponse getUserById(Long id);

    UserPageResponse searchUsers(String search, RoleType role, AccountStatus status,
                                  Long districtId, int page, int size,
                                  String sortBy, String sortDirection);

    CitizenProfileResponse getCitizenProfile(String email);

    CitizenProfileResponse updateCitizenProfile(String email, UpdateCitizenProfileRequest request);

    UserResponse updateUserAccountStatus(Long userId, UpdateAccountStatusRequest request);
}
