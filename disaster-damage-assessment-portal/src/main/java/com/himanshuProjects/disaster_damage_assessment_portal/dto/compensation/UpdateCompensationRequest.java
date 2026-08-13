package com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCompensationRequest {

    @DecimalMin(value = "0.0", inclusive = true, message = "Approved amount cannot be negative")
    private BigDecimal approvedAmount;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
