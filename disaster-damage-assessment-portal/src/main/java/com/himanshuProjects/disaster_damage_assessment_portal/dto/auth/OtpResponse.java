package com.himanshuProjects.disaster_damage_assessment_portal.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponse {

    private String message;
    private boolean success;
    private String email;
    private String token; // JWT token after successful verification
}