package com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "report_images")
public class ReportImage extends BaseEntity {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Column(name = "image_url",nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY) // Many reports belongs to one disaster
    @JoinColumn(name = "disaster_report_id", nullable = false)
    private DisasterReport disasterReport;
}
