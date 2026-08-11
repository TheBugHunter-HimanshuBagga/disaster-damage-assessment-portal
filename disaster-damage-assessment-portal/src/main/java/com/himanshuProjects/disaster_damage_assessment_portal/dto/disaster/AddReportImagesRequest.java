package com.himanshuProjects.disaster_damage_assessment_portal.dto.disaster;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddReportImagesRequest {

    @NotBlank(message = "At least one image URL is required")
    @Size(max = 10, message = "Cannot add more than 10 images at once")
    private List<@Size(max = 500, message = "Image URL cannot exceed 500 characters") String> imageUrls;
}
