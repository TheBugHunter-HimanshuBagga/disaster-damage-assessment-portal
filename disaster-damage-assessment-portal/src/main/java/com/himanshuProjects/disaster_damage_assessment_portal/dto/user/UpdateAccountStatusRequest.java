package com.himanshuProjects.disaster_damage_assessment_portal.dto.user;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}
