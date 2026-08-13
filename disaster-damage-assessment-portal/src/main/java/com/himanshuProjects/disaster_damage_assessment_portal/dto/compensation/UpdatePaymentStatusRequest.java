package com.himanshuProjects.disaster_damage_assessment_portal.dto.compensation;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
