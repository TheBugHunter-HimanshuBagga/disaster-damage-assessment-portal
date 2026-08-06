package com.himanshuProjects.disaster_damage_assessment_portal.service.auth;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.AuthResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.LoginRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void activateAccount(String email);
}