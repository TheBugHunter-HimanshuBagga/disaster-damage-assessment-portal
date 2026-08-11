package com.himanshuProjects.disaster_damage_assessment_portal.dto.assessment;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddInspectionImagesRequest {

    @Size(max = 10, message = "Cannot add more than 10 images at once")
    private List<@Size(max = 500, message = "Image URL cannot exceed 500 characters") String> imageUrls;
}
